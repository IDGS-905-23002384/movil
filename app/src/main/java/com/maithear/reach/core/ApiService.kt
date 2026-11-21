package com.maithear.reach.core

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthRespuesta

    @POST("api/auth/registro")
    suspend fun registro(@Body body: RegistroRequest): AuthRespuesta

    @GET("api/auth/perfil")
    suspend fun perfil(): Usuario

    @PUT("api/auth/perfil")
    suspend fun actualizarPerfil(@Body body: PerfilRequest): Usuario

    @PUT("api/auth/password")
    suspend fun cambiarPassword(@Body body: PasswordRequest): Response<Unit>

    @GET("api/categorias")
    suspend fun categorias(): List<Categoria>

    @POST("api/categorias")
    suspend fun crearCategoria(@Body body: CategoriaGuardar): Categoria

    @PUT("api/categorias/{id}")
    suspend fun actualizarCategoria(@Path("id") id: Int, @Body body: CategoriaGuardar): Categoria

    @GET("api/productos")
    suspend fun productos(
        @Query("categoriaId") categoriaId: Int? = null,
        @Query("q") q: String? = null,
        @Query("marca") marca: String? = null,
        @Query("orden") orden: String? = null,
        @Query("soloOfertas") soloOfertas: Boolean? = null,
        @Query("soloDestacados") soloDestacados: Boolean? = null,
        @Query("incluirInactivos") incluirInactivos: Boolean? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 20
    ): Pagina<ProductoLista>

    @GET("api/productos/{id}")
    suspend fun producto(@Path("id") id: Int): ProductoDetalle

    @GET("api/productos/marcas")
    suspend fun marcas(@Query("categoriaId") categoriaId: Int?): List<String>

    @POST("api/productos")
    suspend fun crearProducto(@Body body: ProductoGuardar): ProductoDetalle

    @PUT("api/productos/{id}")
    suspend fun actualizarProducto(@Path("id") id: Int, @Body body: ProductoGuardar): ProductoDetalle

    @DELETE("api/productos/{id}")
    suspend fun desactivarProducto(@Path("id") id: Int): Response<Unit>

    @PATCH("api/productos/{id}/activar")
    suspend fun activarProducto(@Path("id") id: Int): Response<Unit>

    @GET("api/configurador/pasos")
    suspend fun pasos(): List<PasoConfigurador>

    @POST("api/configurador/opciones")
    suspend fun opciones(@Body body: OpcionesRequest): List<OpcionComponente>

    @POST("api/configurador/validar")
    suspend fun validar(@Body body: SeleccionPc): ResumenConfiguracion

    @GET("api/configurador/prearmados")
    suspend fun prearmados(): List<Prearmado>

    @GET("api/carrito")
    suspend fun carrito(): Carrito

    @POST("api/carrito/items")
    suspend fun agregarCarrito(@Body body: AgregarCarrito): Carrito

    @PUT("api/carrito/items/{id}")
    suspend fun actualizarCantidad(@Path("id") id: Int, @Body body: ActualizarCantidad): Carrito

    @DELETE("api/carrito/items/{id}")
    suspend fun eliminarItem(@Path("id") id: Int): Carrito

    @DELETE("api/carrito")
    suspend fun vaciarCarrito(): Response<Unit>

    @POST("api/carrito/ensamble")
    suspend fun agregarEnsamble(@Body body: AgregarEnsamble): Carrito

    @POST("api/carrito/prearmado/{id}")
    suspend fun agregarPrearmado(@Path("id") id: Int): Carrito

    @DELETE("api/carrito/ensamble/{ensambleId}")
    suspend fun eliminarEnsamble(@Path("ensambleId") ensambleId: String): Carrito

    @GET("api/direcciones")
    suspend fun direcciones(): List<Direccion>

    @POST("api/direcciones")
    suspend fun crearDireccion(@Body body: DireccionGuardar): Direccion

    @PUT("api/direcciones/{id}")
    suspend fun actualizarDireccion(@Path("id") id: Int, @Body body: DireccionGuardar): Direccion

    @PUT("api/direcciones/{id}/predeterminada")
    suspend fun predeterminarDireccion(@Path("id") id: Int): Response<Unit>

    @DELETE("api/direcciones/{id}")
    suspend fun eliminarDireccion(@Path("id") id: Int): Response<Unit>

    @GET("api/envios/metodos")
    suspend fun metodosEnvio(@Query("subtotal") subtotal: Double): List<MetodoEnvio>

    @POST("api/pedidos")
    suspend fun crearPedido(@Body body: Checkout): PedidoDetalle

    @GET("api/pedidos")
    suspend fun misPedidos(): List<PedidoResumen>

    @GET("api/pedidos/{id}")
    suspend fun pedido(@Path("id") id: Int): PedidoDetalle

    @POST("api/pedidos/{id}/cancelar")
    suspend fun cancelarPedido(@Path("id") id: Int): PedidoDetalle

    @GET("api/admin/panel")
    suspend fun panel(): ResumenPanel

    @GET("api/admin/pedidos")
    suspend fun pedidosAdmin(
        @Query("estado") estado: String?,
        @Query("q") q: String?,
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 30
    ): Pagina<PedidoResumen>

    @GET("api/admin/pedidos/{id}")
    suspend fun pedidoAdmin(@Path("id") id: Int): PedidoDetalle

    @PUT("api/admin/pedidos/{id}/armado")
    suspend fun actualizarArmado(@Path("id") id: Int, @Body body: ActualizarArmado): PedidoDetalle

    @PUT("api/admin/pedidos/{id}/envio")
    suspend fun actualizarEnvio(@Path("id") id: Int, @Body body: ActualizarEnvio): PedidoDetalle

    @POST("api/admin/pedidos/{id}/cancelar")
    suspend fun cancelarPedidoAdmin(@Path("id") id: Int): PedidoDetalle

    @GET("api/admin/inventario/alertas")
    suspend fun alertas(): List<AlertaStock>

    @GET("api/admin/inventario/kardex")
    suspend fun kardex(
        @Query("productoId") productoId: Int?,
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 30
    ): Pagina<Movimiento>

    @POST("api/admin/inventario/movimientos")
    suspend fun registrarMovimiento(@Body body: MovimientoCrear): Movimiento

    @GET("api/sucursales")
    suspend fun sucursales(): List<Sucursal>

    @GET("api/notificaciones")
    suspend fun notificaciones(
        @Query("soloNoLeidas") soloNoLeidas: Boolean = false,
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 50
    ): Pagina<Notificacion>

    @GET("api/notificaciones/conteo")
    suspend fun conteoNotificaciones(): ConteoNotificaciones

    @GET("api/notificaciones/nuevas")
    suspend fun notificacionesNuevas(@Query("despuesDe") despuesDe: Int): List<Notificacion>

    @PUT("api/notificaciones/{id}/leida")
    suspend fun marcarNotificacionLeida(@Path("id") id: Int): Response<Unit>

    @PUT("api/notificaciones/leidas")
    suspend fun marcarTodasLeidas(): Response<Unit>

    @GET("api/admin/proveedores")
    suspend fun proveedores(): List<Proveedor>

    @POST("api/admin/proveedores")
    suspend fun crearProveedor(@Body body: ProveedorGuardar): Proveedor

    @PUT("api/admin/proveedores/{id}")
    suspend fun actualizarProveedor(@Path("id") id: Int, @Body body: ProveedorGuardar): Proveedor
}
