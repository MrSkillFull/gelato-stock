package com.flstudios.a2lopes.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
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
fun HistoryScreen(
    state: HistoryUiState,
    onFilter: (HistoryFilter) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpen: (HistoryKind, String) -> Unit,
    onClose: () -> Unit,
    onRetryDetail: () -> Unit,
    onReverse: (HistoryDetail) -> Unit = {}
) {
    BackHandler(enabled = state.selectedId != null, onBack = onClose)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state.selectedId != null) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Voltar ao histórico")
                }

                when {
                    state.detailLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    state.detailError != null -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(state.detailError, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetryDetail) { Text("Tentar novamente") }
                    }

                    state.detail != null -> DetailContent(state.detail, onOpen, onReverse)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    HistoryFilter.entries.chunked(2).forEach { filters ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filters.forEach { filter ->
                                FilterChip(
                                    selected = state.filter == filter,
                                    onClick = { onFilter(filter) },
                                    shape = RoundedCornerShape(10.dp),
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
                    text = if (state.filter == HistoryFilter.MOVEMENTS)
                        "Entradas e saídas por sabor. Abra a origem para consultar a produção ou venda."
                    else "Operações por data, da mais recente para a mais antiga.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        state.error != null -> Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRetry) { Text("Tentar novamente") }
                        }

                        state.entries.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "Nenhum registro neste filtro.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        else -> key(state.filter, state.page) {
                            LazyColumn(
                                state = rememberLazyListState(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 12.dp),
                                modifier = Modifier.testTag("history-list")
                            ) {
                                items(state.entries, key = { it.key }) { entry ->
                                    if (entry.kind == HistoryKind.MOVEMENT) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("history-${entry.key}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = movementText(entry.movementType),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${entry.flavorName}: ${entry.units} unidades",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Data: ${dateTime(entry.occurredAt)} • #${entry.id}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                entry.notes?.takeIf { it.isNotBlank() }?.let {
                                                    Text(
                                                        text = "Obs: $it",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (entry.originKind != null && entry.originId != null) {
                                                    TextButton(
                                                        onClick = { onOpen(entry.originKind, entry.originId) },
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("Origem: ${kindText(entry.originKind)} #${entry.originId}")
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Origem: ${entry.reference}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        ElevatedCard(
                                            onClick = { onOpen(entry.kind, entry.id) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("history-${entry.key}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.elevatedCardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${entry.correctionType?.let(::correctionTitle) ?: kindText(entry.kind)} #${entry.id}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    ) {
                                                        Text(
                                                            text = "${entry.units} un",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Data: ${dateTime(entry.occurredAt)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${entry.flavorCount} sabor(es)${entry.flavorName?.let { " • $it" }.orEmpty()}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                entry.notes?.let {
                                                    Text(
                                                        text = "Motivo: $it",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (entry.reversalId != null) {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ) {
                                                        Text("Registro estornado", modifier = Modifier.padding(4.dp))
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevious,
                        enabled = state.page > 0 && !state.loading,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Anteriores") }

                    Text(
                        text = "Página ${state.page + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onNext,
                        enabled = state.hasNext && !state.loading,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Próximos") }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: HistoryDetail,
    onOpen: (HistoryKind, String) -> Unit,
    onReverse: (HistoryDetail) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("history-detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${detail.correctionType?.let(::correctionTitle) ?: kindText(detail.kind)} #${detail.id}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Data do registro: ${dateTime(detail.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (detail.kind == HistoryKind.CORRECTION) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Detalhes da Correção",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Motivo: ${detail.notes.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                OutlinedButton(
                    onClick = { onOpen(detail.sourceKind, detail.sourceId) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver origem: ${kindText(detail.sourceKind)} #${detail.sourceId}")
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Itens da Operação",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    detail.notes?.takeIf { it.isNotBlank() }?.let { Text("Notas: $it", style = MaterialTheme.typography.bodyMedium) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    detail.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.flavorName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${item.quantity} unidades • R$ ${FlavorRules.priceText(item.unitPriceCents)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (detail.reversalId == null && (detail.kind == HistoryKind.SALE || detail.kind == HistoryKind.PRODUCTION)) {
            Button(
                onClick = { onReverse(detail) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(imageVector = Icons.Outlined.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Estornar ${kindText(detail.kind).lowercase()}", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
