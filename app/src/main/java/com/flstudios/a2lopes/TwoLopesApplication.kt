package com.flstudios.a2lopes

import android.app.Application
import com.flstudios.a2lopes.di.AppContainer

class TwoLopesApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private var updateCheckStarted = false

    @Synchronized
    fun tryStartUpdateCheck(): Boolean {
        if (updateCheckStarted) return false
        updateCheckStarted = true
        return true
    }
}
