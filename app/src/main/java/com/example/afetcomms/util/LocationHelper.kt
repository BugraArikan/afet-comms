package com.example.afetcomms.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object LocationHelper {

    data class Coordinates(val latitude: Double, val longitude: Double)

    fun interface LocationCallback {
        fun onLocation(coords: Coordinates?)
    }

    fun formatForMessage(coords: Coordinates?): String {
        if (coords == null) return ""
        return " [konum: ${"%.5f".format(coords.latitude)}, ${"%.5f".format(coords.longitude)}]"
    }

    @SuppressLint("MissingPermission")
    fun getLastKnown(context: Context): Coordinates? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            if (!manager.isProviderEnabled(provider)) continue
            val loc = manager.getLastKnownLocation(provider) ?: continue
            if (best == null || loc.time > best.time) best = loc
        }
        return best?.let { Coordinates(it.latitude, it.longitude) }
    }

    /**
     * SOS / Güvendeyim gönderiminden hemen önce çağrılır.
     * Önce önbellek, yoksa kısa süreli canlı GPS isteği.
     */
    @SuppressLint("MissingPermission")
    suspend fun awaitForSend(context: Context, timeoutMs: Long = 5000L): Coordinates? {
        return withContext(Dispatchers.Main) {
            val cached = getLastKnown(context)
            if (cached != null) return@withContext cached

            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return@withContext null
            }

            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) {
                            cont.resume(Coordinates(location.latitude, location.longitude))
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val timeoutHandler = Handler(Looper.getMainLooper())
                timeoutHandler.postDelayed({
                    manager.removeUpdates(listener)
                    if (cont.isActive) {
                        cont.resume(getLastKnown(context))
                    }
                }, timeoutMs)

                cont.invokeOnCancellation {
                    timeoutHandler.removeCallbacksAndMessages(null)
                    manager.removeUpdates(listener)
                }

                manager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(
        context: Context,
        minIntervalMs: Long = 15_000L,
        callback: LocationCallback
    ): LocationListener? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        callback.onLocation(getLastKnown(context))

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                callback.onLocation(Coordinates(location.latitude, location.longitude))
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return listener
        }

        manager.requestLocationUpdates(
            provider,
            minIntervalMs,
            10f,
            listener,
            Looper.getMainLooper()
        )
        return listener
    }

    fun stopUpdates(context: Context, listener: LocationListener?) {
        if (listener == null) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        manager.removeUpdates(listener)
    }

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }
}
