package com.maithear.reach.mapas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.maithear.reach.core.LOCALE_MX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DireccionDetectada(
    val calle: String?,
    val numero: String?,
    val colonia: String?,
    val ciudad: String?,
    val estado: String?,
    val codigoPostal: String?
) {
    fun texto(): String {
        val linea1 = listOfNotNull(calle, numero).joinToString(" ")
        val linea2 = listOfNotNull(colonia, ciudad, estado).joinToString(", ")
        return listOf(linea1, linea2).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Ubicación seleccionada" }
    }
}

object Ubicacion {
    val PERMISOS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun tienePermiso(context: Context): Boolean = PERMISOS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun obtener(context: Context, alObtener: (Location?) -> Unit) {
        if (!tienePermiso(context)) {
            alObtener(null)
            return
        }
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null) {
            alObtener(null)
            return
        }
        val proveedores = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        val proveedor = proveedores.firstOrNull { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
        if (proveedor == null) {
            alObtener(null)
            return
        }
        LocationManagerCompat.getCurrentLocation(manager, proveedor, CancellationSignal(), ContextCompat.getMainExecutor(context)) { ubicacion ->
            alObtener(ubicacion ?: manager.getLastKnownLocation(proveedor))
        }
    }

    suspend fun direccionDe(context: Context, latitud: Double, longitud: Double): DireccionDetectada? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            @Suppress("DEPRECATION")
            val resultados = Geocoder(context, LOCALE_MX).getFromLocation(latitud, longitud, 1)
            val d = resultados?.firstOrNull() ?: return@withContext null
            DireccionDetectada(
                calle = d.thoroughfare,
                numero = d.subThoroughfare,
                colonia = d.subLocality,
                ciudad = d.locality ?: d.subAdminArea,
                estado = d.adminArea,
                codigoPostal = d.postalCode
            )
        } catch (e: Exception) {
            null
        }
    }
}
