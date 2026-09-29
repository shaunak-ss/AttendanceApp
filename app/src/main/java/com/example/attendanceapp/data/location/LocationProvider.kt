package com.example.attendanceapp.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class CapturedLocation(val lat: Double?, val lng: Double?, val address: String?)

/**
 * Handle for a location fix that started acquiring in the background as soon as the
 * attendance/camera flow began, so the wait overlaps with face matching instead of following it.
 * [awaitBest] returns immediately once the background job has resolved; otherwise it waits only
 * for whatever remains of the original acquisition budget before giving up.
 */
class LocationAcquisition internal constructor(
    private val startedAtMillis: Long,
    private val totalBudgetMillis: Long,
    private val deferred: Deferred<Location?>
) {
    suspend fun awaitBest(): Location? {
        if (deferred.isCompleted) return deferred.getCompleted()
        // The underlying request's own duration budget (totalBudgetMillis) is a soft deadline —
        // it can land slightly after that mark once dispatch/teardown overhead is accounted for.
        // Add a small grace window here so we don't discard a result that's a moment away from
        // arriving; this doesn't change how long the location request itself is allowed to run.
        val remaining = (totalBudgetMillis + AWAIT_GRACE_MS) - (System.currentTimeMillis() - startedAtMillis)
        return withTimeoutOrNull(remaining.coerceAtLeast(0)) { deferred.await() }
    }

    companion object {
        private const val AWAIT_GRACE_MS = 2_000L
    }
}

class LocationProvider(private val context: Context) {

    /**
     * Kicks off location acquisition immediately: a cheap cached-location check and a fresh
     * fused-provider request run concurrently in [scope]. Call [LocationAcquisition.awaitBest]
     * later (e.g. once face matching finishes) to get whatever's freshest/valid by then.
     */
    fun beginAcquisition(scope: CoroutineScope, budgetMillis: Long = ACQUISITION_BUDGET_MS): LocationAcquisition {
        val startedAt = System.currentTimeMillis()
        Log.d(TAG, "beginAcquisition: budgetMillis=$budgetMillis hasPermission=${hasLocationPermission()}")
        val deferred = scope.async(Dispatchers.IO) {
            coroutineScope {
                val cached = async {
                    val fix = getCachedFix()
                    Log.d(TAG, "cached fix: ${fix.describe()}")
                    fix?.takeIf { it.isValid() }
                }
                val fresh = async {
                    val fix = requestFreshFix(budgetMillis)
                    Log.d(TAG, "fresh fix: ${fix.describe()}")
                    fix?.takeIf { it.isValid() }
                }
                // Prefer the fresh fix once it lands; fall back to a valid cached fix otherwise.
                val freshResult = fresh.await()
                val (result, source) = if (freshResult != null) freshResult to "fresh" else cached.await() to "cached"
                val elapsedMs = System.currentTimeMillis() - startedAt
                Log.d(
                    TAG,
                    "beginAcquisition result: source=${if (result != null) source else "none"} " +
                        "elapsedMs=$elapsedMs ${result.describe()}"
                )
                result
            }
        }
        return LocationAcquisition(startedAt, budgetMillis, deferred)
    }

    /** Best-effort last-known fix. Returns fast; never waits for a new GPS/network fix. */
    @SuppressLint("MissingPermission")
    suspend fun getCachedFix(): Location? {
        if (!hasLocationPermission()) return null
        val fused = try {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
        } catch (e: Exception) {
            Log.w("LocationProvider", "cached lastLocation failed: ${e.message}")
            null
        }
        if (fused != null) return fused

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
    }

    /**
     * Requests a fresh fix by racing the fused client and LocationManager concurrently, each
     * given the full [budgetMillis]. They run in parallel (not fused-then-fallback) so a fused
     * request that quietly holds on for its whole duration without producing a fix can't starve
     * the LocationManager fallback of its own chance to get one.
     */
    @SuppressLint("MissingPermission")
    suspend fun requestFreshFix(budgetMillis: Long): Location? {
        if (!hasLocationPermission()) return null
        return coroutineScope {
            val fused = async { fetchFromFusedClient(budgetMillis) }
            val manager = async { fetchFromLocationManager(budgetMillis) }
            fused.await() ?: manager.await()
        }
    }

    /** Reverse-geocodes the chosen fix. Runs off the caller's dispatcher since Geocoder is blocking I/O. */
    suspend fun toCapturedLocation(location: Location): CapturedLocation = withContext(Dispatchers.IO) {
        val address = reverseGeocode(location.latitude, location.longitude)
            ?: String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
        CapturedLocation(location.latitude, location.longitude, address)
    }

    private fun Location.isValid(): Boolean {
        val age = System.currentTimeMillis() - time
        return age <= MAX_FIX_AGE_MS && (!hasAccuracy() || accuracy <= MAX_ACCURACY_METERS)
    }

    private fun Location?.describe(): String {
        if (this == null) return "null"
        val age = System.currentTimeMillis() - time
        return "provider=$provider lat=$latitude lng=$longitude accuracy=$accuracy ageMs=$age valid=${isValid()}"
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFromFusedClient(budgetMillis: Long): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            // Balanced (network/Wi-Fi based) rather than high-accuracy (GPS-locked): attendance
            // needs a reasonable fix, not GPS-grade precision, and GPS cold-start indoors can take
            // far longer than this budget allows, whereas network-based location typically resolves
            // in a few seconds even indoors.
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(budgetMillis)
            .setMaxUpdateAgeMillis(0)
            .build()

        return try {
            client.getCurrentLocation(request, null).await()
        } catch (e: Exception) {
            Log.w("LocationProvider", "getCurrentLocation failed: ${e.message}")
            null
        }
    }

    /**
     * Fallback for devices/OEM skins where the fused location client returns nothing (e.g. Wi-Fi
     * scanning for network location disabled, or the fused provider otherwise restricted). Uses
     * the platform LocationManager directly against GPS/network providers.
     */
    @SuppressLint("MissingPermission")
    private suspend fun fetchFromLocationManager(timeoutMillis: Long): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            Log.w("LocationProvider", "No enabled location providers (is Location turned on?)")
            return null
        }

        return suspendCancellableCoroutine { cont ->
            var resumed = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!resumed) {
                        resumed = true
                        manager.removeUpdates(this)
                        cont.resume(location) { _, _, _ -> }
                    }
                }
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }

            providers.forEach { provider ->
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (!resumed) {
                    resumed = true
                    manager.removeUpdates(listener)
                    cont.resume(null) { _, _, _ -> }
                }
            }, timeoutMillis)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lng: Double): String? = try {
        val geocoder = Geocoder(context, Locale.getDefault())
        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.let { addr ->
            addr.getAddressLine(0) ?: "${addr.locality ?: ""}, ${addr.countryName ?: ""}".trim(',', ' ')
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TAG = "LocationProvider"
        /** A fix older than this isn't trusted as "current" for attendance purposes. */
        private const val MAX_FIX_AGE_MS = 2 * 60 * 1000L
        /** A fix with worse accuracy than this (meters) isn't trusted as "current" for attendance purposes. */
        private const val MAX_ACCURACY_METERS = 200f
        /** Total time budget for the whole acquisition (fused request and LocationManager fallback run concurrently within this). */
        const val ACQUISITION_BUDGET_MS = 10_000L
    }
}
