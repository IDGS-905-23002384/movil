package com.maithear.reach.tienda

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maithear.reach.R
import com.maithear.reach.carrito.CarritoFragment
import com.maithear.reach.configurador.ArmaPcFragment
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.api
import com.maithear.reach.core.siOk
import com.maithear.reach.cuenta.CuentaFragment
import com.maithear.reach.notificaciones.ProgramadorNotificaciones
import kotlinx.coroutines.launch

class TiendaActivity : BaseActivity() {

    companion object {
        const val EXTRA_TAB = "tab"
        private const val ESTADO_TAB = "tab_actual"
    }

    private lateinit var navegacion: BottomNavigationView
    private var tabActual = R.id.nav_inicio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tienda)
        navegacion = findViewById(R.id.navTienda)
        prepararNotificaciones()

        navegacion.setOnItemSelectedListener { item ->
            mostrar(item.itemId)
            true
        }
        navegacion.setOnItemReselectedListener { item ->
            (supportFragmentManager.findFragmentByTag(etiqueta(item.itemId)) as? Refrescable)?.alMostrar()
        }

        val inicial = savedInstanceState?.getInt(ESTADO_TAB) ?: intent.getIntExtra(EXTRA_TAB, R.id.nav_inicio)
        navegacion.selectedItemId = inicial
        if (inicial == R.id.nav_inicio) mostrar(inicial)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        if (tab != 0) irA(tab)
    }

    override fun onResume() {
        super.onResume()
        refrescarBadge()
        ProgramadorNotificaciones.revisarAhora(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ESTADO_TAB, tabActual)
    }

    fun irA(tab: Int) {
        if (navegacion.selectedItemId == tab) {
            mostrar(tab)
        } else {
            navegacion.selectedItemId = tab
        }
    }

    fun abrirCatalogo(categoriaId: Int? = null, busqueda: String? = null, soloOfertas: Boolean = false, enfocarBusqueda: Boolean = false) {
        val catalogo = obtener(R.id.nav_catalogo) as CatalogoFragment
        catalogo.aplicarFiltro(categoriaId, busqueda, soloOfertas, enfocarBusqueda)
        irA(R.id.nav_catalogo)
    }

    fun actualizarBadge(cantidad: Int) {
        if (cantidad > 0) {
            navegacion.getOrCreateBadge(R.id.nav_carrito).apply {
                number = cantidad
                isVisible = true
            }
        } else {
            navegacion.removeBadge(R.id.nav_carrito)
        }
    }

    fun refrescarBadge() {
        if (!Sesion.activa) {
            actualizarBadge(0)
            return
        }
        lifecycleScope.launch {
            api { carrito() }.siOk { actualizarBadge(it.totalArticulos) }
        }
    }

    private fun etiqueta(tab: Int) = "tienda_$tab"

    private fun crear(tab: Int): Fragment = when (tab) {
        R.id.nav_catalogo -> CatalogoFragment()
        R.id.nav_arma -> ArmaPcFragment()
        R.id.nav_carrito -> CarritoFragment()
        R.id.nav_cuenta -> CuentaFragment()
        else -> InicioFragment()
    }

    private fun obtener(tab: Int): Fragment {
        val fm = supportFragmentManager
        fm.findFragmentByTag(etiqueta(tab))?.let { return it }
        val nuevo = crear(tab)
        fm.beginTransaction().add(R.id.contenedorTienda, nuevo, etiqueta(tab)).hide(nuevo).commitNow()
        return nuevo
    }

    private fun mostrar(tab: Int) {
        tabActual = tab
        val destino = obtener(tab)
        val transaccion = supportFragmentManager.beginTransaction()
        for (f in supportFragmentManager.fragments) {
            if (f.id == R.id.contenedorTienda && f !== destino && !f.isHidden) transaccion.hide(f)
        }
        transaccion.show(destino).commitNow()
        (destino as? Refrescable)?.alMostrar()
    }
}
