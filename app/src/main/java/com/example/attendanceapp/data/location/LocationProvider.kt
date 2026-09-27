package com.example.attendanceapp.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class CapturedLocation(val lat: Double?, val lng: Double?, val address: String?)

class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CapturedLocation {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            val location = client.getCurrentLocation(request, null).await()
                ?: return CapturedLocation(null, null, null)

            val address = reverseGeocode(location.latitude, location.longitude)
            CapturedLocation(location.latitude, location.longitude, address)
        } catch (e: Exception) {
            CapturedLocation(null, null, null)
        }
    }

    // Runs off the main thread (suspend fun), so the deprecated synchronous overload is fine
    // here — it avoids the callback plumbing the API 33+ async overload would need.
    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lng: Double): String? = try {
        val geocoder = Geocoder(context, Locale.getDefault())
        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
    } catch (e: Exception) {
        null
    }
}
