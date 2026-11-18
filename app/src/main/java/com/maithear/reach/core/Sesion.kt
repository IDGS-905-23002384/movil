package com.maithear.reach.core

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.maithear.reach.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Sesion {
    private const val PREFS = "reach_sesion"
    private const val ALIAS = "reach_sesion_llave"
    private const val K_TOKEN = "token"
    private const val K_USUARIO = "usuario"
    private const val K_EXPIRA = "expira"
    private const val K_URL = "base_url"
    private const val K_EMAIL = "ultimo_email"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private var tokenMemoria: String? = null
    private var expiraMemoria: Long = 0L
    var usuario: Usuario? = null
        private set

    fun iniciar(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cifrado = prefs.getString(K_TOKEN, null)
        tokenMemoria = cifrado?.let { descifrar(it) }
        expiraMemoria = prefs.getLong(K_EXPIRA, 0L)
        usuario = prefs.getString(K_USUARIO, null)?.let {
            try {
                gson.fromJson(it, Usuario::class.java)
            } catch (e: Exception) {
                null
            }
        }
        if (!activa) limpiarToken()
    }

    val activa: Boolean
        get() = tokenMemoria != null && usuario != null && System.currentTimeMillis() < expiraMemoria

    val token: String?
        get() = if (activa) tokenMemoria else null

    val esPersonal: Boolean
        get() = activa && (usuario?.rol == "Empleado" || usuario?.rol == "Administrador")

    val esAdministrador: Boolean
        get() = activa && usuario?.rol == "Administrador"

    var baseUrl: String
        get() = prefs.getString(K_URL, null) ?: BuildConfig.API_BASE_URL
        set(valor) {
            prefs.edit().putString(K_URL, normalizarUrl(valor)).apply()
        }

    val ultimoEmail: String?
        get() = prefs.getString(K_EMAIL, null)

    fun guardar(auth: AuthRespuesta, recordar: Boolean) {
        tokenMemoria = auth.token
        usuario = auth.usuario
        expiraMemoria = Fechas.milis(auth.expira) ?: (System.currentTimeMillis() + 8 * 60 * 60 * 1000L)
        val editor = prefs.edit().putString(K_EMAIL, auth.usuario.email)
        if (recordar) {
            editor.putString(K_TOKEN, cifrar(auth.token))
                .putLong(K_EXPIRA, expiraMemoria)
                .putString(K_USUARIO, gson.toJson(auth.usuario))
        } else {
            editor.remove(K_TOKEN).remove(K_EXPIRA).remove(K_USUARIO)
        }
        editor.apply()
    }

    fun actualizarUsuario(nuevo: Usuario) {
        usuario = nuevo
        if (prefs.contains(K_USUARIO)) prefs.edit().putString(K_USUARIO, gson.toJson(nuevo)).apply()
    }

    fun cerrar() {
        tokenMemoria = null
        usuario = null
        expiraMemoria = 0L
        limpiarToken()
    }

    private fun limpiarToken() {
        prefs.edit().remove(K_TOKEN).remove(K_EXPIRA).remove(K_USUARIO).apply()
    }

    fun normalizarUrl(valor: String): String {
        var url = valor.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url = "$url/"
        return url
    }

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private fun cifrar(texto: String): String? = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(datos, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    private fun descifrar(valor: String): String? = try {
        val datos = Base64.decode(valor, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, datos, 0, 12))
        String(cipher.doFinal(datos, 12, datos.size - 12), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}
