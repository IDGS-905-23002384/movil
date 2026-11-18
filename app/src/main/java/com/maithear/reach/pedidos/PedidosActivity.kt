package com.maithear.reach.pedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.maithear.reach.R
import com.maithear.reach.core.BaseActivity
import com.maithear.reach.core.Resultado
import com.maithear.reach.core.api
import com.maithear.reach.core.mostrarVacio
import com.maithear.reach.core.ocultarVacio
import com.maithear.reach.tienda.TiendaActivity
import kotlinx.coroutines.launch

class PedidosActivity : BaseActivity() {

    private lateinit var refrescar: SwipeRefreshLayout
    private val adaptador = PedidoAdapter { PedidoDetalleActivity.abrir(this, it.id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista)
        configurarHeader("Mis pedidos", "Compras")
        refrescar = findViewById(R.id.refrescarLista)
        refrescar.setColorSchemeResources(R.color.azul)
        refrescar.setOnRefreshListener { cargar() }
        findViewById<RecyclerView>(R.id.rvLista).apply {
            layoutManager = LinearLayoutManager(this@PedidosActivity)
            adapter = adaptador
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        refrescar.isRefreshing = true
        lifecycleScope.launch {
            val r = api { misPedidos() }
            refrescar.isRefreshing = false
            val raiz = findViewById<View>(android.R.id.content)
            when (r) {
                is Resultado.Ok -> {
                    adaptador.establecer(r.dato)
                    if (r.dato.isEmpty()) {
                        raiz.mostrarVacio(R.drawable.ic_receipt, "Aún no tienes pedidos", "Cuando compres, aquí podrás seguir tu envío paso a paso.", "Ir a la tienda") {
                            startActivity(Intent(this@PedidosActivity, TiendaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                            finish()
                        }
                    } else {
                        raiz.ocultarVacio()
                    }
                }
                is Resultado.Error -> raiz.mostrarVacio(R.drawable.ic_warning, "No pudimos cargar tus pedidos", r.mensaje, "Reintentar") { cargar() }
            }
        }
    }
}
