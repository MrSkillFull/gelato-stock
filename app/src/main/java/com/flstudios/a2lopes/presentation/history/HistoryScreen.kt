package com.flstudios.a2lopes.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.presentation.corrections.correctionTitle
import java.text.SimpleDateFormat
import java.util.Locale

private fun dateTime(timestamp: Long) = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR")).format(timestamp)
private fun kindText(kind: HistoryKind) = when (kind) {
    HistoryKind.PRODUCTION -> "Produção"
    HistoryKind.SALE -> "Venda"
    HistoryKind.MOVEMENT -> "Movimentação de estoque"
    HistoryKind.CORRECTION -> "Correção"
}
private fun movementText(type: String?) = when (type) {
    "PRODUCTION" -> "Entrada por produção"
    "SALE" -> "Saída por venda"
    "LOSS" -> "Perda"
    "ADJUSTMENT" -> "Ajuste por contagem"
    "SALE_REVERSAL" -> "Entrada por estorno de venda"
    "PRODUCTION_REVERSAL" -> "Saída por estorno de produção"
    else -> "Movimentação: ${type.orEmpty()}"
}

@Composable
fun HistoryScreen(state: HistoryUiState, onFilter: (HistoryFilter) -> Unit, onRetry: () -> Unit,
    onNext: () -> Unit, onPrevious: () -> Unit, onOpen: (HistoryKind, String) -> Unit,
    onClose: () -> Unit, onRetryDetail: () -> Unit, onReverse: (HistoryDetail) -> Unit = {}) {
    BackHandler(enabled = state.selectedId != null, onBack = onClose)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Histórico", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        if (state.selectedId != null) {
            TextButton(onClick = onClose) { Text("Voltar ao histórico") }
            when {
                state.detailLoading -> CircularProgressIndicator()
                state.detailError != null -> {
                    Text(state.detailError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetryDetail) { Text("Tentar novamente") }
                }
                state.detail != null -> DetailContent(state.detail, onOpen, onReverse)
            }
        } else {
            // Two rows keep all four filters reachable at large font sizes on phones.
            Column {
                HistoryFilter.entries.chunked(2).forEach { filters ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filters.forEach { filter ->
                            FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) },
                                modifier = Modifier.weight(1f).testTag("history-filter-${filter.name}"), label = {
                                    Text(when (filter) {
                                        HistoryFilter.ALL -> "Todos"
                                        HistoryFilter.PRODUCTIONS -> "Produções"
                                        HistoryFilter.SALES -> "Vendas"
                                        HistoryFilter.MOVEMENTS -> "Movimentações de estoque"
                                        HistoryFilter.CORRECTIONS -> "Perdas, ajustes e estornos"
                                    })
                                })
                        }
                    }
                }
            }
            Text(if (state.filter == HistoryFilter.MOVEMENTS)
                "Entradas e saídas por sabor. Abra a origem para consultar a produção ou venda."
                else "Operações por data, da mais recente para a mais antiga. Movimentos individuais ficam no filtro próprio.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.error != null -> Column {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Tentar novamente") }
                    }
                    state.entries.isEmpty() -> Text("Nenhum registro neste filtro.")
                    else -> key(state.filter, state.page) {
                        LazyColumn(state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp), modifier = Modifier.testTag("history-list")) {
                            items(state.entries, key = { it.key }) { entry ->
                                if (entry.kind == HistoryKind.MOVEMENT) {
                                    OutlinedCard(Modifier.fillMaxWidth().testTag("history-${entry.key}")) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(movementText(entry.movementType), style = MaterialTheme.typography.titleMedium)
                                            Text("${entry.flavorName}: ${entry.units} unidades")
                                            Text("Data da movimentação: ${dateTime(entry.occurredAt)}")
                                            Text("Movimentação #${entry.id}")
                                            entry.notes?.takeIf { it.isNotBlank() }?.let { Text("Observações: $it") }
                                            if (entry.originKind != null && entry.originId != null) {
                                                TextButton(onClick = { onOpen(entry.originKind, entry.originId) }) {
                                                    Text("Origem: ${kindText(entry.originKind)} #${entry.originId}")
                                                }
                                            } else Text("Origem: ${entry.reference}")
                                        }
                                    }
                                } else {
                                    OutlinedCard(onClick = { onOpen(entry.kind, entry.id) },
                                        modifier = Modifier.fillMaxWidth().testTag("history-${entry.key}")) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("${entry.correctionType?.let(::correctionTitle) ?: kindText(entry.kind)} #${entry.id}", style = MaterialTheme.typography.titleMedium)
                                            Text("${if (entry.kind == HistoryKind.PRODUCTION) "Data da produção" else if (entry.kind == HistoryKind.SALE) "Data da venda" else "Data da operação"}: ${dateTime(entry.occurredAt)}")
                                            Text("${entry.units} unidades • ${entry.flavorCount} sabor(es)")
                                            entry.flavorName?.let { Text(it) }
                                            entry.notes?.let { Text("Motivo: $it") }
                                            if (entry.reversalId != null) Text("Registro estornado")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPrevious, enabled = state.page > 0 && !state.loading) { Text("Anteriores") }
                Text("Página ${state.page + 1}", modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = onNext, enabled = state.hasNext && !state.loading) { Text("Próximos") }
            }
        }
    }
}

