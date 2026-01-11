package com.arkus.shoppyjuan.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.arkus.shoppyjuan.domain.settings.UserPreferencesManager
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * User location data
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages user location for price filtering by distance
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the last known location (quick, may be stale)
     */
    suspend fun getLastKnownLocation(): UserLocation? {
        if (!hasLocationPermission()) return null

        return try {
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                continuation.resume(
                                    UserLocation(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        accuracy = location.accuracy,
                                        timestamp = location.time
                                    )
                                )
                            } else {
                                continuation.resume(null)
                            }
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current location (more accurate, may take time)
     */
    suspend fun getCurrentLocation(): UserLocation? {
        if (!hasLocationPermission()) return null

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setMaxUpdates(1)
        }.build()

        return try {
            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        val location = result.lastLocation
                        if (location != null) {
                            continuation.resume(
                                UserLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    timestamp = location.time
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                }

                try {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        Looper.getMainLooper()
                    )

                    continuation.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get location updates as a Flow
     */
    fun getLocationUpdates(intervalMs: Long = 60000L): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        UserLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Get the user's location for price filtering
     * First tries saved location, then last known, then current
     */
    suspend fun getLocationForPriceFilter(): UserLocation? {
        // First, try to use saved location from preferences
        val savedLocation = userPreferencesManager.lastLocation.first()
        if (savedLocation != null) {
            return UserLocation(savedLocation.first, savedLocation.second)
        }

        // Otherwise, try to get current location
        if (!hasLocationPermission()) return null

        val lastKnown = getLastKnownLocation()
        if (lastKnown != null) {
            // Save for future use
            userPreferencesManager.updateLocation(lastKnown.latitude, lastKnown.longitude)
            return lastKnown
        }

        val current = getCurrentLocation()
        if (current != null) {
            userPreferencesManager.updateLocation(current.latitude, current.longitude)
        }
        return current
    }

    /**
     * Update and save current location
     */
    suspend fun updateAndSaveLocation(): UserLocation? {
        val location = getCurrentLocation() ?: getLastKnownLocation()
        location?.let {
            userPreferencesManager.updateLocation(it.latitude, it.longitude)
        }
        return location
    }
}
