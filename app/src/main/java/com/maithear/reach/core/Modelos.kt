package com.maithear.reach.core

data class Usuario(
    val id: Int,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val rol: String
)

data class AuthRespuesta(val token: String, val expira: String, val usuario: Usuario)
data class LoginRequest(val email: String, val password: String)
data class RegistroRequest(val nombre: String, val email: String, val telefono: String?, val password: String)
data class PerfilRequest(val nombre: String, val telefono: String?)
data class PasswordRequest(val passwordActual: String, val passwordNueva: String)

data class Categoria(
    val id: Int,
    val nombre: String,
    val slug: String,
    val tipoComponente: String?,
    val orden: Int,
    val totalProductos: Int,
    val imagenUrl: String?
)

data class CategoriaGuardar(val nombre: String, val tipoComponente: String?, val orden: Int)

data class ProductoLista(
    val id: Int,
    val sku: String,
    val nombre: String,
    val marca: String,
    val precio: Double,
    val precioAnterior: Double?,
    val descuentoPorcentaje: Int,
    val stock: Int,
    val imagenUrl: String?,
    val categoriaId: Int,
    val categoriaNombre: String,
    val tipoComponente: String?,
    val destacado: Boolean,
    val activo: Boolean,
    val envio: Boolean
)

data class EspecificacionLinea(val nombre: String, val valor: String)

data class EspecificacionComponente(
    val socket: String? = null,
    val socketsSoportados: String? = null,
    val tipoMemoria: String? = null,
    val formFactor: String? = null,
    val formFactorsSoportados: String? = null,
    val interfaz: String? = null,
    val consumoW: Int? = null,
    val capacidadW: Int? = null,
    val fuenteRecomendadaW: Int? = null,
    val tdpMaxW: Int? = null,
    val longitudMm: Int? = null,
    val longitudMaxGpuMm: Int? = null,
    val alturaMm: Int? = null,
    val alturaMaxCoolerMm: Int? = null,
    val radiadorMm: Int? = null,
    val radiadorMaxMm: Int? = null,
    val slotsRam: Int? = null,
    val modulosRam: Int? = null,
    val capacidadGb: Int? = null,
    val ranurasM2: Int? = null,
    val graficosIntegrados: Boolean? = null,
    val incluyeDisipador: Boolean? = null
)

data class ProductoDetalle(
    val id: Int,
    val sku: String,
    val nombre: String,
    val marca: String,
    val descripcion: String?,
    val precio: Double,
    val precioAnterior: Double?,
    val descuentoPorcentaje: Int,
    val stock: Int,
    val stockMinimo: Int,
    val imagenUrl: String?,
    val categoriaId: Int,
    val categoriaNombre: String,
    val tipoComponente: String?,
    val destacado: Boolean,
    val activo: Boolean,
    val envio: Boolean,
    val proveedorId: Int?,
    val proveedorNombre: String?,
    val especificacionesTecnicas: String?,
    val especificaciones: List<EspecificacionLinea>?,
    val especificacion: EspecificacionComponente?
)

data class ProductoGuardar(
    val sku: String,
    val nombre: String,
    val marca: String,
    val descripcion: String,
    val precio: Double,
    val precioAnterior: Double?,
    val stock: Int,
    val stockMinimo: Int,
    val imagenUrl: String,
    val especificacionesTecnicas: String,
    val categoriaId: Int,
    val proveedorId: Int?,
    val envio: Boolean,
    val destacado: Boolean,
    val activo: Boolean,
    val especificacion: EspecificacionComponente?
)

data class Pagina<T>(val items: List<T>, val pagina: Int, val tamano: Int, val total: Int, val totalPaginas: Int)

data class Proveedor(
    val id: Int,
    val nombreEmpresa: String,
    val contactoPersona: String?,
    val telefono: String?,
    val email: String?,
    val activo: Boolean,
    val totalProductos: Int
)

data class ProveedorGuardar(
    val nombreEmpresa: String,
    val contactoPersona: String,
    val telefono: String?,
    val email: String?,
    val activo: Boolean
)

data class SeleccionPc(
    val cpuId: Int? = null,
    val motherboardId: Int? = null,
    val ramId: Int? = null,
    val gpuId: Int? = null,
    val coolerId: Int? = null,
    val caseId: Int? = null,
    val psuId: Int? = null,
    val almacenamientoIds: List<Int> = emptyList()
)

data class OpcionesRequest(val tipo: String, val seleccion: SeleccionPc)

data class PasoConfigurador(
    val tipo: String,
    val nombre: String,
    val categoriaId: Int,
    val obligatorio: Boolean,
    val permiteVarios: Boolean,
    val ayuda: String
)

data class OpcionComponente(val producto: ProductoLista, val compatible: Boolean, val motivos: List<String>?, val resumen: String?)

data class ComponenteSeleccionado(val tipo: String, val paso: String, val producto: ProductoLista)

data class ResumenConfiguracion(
    val componentes: List<ComponenteSeleccionado>,
    val errores: List<String>,
    val advertencias: List<String>,
    val pendientes: List<String>,
    val consumoEstimadoW: Int,
    val fuenteRecomendadaW: Int,
    val subtotal: Double,
    val costoEnsamble: Double,
    val total: Double,
    val completo: Boolean
)

data class PrearmadoComponente(val tipo: String, val producto: ProductoLista, val cantidad: Int)

data class Prearmado(
    val id: Int,
    val nombre: String,
    val categoriaUso: String,
    val descripcion: String,
    val imagenUrl: String?,
    val precioComponentes: Double,
    val costoManoObra: Double,
    val precioTotal: Double,
    val disponible: Boolean,
    val componentes: List<PrearmadoComponente>
)

