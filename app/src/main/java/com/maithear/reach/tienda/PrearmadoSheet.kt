package com.maithear.reach.tienda

import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.maithear.reach.R
import com.maithear.reach.configurador.ConfiguradorEstado
import com.maithear.reach.configurador.ArmaPcFragment
import com.maithear.reach.core.Prearmado
import com.maithear.reach.core.agregarFilaTotal
import com.maithear.reach.core.cargarImagen
import com.maithear.reach.core.mxn
import kotlinx.coroutines.launch

object PrearmadoSheet {

    fun mostrar(fragment: Fragment, prearmado: Prearmado) {
        val contexto = fragment.requireContext()
        val dialogo = BottomSheetDialog(contexto)
        val vista = LayoutInflater.from(contexto).inflate(R.layout.sheet_prearmado, null)

        vista.findViewById<ImageView>(R.id.ivSheetPrearmado).cargarImagen(prearmado.imagenUrl)
        vista.findViewById<TextView>(R.id.tvSheetUso).text = prearmado.categoriaUso
        vista.findViewById<TextView>(R.id.tvSheetNombre).text = prearmado.nombre
        vista.findViewById<TextView>(R.id.tvSheetDescripcion).text = prearmado.descripcion

        val lista = vista.findViewById<LinearLayout>(R.id.llSheetComponentes)
        for (c in prearmado.componentes) {
            val fila = LayoutInflater.from(contexto).inflate(R.layout.item_resumen_componente, lista, false)
            fila.findViewById<ImageView>(R.id.ivRc).cargarImagen(c.producto.imagenUrl)
            fila.findViewById<TextView>(R.id.tvRcPaso).text = ArmaPcFragment.nombreTipo(c.tipo)
            fila.findViewById<TextView>(R.id.tvRcNombre).text = c.producto.nombre
            fila.findViewById<TextView>(R.id.tvRcPrecio).text = c.producto.precio.mxn()
            fila.setOnClickListener { ProductoDetalleActivity.abrir(contexto, c.producto.id) }
            lista.addView(fila)
        }

        val totales = vista.findViewById<LinearLayout>(R.id.llSheetTotales)
        totales.agregarFilaTotal("Componentes", prearmado.precioComponentes.mxn())
        totales.agregarFilaTotal("Ensamble y pruebas", prearmado.costoManoObra.mxn())
        totales.agregarFilaTotal("Total", prearmado.precioTotal.mxn(), destacado = true)

        val btnAgregar = vista.findViewById<MaterialButton>(R.id.btnSheetAgregar)
        btnAgregar.isEnabled = prearmado.disponible
        if (!prearmado.disponible) btnAgregar.text = "Sin existencias suficientes"
        btnAgregar.setOnClickListener {
            btnAgregar.isEnabled = false
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                val ok = AccionesCarrito.agregarPrearmado(fragment.requireActivity(), prearmado.id)
                btnAgregar.isEnabled = true
                if (ok) dialogo.dismiss()
            }
        }

        vista.findViewById<MaterialButton>(R.id.btnSheetPersonalizar).setOnClickListener {
            ConfiguradorEstado.cargarDesde(prearmado)
            dialogo.dismiss()
            (fragment.activity as? TiendaActivity)?.irA(R.id.nav_arma)
        }

        dialogo.setContentView(vista)
        dialogo.show()
    }
}
