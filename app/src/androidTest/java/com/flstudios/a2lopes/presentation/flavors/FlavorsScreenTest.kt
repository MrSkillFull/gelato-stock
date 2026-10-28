package com.flstudios.a2lopes.presentation.flavors

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.repository.RoomFlavorRepository
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlavorsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var database: AppDatabase
    private lateinit var viewModel: FlavorsViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java,
        ).build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = ViewModelProvider(store, FlavorsViewModel.Factory(RoomFlavorRepository(database)))[
                FlavorsViewModel::class.java,
            ]
        }
        compose.setContent {
            MaterialTheme {
                FlavorsScreen(
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    onNew = { viewModel.openForm() }, onEdit = viewModel::openForm,
                    onSetActive = viewModel::setActive, onNameChange = viewModel::changeName,
                    onPriceChange = viewModel::changePrice, onSave = viewModel::save,
                    onDismiss = viewModel::closeForm, onRetry = viewModel::reload,
                    onDelete = viewModel::requestDeletion,
                    onConfirmDelete = viewModel::confirmDeletion, onCancelDelete = viewModel::cancelDeletion,
                    onMessageShown = viewModel::clearMessage,
                )
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
    }

    @Test
    fun validatesCreatesEditsAndChangesStatusThroughUi() {
        compose.onNodeWithText("Novo sabor").performClick()
        compose.onNodeWithText("Salvar").performClick()
        compose.onNodeWithText("Informe o nome do sabor.").assertIsDisplayed()
        compose.onNodeWithText("Nome").performTextInput("  Coco   Cremoso ")
        compose.onNodeWithText("Preço padrão (opcional)").performTextInput("5,50")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.flavors.size == 1 && viewModel.state.value.form == null && !viewModel.state.value.busy }
        val id = viewModel.state.value.flavors.single().id
        compose.onNodeWithText("Coco Cremoso").assertIsDisplayed()
        compose.onNodeWithText("Preço padrão: R$ 5,50").assertIsDisplayed()
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Nome").performTextReplacement("Morango")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.flavors.single().name == "Morango" && viewModel.state.value.form == null && !viewModel.state.value.busy }
        assertEquals(id, viewModel.state.value.flavors.single().id)
        compose.onNodeWithText("Desativar").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.flavors.single().active && !viewModel.state.value.busy }
        compose.onNodeWithText("Inativo").assertIsDisplayed()
        compose.onNodeWithText("Reativar").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.flavors.single().active && !viewModel.state.value.busy }
        compose.onNodeWithText("Ativo").assertIsDisplayed()
        assertEquals(1, viewModel.state.value.flavors.size)
    }
    @Test
    fun cancelKeepsFlavorAndConfirmRemovesItFromRoomAndList() {
        compose.onNodeWithText("Novo sabor").performClick()
        compose.onNodeWithText("Nome").performTextInput("Coco")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.flavors.size == 1 && !viewModel.state.value.busy }
        val id = viewModel.state.value.flavors.single().id
        compose.onNodeWithText("Excluir").performClick()
        compose.onNodeWithText("Excluir sabor?").assertIsDisplayed()
        compose.onNodeWithText("irrevers", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Excluir sabor?").assertDoesNotExist()
        assertEquals(1, kotlinx.coroutines.runBlocking { database.flavorDao().observeAll().first().size })
        compose.onNodeWithText("Excluir").performClick()
        compose.onNodeWithText("Excluir definitivamente").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.flavors.isEmpty() && !viewModel.state.value.busy }
        compose.onNodeWithText("Coco").assertDoesNotExist()
        org.junit.Assert.assertNull(kotlinx.coroutines.runBlocking { database.flavorDao().findById(id) })
    }

}
