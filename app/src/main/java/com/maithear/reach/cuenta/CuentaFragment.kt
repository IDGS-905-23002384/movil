package com.maithear.reach.cuenta

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.maithear.reach.BuildConfig
import com.maithear.reach.R
import com.maithear.reach.admin.AdminActivity
import com.maithear.reach.admin.CatalogosActivity
import com.maithear.reach.auth.MainActivity
import com.maithear.reach.core.Campo
import com.maithear.reach.core.PasswordRequest
import com.maithear.reach.core.PerfilRequest
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.configurarFila
import com.maithear.reach.core.confirmar
import com.maithear.reach.core.dialogoFormulario
import com.maithear.reach.core.exito
import com.maithear.reach.core.iniciales
import com.maithear.reach.core.insetsSistema
import com.maithear.reach.core.requiereSesion
import com.maithear.reach.core.siError
import com.maithear.reach.core.siOk
import com.maithear.reach.core.toast
import com.maithear.reach.core.visible
import com.maithear.reach.pedidos.PedidosActivity
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.launch

class CuentaFragment : Fragment(), Refrescable {

    companion object {
        private const val ARG_ADMIN = "modo_admin"

        fun paraAdmin(): CuentaFragment = CuentaFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_ADMIN, true) }
        }
    }

    private val modoAdmin: Boolean
        get() = arguments?.getBoolean(ARG_ADMIN, false) ?: false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_cuenta, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.headerCuenta).insetsSistema(arriba = true)
        view.findViewById<TextView>(R.id.tvVersion).text = "Reach ${BuildConfig.VERSION_NAME}"
        view.findViewById<MaterialButton>(R.id.btnCuentaIniciar).setOnClickListener { activity?.requiereSesion() }
        pintar()
    }

    override fun alMostrar() = pintar()

    override fun onResume() {
        super.onResume()
        if (!isHidden) pintar()
    }

    private fun pintar() {
        val raiz = view ?: return
        val usuario = Sesion.usuario
        val activa = Sesion.activa && usuario != null

        raiz.findViewById<TextView>(R.id.tvCuentaAvatar).text = if (activa) iniciales(usuario?.nombre) else "R"
        raiz.findViewById<TextView>(R.id.tvCuentaNombre).text = if (activa) usuario?.nombre else "Invitado"
        raiz.findViewById<TextView>(R.id.tvCuentaEmail).text = when {
            !activa -> "Inicia sesión para comprar y seguir tus pedidos"
            Sesion.esPersonal -> "${usuario?.email} · ${usuario?.rol}"
            else -> usuario?.email
        }
        raiz.findViewById<View>(R.id.btnCuentaIniciar).visible(!activa)

        val filaPedidos = raiz.findViewById<View>(R.id.filaPedidos)
        val filaDirecciones = raiz.findViewById<View>(R.id.filaDirecciones)
        val filaPerfil = raiz.findViewById<View>(R.id.filaPerfil)
        val filaPassword = raiz.findViewById<View>(R.id.filaPassword)
        val filaPanel = raiz.findViewById<View>(R.id.filaPanel)
        val filaServidor = raiz.findViewById<View>(R.id.filaServidor)
        val filaCerrar = raiz.findViewById<View>(R.id.filaCerrarSesion)

        raiz.findViewById<View>(R.id.cardMenuCuenta).visible(activa)
        filaCerrar.visible(activa)

        if (modoAdmin) {
            filaPedidos.configurarFila(R.drawable.ic_truck, "Proveedores", "Distribuidores y contactos") {
                startActivity(Intent(requireContext(), CatalogosActivity::class.java).putExtra(CatalogosActivity.EXTRA_TIPO, CatalogosActivity.TIPO_PROVEEDORES))
            }
            filaDirecciones.configurarFila(R.drawable.ic_grid, "Categorías", "Organiza el catálogo y el configurador") {
                startActivity(Intent(requireContext(), CatalogosActivity::class.java).putExtra(CatalogosActivity.EXTRA_TIPO, CatalogosActivity.TIPO_CATEGORIAS))
            }
            filaPanel.configurarFila(R.drawable.ic_store, "Ver tienda", "Navega como lo hace un cliente") {
                startActivity(Intent(requireContext(), TiendaActivity::class.java))
            }
            filaPanel.visible(true)
        } else {
            filaPedidos.configurarFila(R.drawable.ic_receipt, "Mis pedidos", "Seguimiento y entregas") {
                startActivity(Intent(requireContext(), PedidosActivity::class.java))
            }
            filaDirecciones.configurarFila(R.drawable.ic_location, "Mis direcciones", "Domicilios de entrega") {
                startActivity(Intent(requireContext(), DireccionesActivity::class.java))
            }
            filaPanel.configurarFila(R.drawable.ic_dashboard, "Panel de administración", "Pedidos, inventario y productos") {
                startActivity(Intent(requireContext(), AdminActivity::class.java))
            }
            filaPanel.visible(Sesion.esPersonal)
        }

        filaPerfil.configurarFila(R.drawable.ic_edit, "Editar perfil", usuario?.telefono?.ifBlank { null }) { editarPerfil() }
        filaPassword.configurarFila(R.drawable.ic_lock, "Cambiar contraseña") { cambiarPassword() }
        filaServidor.configurarFila(R.drawable.ic_tune, "Servidor de la API", Sesion.baseUrl) { configurarServidor() }
        filaCerrar.configurarFila(R.drawable.ic_logout, "Cerrar sesión", peligro = true) {
            requireContext().confirmar("Cerrar sesión", "¿Quieres salir de tu cuenta?", "Salir") {
                Sesion.cerrar()
                startActivity(Intent(requireContext(), MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }

    private fun editarPerfil() {
        val usuario = Sesion.usuario ?: return
        requireContext().dialogoFormulario(
            titulo = "Editar perfil",
            campos = listOf(
                Campo("nombre", "Nombre completo", usuario.nombre, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS),
                Campo("telefono", "Teléfono", usuario.telefono.orEmpty(), InputType.TYPE_CLASS_PHONE, "10 dígitos")
            )
        ) { valores, dialogo ->
            val nombre = valores["nombre"].orEmpty()
            val telefono = valores["telefono"].orEmpty()
            if (nombre.length < 2) {
                requireContext().toast("Escribe tu nombre")
                return@dialogoFormulario
            }
            if (telefono.isNotEmpty() && !Regex("^\\d{10}$").matches(telefono)) {
                requireContext().toast("El teléfono debe tener 10 dígitos")
                return@dialogoFormulario
            }
            viewLifecycleOwner.lifecycleScope.launch {
                api { actualizarPerfil(PerfilRequest(nombre, telefono.ifEmpty { null })) }
                    .siOk {
                        Sesion.actualizarUsuario(it)
                        dialogo.dismiss()
                        requireContext().toast("Perfil actualizado")
                        pintar()
                    }
                    .siError { requireContext().toast(it.mensaje) }
            }
        }
    }

    private fun cambiarPassword() {
        val tipo = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        requireContext().dialogoFormulario(
            titulo = "Cambiar contraseña",
            campos = listOf(
                Campo("actual", "Contraseña actual", tipo = tipo),
                Campo("nueva", "Nueva contraseña", tipo = tipo, pista = "Mínimo 8 caracteres con letras y números"),
                Campo("confirmar", "Confirmar nueva contraseña", tipo = tipo)
            ),
            textoAceptar = "Actualizar"
        ) { valores, dialogo ->
            val nueva = valores["nueva"].orEmpty()
            when {
                valores["actual"].isNullOrEmpty() -> requireContext().toast("Escribe tu contraseña actual")
                nueva.length < 8 || !nueva.any { it.isLetter() } || !nueva.any { it.isDigit() } ->
                    requireContext().toast("La nueva contraseña debe tener 8 caracteres con letras y números")
                nueva != valores["confirmar"] -> requireContext().toast("Las contraseñas no coinciden")
                else -> viewLifecycleOwner.lifecycleScope.launch {
                    api { cambiarPassword(PasswordRequest(valores["actual"].orEmpty(), nueva)).exito() }
                        .siOk {
                            dialogo.dismiss()
                            requireContext().toast("Contraseña actualizada")
                        }
                        .siError { requireContext().toast(it.mensaje) }
                }
            }
        }
    }

    private fun configurarServidor() {
        requireContext().dialogoFormulario(
            titulo = "Servidor de la API",
            mensaje = "Dirección del backend. Ejemplo: http://192.168.1.9:5133/",
            campos = listOf(Campo("url", "URL base", Sesion.baseUrl, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        ) { valores, dialogo ->
            val url = valores["url"].orEmpty()
            if (url.isBlank()) {
                requireContext().toast("Escribe una URL")
            } else {
                Sesion.baseUrl = url
                dialogo.dismiss()
                pintar()
            }
        }
    }
}
