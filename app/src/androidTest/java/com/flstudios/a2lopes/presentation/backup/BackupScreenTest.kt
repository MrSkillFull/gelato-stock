package com.flstudios.a2lopes.presentation.backup

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.*
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.backup.*
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var vm: BackupViewModel
    private var launched: Intent? = null
    private var restored = false
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            launched = contract.createIntent(compose.activity, input)
            dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
        }
    }
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking { db.flavorDao().insert(FlavorEntity(1, "Atual")) }
        compose.activityRule.scenario.onActivity { activity ->
            vm = ViewModelProvider(activity, BackupViewModel.Factory(RoomBackupRepository(db)))[BackupViewModel::class.java]
            val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            activity.setContent {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    MaterialTheme { BackupScreen(vm) { restored = true } }
                }
            }
        }
    }
    @After fun close() { compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }; db.close() }
    private fun inspectEmpty() {
        val data = runBlocking { RoomBackupRepository(db).snapshot() }.copy(flavors = emptyList())
        val bytes = ByteArrayOutputStream().also { BackupJson.write(data, it) }.toByteArray()
        compose.runOnIdle { vm.inspect { bytes.inputStream() } }
        compose.waitUntil(5000) { !vm.state.value.busy && vm.state.value.pending != null }
    }
    @Test fun safUsesCreateAndOpenDocumentsAndCancellationDoesNotWrite() {
        compose.onNodeWithText("Exportar backup").performClick()
        compose.runOnIdle {
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, launched!!.action)
            assertEquals("application/json", launched!!.type)
            assertTrue(launched!!.getStringExtra(Intent.EXTRA_TITLE)!!.startsWith("2lopes-backup-"))
        }
        compose.onNodeWithText("Restaurar backup").performClick()
        compose.runOnIdle { assertEquals(Intent.ACTION_OPEN_DOCUMENT, launched!!.action); assertFalse(vm.state.value.selecting) }
        assertEquals("Atual", runBlocking { db.flavorDao().findById(1)!!.name })
    }
    @Test fun summaryAndWarningRequireConfirmationAndCancelPreservesDatabase() {
        inspectEmpty()
        compose.onNodeWithText("Todos os dados atuais serão substituídos", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancelar restauração").performScrollTo().performClick()
        compose.runOnIdle { assertNull(vm.state.value.pending); assertFalse(restored) }
        assertNotNull(runBlocking { db.flavorDao().findById(1) })
    }
    @Test fun explicitConfirmationReplacesDataAndSignalsCacheReset() {
        inspectEmpty()
        compose.onNodeWithText("Confirmar substituição completa").performScrollTo().performClick()
        compose.waitUntil(5000) { restored }
        assertNull(runBlocking { db.flavorDao().findById(1) })
        compose.runOnIdle { assertEquals("Backup restaurado com sucesso.", vm.state.value.message) }
    }
    @Test fun invalidFileShowsErrorAndNeverOffersConfirmation() {
        compose.runOnIdle { vm.inspect { "invalid".byteInputStream() } }
        compose.waitUntil(5000) { !vm.state.value.busy && vm.state.value.message != null }
        compose.onNodeWithText("Confirmar substituição completa").assertDoesNotExist()
        assertNotNull(runBlocking { db.flavorDao().findById(1) })
    }
}
