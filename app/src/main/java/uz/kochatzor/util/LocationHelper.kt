package uz.kochatzor.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** GPS/tarmoq orqali joriy koordinatani bir martalik so'raydi. Play Services shart emas. */
object LocationHelper {
 @SuppressLint("MissingPermission")
 suspend fun current(context: Context): Location? {
  val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  val provider = when {
   lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
   lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
   else -> return null
  }
  return suspendCancellableCoroutine { cont ->
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val signal = CancellationSignal()
    cont.invokeOnCancellation { signal.cancel() }
    lm.getCurrentLocation(provider, signal, Executors.newSingleThreadExecutor()) { loc ->
     if (cont.isActive) cont.resume(loc ?: lm.getLastKnownLocation(provider))
    }
   } else {
    val listener = object : LocationListener {
     override fun onLocationChanged(location: Location) {
      lm.removeUpdates(this)
      if (cont.isActive) cont.resume(location)
     }
    }
    cont.invokeOnCancellation { lm.removeUpdates(listener) }
    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
   }
  }
 }
}
