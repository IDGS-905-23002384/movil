package com.maithear.reach.core

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val gson = Gson()
    private val principal = Handler(Looper.getMainLooper())

    @Volatile
    private var cache: Pair<String, ApiService>? = null

    var alExpirarSesion: (() -> Unit)? = null

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val token = Sesion.token
                val peticion = if (token != null) {
                    original.newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    original
                }
                val respuesta = chain.proceed(peticion)
                if (respuesta.code == 401 && token != null && !original.url.encodedPath.startsWith("/api/auth/")) {
                    principal.post {
                        if (Sesion.activa) {
                            Sesion.cerrar()
                            alExpirarSesion?.invoke()
                        }
                    }
                }
                respuesta
            })
            .build()
    }

    val servicio: ApiService
        get() {
            val url = Sesion.baseUrl
            cache?.let { if (it.first == url) return it.second }
            val nuevo = Retrofit.Builder()
                .baseUrl(url)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
            cache = url to nuevo
            return nuevo
        }

    fun urlImagen(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) return ruta
        val base = Sesion.baseUrl.trimEnd('/')
        return if (ruta.startsWith("/")) base + ruta else "$base/$ruta"
    }

    fun mensajeDe(e: HttpException): String {
        val cuerpo = try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            null
        }
        if (!cuerpo.isNullOrBlank()) {
            try {
                val json = JsonParser.parseString(cuerpo).asJsonObject
                if (json.has("errors") && json.get("errors").isJsonObject) {
                    val errores = json.getAsJsonObject("errors")
                    for (entrada in errores.entrySet()) {
                        val lista = entrada.value
                        if (lista.isJsonArray && lista.asJsonArray.size() > 0) return lista.asJsonArray[0].asString
                    }
                }
                if (json.has("detail") && !json.get("detail").isJsonNull) return json.get("detail").asString
                if (json.has("title") && !json.get("title").isJsonNull) return json.get("title").asString
            } catch (ex: Exception) {
                return mensajePorCodigo(e.code())
            }
        }
        return mensajePorCodigo(e.code())
    }

    private fun mensajePorCodigo(codigo: Int): String = when (codigo) {
        401 -> "Tu sesión expiró. Inicia sesión de nuevo."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "No encontramos lo que buscabas."
        429 -> "Demasiados intentos. Espera un momento e intenta de nuevo."
        in 500..599 -> "El servidor tuvo un problema. Intenta más tarde."
        else -> "No se pudo completar la operación ($codigo)."
    }
}

sealed class Resultado<out T> {
    data class Ok<T>(val dato: T) : Resultado<T>()
    data class Error(val mensaje: String, val codigo: Int = 0) : Resultado<Nothing>()
}

suspend fun <T> api(bloque: suspend ApiService.() -> T): Resultado<T> = try {
    Resultado.Ok(ApiClient.servicio.bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Resultado.Error(ApiClient.mensajeDe(e), e.code())
} catch (e: IOException) {
    Resultado.Error("No pudimos conectar con el servidor. Revisa tu conexión o la dirección del servidor.")
} catch (e: Exception) {
    Resultado.Error("Ocurrió un error inesperado. Intenta de nuevo.")
}

fun Response<Unit>.exito() {
    if (!isSuccessful) throw HttpException(this)
}

inline fun <T> Resultado<T>.siOk(accion: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Ok) accion(dato)
    return this
}

inline fun <T> Resultado<T>.siError(accion: (Resultado.Error) -> Unit): Resultado<T> {
    if (this is Resultado.Error) accion(this)
    return this
}
