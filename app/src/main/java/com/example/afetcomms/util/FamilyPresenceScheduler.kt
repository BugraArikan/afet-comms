package com.example.afetcomms.util

import android.os.Handler
import android.os.Looper

/**
 * Aile üyelerinin birbirini görmesi için periyodik PRESENCE yayını.
 * BLE tek seferlik yayınlarda eşleşme kaçırılabildiğinden heartbeat zorunlu.
 */
class FamilyPresenceScheduler(
    private val onTick: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    fun start() {
        handler.removeCallbacks(tickRunnable)
        onTick()
        handler.postDelayed(tickRunnable, INTERVAL_MS)
    }

    /** İlk katılım / ekran açılışında birkaç kez art arda yayınla. */
    fun burst(extraDelaysMs: LongArray = longArrayOf(2_000L, 5_000L)) {
        onTick()
        extraDelaysMs.forEach { delay ->
            handler.postDelayed({ onTick() }, delay)
        }
    }

    fun stop() {
        handler.removeCallbacks(tickRunnable)
    }

    companion object {
        const val INTERVAL_MS = 12_000L
    }
}
