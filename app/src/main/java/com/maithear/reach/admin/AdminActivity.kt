package com.maithear.reach.admin

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maithear.reach.R
import com.maithear.reach.auth.MainActivity
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Refrescable
import com.maithear.reach.core.Sesion
import com.maithear.reach.core.toast
import com.maithear.reach.cuenta.CuentaFragment

class AdminActivity : BaseActivity() {

    companion object {
        private const val ESTADO_TAB = "tab_admin"
    }

    private lateinit var navegacion: BottomNavigationView
    private var tabActual = R.id.nav_panel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.esPersonal) {
            toast("Acceso solo para personal de la tienda")
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }
        setContentView(R.layout.activity_admin)
        navegacion = findViewById(R.id.navAdmin)
        navegacion.setOnItemSelectedListener { item ->
            mostrar(item.itemId)
            true
        }
        navegacion.setOnItemReselectedListener { item ->
            (supportFragmentManager.findFragmentByTag(etiqueta(item.itemId)) as? Refrescable)?.alMostrar()
        }
        val inicial = savedInstanceState?.getInt(ESTADO_TAB) ?: R.id.nav_panel
        navegacion.selectedItemId = inicial
        if (inicial == R.id.nav_panel) mostrar(inicial)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ESTADO_TAB, tabActual)
    }

    fun irA(tab: Int) {
        if (navegacion.selectedItemId == tab) mostrar(tab) else navegacion.selectedItemId = tab
    }

    fun abrirPedidos(filtro: String?) {
        (obtener(R.id.nav_pedidos) as AdminPedidosFragment).aplicarFiltro(filtro)
        irA(R.id.nav_pedidos)
    }

    fun abrirInventario(alertas: Boolean) {
        (obtener(R.id.nav_inventario) as InventarioFragment).mostrarAlertas(alertas)
        irA(R.id.nav_inventario)
    }

    private fun etiqueta(tab: Int) = "admin_$tab"

    private fun crear(tab: Int): Fragment = when (tab) {
        R.id.nav_pedidos -> AdminPedidosFragment()
        R.id.nav_productos -> AdminProductosFragment()
        R.id.nav_inventario -> InventarioFragment()
        R.id.nav_mas -> CuentaFragment.paraAdmin()
        else -> PanelFragment()
    }

    private fun obtener(tab: Int): Fragment {
        val fm = supportFragmentManager
        fm.findFragmentByTag(etiqueta(tab))?.let { return it }
        val nuevo = crear(tab)
        fm.beginTransaction().add(R.id.contenedorAdmin, nuevo, etiqueta(tab)).hide(nuevo).commitNow()
        return nuevo
    }

    private fun mostrar(tab: Int) {
        tabActual = tab
        val destino = obtener(tab)
        val transaccion = supportFragmentManager.beginTransaction()
        for (f in supportFragmentManager.fragments) {
            if (f.id == R.id.contenedorAdmin && f !== destino && !f.isHidden) transaccion.hide(f)
        }
        transaccion.show(destino).commitNow()
        (destino as? Refrescable)?.alMostrar()
    }
}