data class AgregarCarrito(val productoId: Int, val cantidad: Int)
data class ActualizarCantidad(val cantidad: Int)
data class AgregarEnsamble(val seleccion: SeleccionPc, val nombre: String?)

data class CarritoItem(
    val id: Int,
    val productoId: Int,
    val sku: String,
    val nombre: String,
    val marca: String,
    val imagenUrl: String?,
    val precio: Double,
    val cantidad: Int,
    val stock: Int,
    val subtotal: Double,
    val esEnsamble: Boolean,
    val ensambleId: String?,
    val ensambleNombre: String?
)

data class Carrito(
    val items: List<CarritoItem>,
    val totalArticulos: Int,
    val totalEnsambles: Int,
    val subtotalProductos: Double,
    val costoEnsamble: Double,
    val total: Double,
    val envioGratisDesde: Double,
    val faltaParaEnvioGratis: Double,
    val avisos: List<String>?
)

data class Direccion(
    val id: Int,
    val alias: String,
    val nombreRecibe: String,
    val telefono: String,
    val calle: String,
    val numeroExterior: String,
    val numeroInterior: String?,
    val colonia: String,
    val ciudad: String,
    val estado: String,
    val codigoPostal: String,
    val referencias: String?,
    val esPredeterminada: Boolean
)

data class DireccionGuardar(
    val alias: String,
    val nombreRecibe: String,
    val telefono: String,
    val calle: String,
    val numeroExterior: String,
    val numeroInterior: String?,
    val colonia: String,
    val ciudad: String,
    val estado: String,
    val codigoPostal: String,
    val referencias: String?,
    val esPredeterminada: Boolean
)

data class MetodoEnvio(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val paqueteria: String,
    val costo: Double,
    val costoAplicado: Double,
    val diasMin: Int,
    val diasMax: Int,
    val requiereDireccion: Boolean,
    val esGratis: Boolean
)

data class Pago(val titular: String, val numeroTarjeta: String, val vencimiento: String, val cvv: String)
data class Checkout(val direccionId: Int?, val metodoEnvioId: Int, val pago: Pago, val notas: String?)

data class PedidoResumen(
    val id: Int,
    val folio: String,
    val fecha: String,
    val total: Double,
    val estado: String,
    val estadoArmado: String,
    val estadoEnvio: String,
    val incluyeEnsamble: Boolean,
    val articulos: Int,
    val imagenUrl: String?,
    val cliente: String?
)

data class PedidoDetalleItem(
    val productoId: Int,
    val sku: String,
    val nombre: String,
    val imagenUrl: String?,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val esEnsamble: Boolean,
    val ensambleId: String?,
    val ensambleNombre: String?
)

data class Seguimiento(val titulo: String, val descripcion: String, val fecha: String)

data class PedidoDetalle(
    val id: Int,
    val folio: String,
    val fecha: String,
    val fechaActualizacion: String,
    val estado: String,
    val estadoArmado: String,
    val estadoEnvio: String,
    val incluyeEnsamble: Boolean,
    val subtotal: Double,
    val costoManoObra: Double,
    val costoEnvio: Double,
    val iva: Double,
    val total: Double,
    val metodoEnvio: String,
    val requiereDireccion: Boolean,
    val paqueteria: String?,
    val numeroGuia: String?,
    val fechaEstimadaEntrega: String?,
    val envioNombreRecibe: String?,
    val envioTelefono: String?,
    val envioDireccion: String?,
    val envioCiudad: String?,
    val envioEstado: String?,
    val envioCodigoPostal: String?,
    val envioReferencias: String?,
    val metodoPago: String,
    val tarjeta: String?,
    val notas: String?,
    val cliente: String,
    val clienteEmail: String,
    val puedeCancelar: Boolean,
    val detalles: List<PedidoDetalleItem>,
    val seguimiento: List<Seguimiento>
)

data class ActualizarArmado(val estado: String)
data class ActualizarEnvio(val estado: String, val paqueteria: String?, val numeroGuia: String?)

data class ResumenPanel(
    val pedidosHoy: Int,
    val ventasHoy: Double,
    val ventasMes: Double,
    val pendientesEnvio: Int,
    val enEnsamble: Int,
    val enviados: Int,
    val productosStockBajo: Int,
    val productosActivos: Int,
    val clientes: Int,
    val ultimosPedidos: List<PedidoResumen>
)

data class Movimiento(
    val id: Int,
    val productoId: Int,
    val productoNombre: String,
    val sku: String,
    val tipoMovimiento: String,
    val cantidad: Int,
    val stockResultante: Int,
    val motivo: String,
    val pedidoId: Int?,
    val responsable: String,
    val fecha: String
)

data class MovimientoCrear(val productoId: Int, val tipoMovimiento: String, val cantidad: Int, val motivo: String)

data class AlertaStock(
    val productoId: Int,
    val sku: String,
    val nombre: String,
    val imagenUrl: String?,
    val stock: Int,
    val stockMinimo: Int,
    val categoria: String
)

data class Notificacion(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val pedidoId: Int?,
    val leida: Boolean,
    val fecha: String
)

data class ConteoNotificaciones(val noLeidas: Int, val ultimoId: Int)

object TiposComponente {
    const val CPU = "CPU"
    const val MOTHERBOARD = "MOTHERBOARD"
    const val RAM = "RAM"
    const val STORAGE = "STORAGE"
    const val GPU = "GPU"
    const val COOLER = "COOLER"
    const val CASE = "CASE"
    const val PSU = "PSU"
}