@Composable
private fun DetailContent(detail: HistoryDetail, onOpen: (HistoryKind, String) -> Unit, onReverse: (HistoryDetail) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("history-detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${detail.correctionType?.let(::correctionTitle) ?: kindText(detail.kind)} #${detail.id}", style = MaterialTheme.typography.titleLarge)
        if (detail.kind == HistoryKind.CORRECTION) {
            Text("Data da operação: ${dateTime(detail.createdAt)}")
            Text("Motivo: ${detail.notes.orEmpty()}")
            detail.correctionItems.forEach { item ->
                Column {
                    Text(item.flavorName, style = MaterialTheme.typography.titleMedium)
                    Text("Saldo anterior: ${item.previousQuantity} unidades")
                    Text("${if (detail.correctionType == CorrectionType.ADJUSTMENT) "Contagem física" else "Saldo após a operação"}: ${item.resultingQuantity} unidades")
                    Text("Variação: ${if (item.quantityDelta > 0) "+" else ""}${item.quantityDelta} unidades")
                    Text(item.movementId?.let { "Movimentação #$it" } ?: "Conferência sem diferença; nenhum movimento criado.")
                }
            }
            if (detail.sourceKind != null && detail.sourceId != null) TextButton(onClick = { onOpen(detail.sourceKind, detail.sourceId) }) {
                Text("Origem: ${kindText(detail.sourceKind)} #${detail.sourceId}")
            }
            return@Column
        }
        if (detail.reversalId != null) {
            Text(if (detail.kind == HistoryKind.SALE) "Venda estornada" else "Produção estornada")
            TextButton(onClick = { onOpen(HistoryKind.CORRECTION, detail.reversalId) }) { Text("Consultar estorno #${detail.reversalId}") }
        }
        Text("Data da ${if (detail.kind == HistoryKind.PRODUCTION) "produção" else "venda"}: ${dateTime(detail.occurredAt)}")
        Text("Data do registro: ${dateTime(detail.createdAt)}")
        detail.items.forEach { item ->
            Column {
                Text("${item.flavorName}: ${item.quantity} unidades")
                if (detail.kind == HistoryKind.SALE) {
                    Text(item.unitPriceCents?.let { "Preço unitário histórico: R$ ${FlavorRules.priceText(it)}" }
                        ?: "Preço histórico não informado")
                }
            }
        }
        Text("Total: ${detail.totalUnits} unidades", style = MaterialTheme.typography.titleMedium)
        if (detail.kind == HistoryKind.SALE) {
            Text(detail.totalCents?.let { "Total financeiro: R$ ${FlavorRules.priceText(it)}" }
                ?: "Total financeiro não disponível: há item sem preço histórico.")
        }
        detail.notes?.takeIf { it.isNotBlank() }?.let { Text("Observações: $it") }
        if (detail.reversalId == null) {
            OutlinedButton(onClick = { onReverse(detail) }) {
                Text(if (detail.kind == HistoryKind.SALE) "Estornar venda" else "Estornar produção")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
