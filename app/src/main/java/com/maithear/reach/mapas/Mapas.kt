package com.maithear.reach.mapas

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.location.Location
import android.net.Uri
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.maithear.reach.BuildConfig
import com.maithear.reach.core.Punto
import com.maithear.reach.core.dp
import com.maithear.reach.core.toast
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

object Mapas {
    val LEON = GeoPoint(21.1221, -101.6827)

    fun iniciar(context: Context) {
        val config = Configuration.getInstance()
        config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        config.userAgentValue = BuildConfig.APPLICATION_ID
        val base = File(context.cacheDir, "osmdroid")
        config.osmdroidBasePath = base
        config.osmdroidTileCache = File(base, "tiles")
    }

    fun preparar(mapa: MapView, centro: GeoPoint = LEON, zoom: Double = 13.0, interactivo: Boolean = true) {
        mapa.setTileSource(TileSourceFactory.MAPNIK)
        mapa.setTilesScaledToDpi(true)
        mapa.setMultiTouchControls(interactivo)
        mapa.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapa.setMinZoomLevel(5.0)
        mapa.setMaxZoomLevel(19.0)
        mapa.controller.setZoom(zoom)
        mapa.controller.setCenter(centro)
        if (interactivo) bloquearScrollPadre(mapa)
    }

    fun punto(p: Punto): GeoPoint = GeoPoint(p.latitud, p.longitud)

    fun marcador(mapa: MapView, punto: GeoPoint, titulo: String, icono: Int): Marker {
        val marcador = Marker(mapa)
        marcador.position = punto
        marcador.title = titulo
        marcador.icon = ContextCompat.getDrawable(mapa.context, icono)
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapa.overlays.add(marcador)
        return marcador
    }

    fun ruta(mapa: MapView, puntos: List<GeoPoint>, color: Int, discontinua: Boolean = false): Polyline {
        val linea = Polyline(mapa)
        linea.setPoints(puntos)
        linea.outlinePaint.color = color
        linea.outlinePaint.strokeWidth = mapa.context.dp(5).toFloat()
        linea.outlinePaint.strokeCap = Paint.Cap.ROUND
        if (discontinua) linea.outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(18f, 14f), 0f)
        mapa.overlays.add(0, linea)
        return linea
    }

    fun encuadrar(mapa: MapView, puntos: List<GeoPoint>, zoomUnico: Double = 15.5) {
        if (puntos.isEmpty()) return
        if (puntos.size == 1) {
            mapa.controller.setZoom(zoomUnico)
            mapa.controller.setCenter(puntos[0])
            return
        }
        mapa.post {
            mapa.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(puntos), false, mapa.context.dp(56))
        }
    }

    fun limpiar(mapa: MapView) {
        mapa.overlays.clear()
        mapa.invalidate()
    }

    fun distanciaKm(desde: GeoPoint, latitud: Double, longitud: Double): Float {
        val resultado = FloatArray(1)
        Location.distanceBetween(desde.latitude, desde.longitude, latitud, longitud, resultado)
        return resultado[0] / 1000f
    }

    fun abrirNavegacion(context: Context, latitud: Double, longitud: Double, nombre: String) {
        val geo = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(nombre)})")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, geo))
        } catch (e: ActivityNotFoundException) {
            val web = Uri.parse("https://www.openstreetmap.org/?mlat=$latitud&mlon=$longitud#map=17/$latitud/$longitud")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, web))
            } catch (ex: ActivityNotFoundException) {
                context.toast("No hay una app de mapas instalada")
            }
        }
    }

    fun llamar(context: Context, telefono: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
        } catch (e: ActivityNotFoundException) {
            context.toast("No se puede abrir el marcador")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bloquearScrollPadre(mapa: MapView) {
        mapa.setOnTouchListener { v, evento ->
            when (evento.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }
}
