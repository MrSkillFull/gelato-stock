package com.flstudios.a2lopes.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Chocolate & Cream Hero Header Banner for History Screen
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3E2723), // Cobertura de Chocolate Amargo
                                    Color(0xFF5D4037), // Chocolate ao Leite
                                    Color(0xFF8D6E63), // Creme Aveludado
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Histórico de Operações 📜🍦",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE0B2) // Creme Suave
                        )
                        Text(
                            "Histórico de atividades",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFF8E1) // Creme Alvo
                        )
                        Text(
                            "Acompanhe as atividades e os registros do aplicativo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD180)
                        )
                    }
                }
            }

            if (state.selectedId != null) {
                OutlinedButton(onClick = onClose, modifier = Modifier.padding(bottom = 8.dp)) { Text("← Voltar ao histórico") }
                when {
                    state.detailLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.detailError != null -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.detailError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetryDetail) { Text("Tentar novamente") }
                    }
                    state.detail != null -> DetailContent(state.detail, onOpen, onReverse)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    HistoryFilter.entries.chunked(2).forEach { filters ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filters.forEach { filter ->
                                FilterChip(
                                    selected = state.filter == filter,
                                    onClick = { onFilter(filter) },
                                    modifier = Modifier.weight(1f).testTag("history-filter-${filter.name}"),
                                    label = {
                                        Text(
                                            when (filter) {
                                                HistoryFilter.ALL -> "Todos"
                                                HistoryFilter.PRODUCTIONS -> "Produções"
                                                HistoryFilter.SALES -> "Vendas"
                                                HistoryFilter.MOVEMENTS -> "Movimentos"
                                                HistoryFilter.CORRECTIONS -> "Correções"
                                            },
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                Text(
                    if (state.filter == HistoryFilter.MOVEMENTS)
                        "Entradas e saídas por sabor. Abra a origem para consultar a produção ou venda."
                    else "Operações por data, da mais recente para a mais antiga.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        state.error != null -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onRetry) { Text("Tentar novamente") }
                        }
                        state.entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text("Nenhum registro neste filtro.", modifier = Modifier.padding(16.dp))
                            }
                        }
                        else -> key(state.filter, state.page) {
                            LazyColumn(state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 12.dp), modifier = Modifier.testTag("history-list")) {
                                items(state.entries, key = { it.key }) { entry ->
                                    if (entry.kind == HistoryKind.MOVEMENT) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("history-${entry.key}"),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(movementText(entry.movementType), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                Text("${entry.flavorName}: ${entry.units} unidades", style = MaterialTheme.typography.bodyLarge)
                                                Text("Data: ${dateTime(entry.occurredAt)} • # ${entry.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                entry.notes?.takeIf { it.isNotBlank() }?.let { Text("Obs: $it", style = MaterialTheme.typography.bodyMedium) }
                                                if (entry.originKind != null && entry.originId != null) {
                                                    TextButton(onClick = { onOpen(entry.originKind, entry.originId) }) {
                                                        Text("Origem: ${kindText(entry.originKind)} #${entry.originId}")
                                                    }
                                                } else {
                                                    Text("Origem: ${entry.reference}", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    } else {
                                        ElevatedCard(
                                            onClick = { onOpen(entry.kind, entry.id) },
                                            modifier = Modifier.fillMaxWidth().testTag("history-${entry.key}"),
                                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${entry.correctionType?.let(::correctionTitle) ?: kindText(entry.kind)} #${entry.id}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text("${entry.units} un", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                                Text("Data: ${dateTime(entry.occurredAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${entry.flavorCount} sabor(es)${entry.flavorName?.let { " • $it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                                                entry.notes?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                                if (entry.reversalId != null) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                                        Text("Registro estornado", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onPrevious, enabled = state.page > 0 && !state.loading) { Text("Anteriores") }
                    Text("Página ${state.page + 1}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onNext, enabled = state.hasNext && !state.loading) { Text("Próximos") }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(detail: HistoryDetail, onOpen: (HistoryKind, String) -> Unit, onReverse: (HistoryDetail) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("history-detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${detail.correctionType?.let(::correctionTitle) ?: kindText(detail.kind)} #${detail.id}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Data da operação: ${dateTime(detail.createdAt)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (detail.kind == HistoryKind.CORRECTION) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detalhes da Correção", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Motivo: ${detail.notes.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    detail.correctionItems.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.flavorName, style = MaterialTheme.typography.titleMedium)
                            Text("Saldo anterior: ${item.previousQuantity} unidades", style = MaterialTheme.typography.bodySmall)
                            Text("${if (detail.correctionType == CorrectionType.ADJUSTMENT) "Contagem física" else "Saldo após"}: ${item.resultingQuantity} unidades", style = MaterialTheme.typography.bodySmall)
                            Text("Variação: ${if (item.quantityDelta > 0) "+" else ""}${item.quantityDelta} unidades", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text(item.movementId?.let { "Movimentação #$it" } ?: "Conferência sem diferença", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (detail.sourceKind != null && detail.sourceId != null) {
                OutlinedButton(onClick = { onOpen(detail.sourceKind, detail.sourceId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver origem: ${kindText(detail.sourceKind)} #${detail.sourceId}")
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Itens da Operação", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    detail.notes?.takeIf { it.isNotBlank() }?.let { Text("Notas: $it", style = MaterialTheme.typography.bodyMedium) }
                    HorizontalDivider()
                    detail.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.flavorName, style = MaterialTheme.typography.bodyLarge)
                            Text("${item.quantity} unidades • R$ ${FlavorRules.priceText(item.unitPriceCents)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (detail.reversalId == null && (detail.kind == HistoryKind.SALE || detail.kind == HistoryKind.PRODUCTION)) {
            Button(onClick = { onReverse(detail) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Estornar ${kindText(detail.kind).lowercase()}", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
