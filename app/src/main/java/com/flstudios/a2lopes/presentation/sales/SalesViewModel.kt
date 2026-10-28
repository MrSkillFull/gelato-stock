package com.flstudios.a2lopes.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.*
import com.flstudios.a2lopes.presentation.production.totalUnits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SaleProduct(val id: Long, val name: String, val unitPriceCents: Long?, val available: Long, val active: Boolean)
data class SaleCartLine(val product: SaleProduct, val quantity: Long) {
    fun toQuantity() = SaleQuantity(product.id, quantity, product.unitPriceCents)
}
data class CartTotal(val cents: Long?, val error: String? = null)

data class SalesUiState(
    val catalog: List<SaleProduct> = emptyList(),
    val cart: Map<Long, SaleCartLine> = emptyMap(),
    val loading: Boolean = true,
    val loadError: String? = null,
    val busy: Boolean = false,
    val pendingRequest: SaleRequest? = null,
    val outcomeUncertain: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val products: List<SaleProduct> get() = catalog.filter { it.active && it.available > 0 }
    val totalUnits: String get() = totalUnits(cart.values.map { it.quantity })
    val financialTotal: CartTotal get() = try {
        CartTotal(saleTotalCents(cart.values.map { it.toQuantity() }))
    } catch (exception: IllegalArgumentException) { CartTotal(null, exception.message) }
    val editable: Boolean get() = !busy && pendingRequest == null
    val canRegister: Boolean get() = !busy && (pendingRequest != null ||
        (!loading && loadError == null && cart.isNotEmpty() && financialTotal.error == null &&
            cart.values.all { it.product.active && it.quantity <= it.product.available }))
}

class SalesViewModel(
    private val flavors: FlavorRepository,
    private val stock: StockRepository,
    private val sales: SaleRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SalesUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var operationId = UUID.randomUUID().toString()

    init { reload() }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                combine(flavors.observeAll(), stock.observeAll()) { flavors, stock ->
                    val balances = stock.associateBy { it.flavorId }
                    flavors.map { flavor ->
                        SaleProduct(flavor.id, flavor.name, flavor.defaultPriceCents,
                            balances[flavor.id]?.quantity ?: 0, flavor.active && balances[flavor.id]?.active == true)
                    }
                }.collect { catalog ->
                    mutableState.update { current ->
                        val cart = if (current.pendingRequest != null) current.cart else reconcile(current.cart, catalog)
                        val changed = cart.any { (id, line) ->
                            line.product.unitPriceCents != current.cart[id]?.product?.unitPriceCents ||
                                !line.product.active || line.quantity > line.product.available
                        }
                        current.copy(catalog = catalog, cart = cart, loading = false, loadError = null,
                            error = if (current.pendingRequest == null && changed)
                                "O preço ou a disponibilidade mudou. Confira o carrinho." else current.error)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadError = "Não foi possível carregar os sabores e o estoque. Tente novamente.") }
            }
        }
    }

    private fun reconcile(cart: Map<Long, SaleCartLine>, catalog: List<SaleProduct>): Map<Long, SaleCartLine> {
        val products = catalog.associateBy { it.id }
        return cart.mapValues { (id, line) ->
            line.copy(product = products[id] ?: line.product.copy(active = false, available = 0))
        }
    }

    fun increment(id: Long) {
        val snapshot = state.value
        if (!snapshot.editable || snapshot.loading || snapshot.loadError != null) return
        val product = snapshot.catalog.firstOrNull { it.id == id && it.active } ?: return
        val quantity = snapshot.cart[id]?.quantity ?: 0
        if (quantity >= product.available) return
        mutableState.update { it.copy(cart = it.cart + (id to SaleCartLine(product, quantity + 1)), error = null, message = null) }
    }

    fun decrement(id: Long) {
        val snapshot = state.value
        if (!snapshot.editable) return
        val line = snapshot.cart[id] ?: return
        mutableState.update { it.copy(cart = if (line.quantity == 1L) it.cart - id else
            it.cart + (id to line.copy(quantity = line.quantity - 1)), error = null, message = null) }
    }

    fun register() {
        val snapshot = state.value
        if (!snapshot.canRegister) return
        val request = snapshot.pendingRequest ?: SaleRequest(operationId, now(), snapshot.cart.values.map { it.toQuantity() })
        mutableState.update { it.copy(busy = true, pendingRequest = request, error = null, message = null) }
        viewModelScope.launch {
            try {
                sales.record(request)
                operationId = UUID.randomUUID().toString()
                mutableState.update { it.copy(cart = emptyMap(), pendingRequest = null, outcomeUncertain = false, message = "Venda registrada. Estoque atualizado.") }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                mutableState.update { it.copy(pendingRequest = null, outcomeUncertain = false,
                    cart = reconcile(it.cart, it.catalog), error = exception.message ?: "Confira os itens do carrinho.") }
            } catch (_: Exception) {
                mutableState.update { it.copy(outcomeUncertain = true,
                    error = "Não foi possível confirmar a venda. Toque em Registrar venda novamente para verificar o registro.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(private val flavors: FlavorRepository, private val stock: StockRepository, private val sales: SaleRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SalesViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(flavors, stock, sales) as T
        }
    }
}
