package com.maithear.reach.model

data class Producto(
    val id: Int = 0,
    val sku: String? = "",
    val nombre: String,
    val marca: String,
    val descripcion: String,
    val precio: Double,
    val precioAnterior: Double? = null,
    val stock: Int = 0,
    val stockMinimo: Int = 5,
    val tipoInventario: String = "Almacen",
    val imagenUrl: String? = "",
    val especificacionesTecnicas: String? = "",
    val envio: Boolean? = true,
    val categoriaId: Int = 1,
    val categoria: Categoria? = null
)