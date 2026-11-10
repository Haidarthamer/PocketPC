package dev.pocketpc.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.pocketpc.system.capability.HardwareSource
import dev.pocketpc.system.capability.android.AndroidHardwareSource

/** The single place production code binds the capability contract to Android APIs. */
object HardwareSourceFactory {
    fun create(context: Context): HardwareSource = AndroidHardwareSource(context.applicationContext)
}

/** Small helper so probes can hop back to the UI thread without leaking a scope. */
private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

fun runOnUiThreadOnMain(block: () -> Unit) = mainHandler.post(block)
