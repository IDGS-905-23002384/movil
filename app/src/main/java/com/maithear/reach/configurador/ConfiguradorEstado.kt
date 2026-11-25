package com.maithear.reach.configurador

import com.maithear.reach.core.ComponenteSeleccionado
import com.maithear.reach.core.Prearmado
import com.maithear.reach.core.ProductoLista
import com.maithear.reach.core.SeleccionPc
import com.maithear.reach.core.TiposComponente

object ConfiguradorEstado {
    private val unicos = mutableMapOf<String, ProductoLista>()
    private val almacenamiento = mutableListOf<ProductoLista>()

    var version = 0
        private set

    var pasoInicial: Int = -1

    fun seleccionado(tipo: String): List<ProductoLista> =
        if (tipo == TiposComponente.STORAGE) almacenamiento.toList() else listOfNotNull(unicos[tipo])

    fun estaSeleccionado(tipo: String, id: Int): Boolean = seleccionado(tipo).any { it.id == id }

    fun tieneSeleccion(tipo: String): Boolean = seleccionado(tipo).isNotEmpty()

    val vacio: Boolean
        get() = unicos.isEmpty() && almacenamiento.isEmpty()

    fun alternar(tipo: String, producto: ProductoLista) {
        if (tipo == TiposComponente.STORAGE) {
            val existente = almacenamiento.indexOfFirst { it.id == producto.id }
            if (existente >= 0) almacenamiento.removeAt(existente) else almacenamiento.add(producto)
        } else {
            if (unicos[tipo]?.id == producto.id) unicos.remove(tipo) else unicos[tipo] = producto
        }
        version++
    }

    fun quitar(tipo: String) {
        if (tipo == TiposComponente.STORAGE) almacenamiento.clear() else unicos.remove(tipo)
        version++
    }

    fun limpiar() {
        unicos.clear()
        almacenamiento.clear()
        pasoInicial = 0
        version++
    }

    fun cargarDesde(prearmado: Prearmado) {
        unicos.clear()
        almacenamiento.clear()
        for (c in prearmado.componentes) {
            if (c.tipo == TiposComponente.STORAGE) almacenamiento.add(c.producto) else unicos[c.tipo] = c.producto
        }
        pasoInicial = 0
        version++
    }

    fun cargarDesde(componentes: List<ComponenteSeleccionado>) {
        unicos.clear()
        almacenamiento.clear()
        for (c in componentes) {
            if (c.tipo == TiposComponente.STORAGE) almacenamiento.add(c.producto) else unicos[c.tipo] = c.producto
        }
        pasoInicial = 0
        version++
    }

    fun seleccionPc(): SeleccionPc = SeleccionPc(
        cpuId = unicos[TiposComponente.CPU]?.id,
        motherboardId = unicos[TiposComponente.MOTHERBOARD]?.id,
        ramId = unicos[TiposComponente.RAM]?.id,
        gpuId = unicos[TiposComponente.GPU]?.id,
        coolerId = unicos[TiposComponente.COOLER]?.id,
        caseId = unicos[TiposComponente.CASE]?.id,
        psuId = unicos[TiposComponente.PSU]?.id,
        almacenamientoIds = almacenamiento.map { it.id }
    )
}
