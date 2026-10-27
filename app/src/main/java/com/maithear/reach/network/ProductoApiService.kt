package com.maithear.reach.network

import com.maithear.reach.model.Categoria
import com.maithear.reach.model.Producto
import retrofit2.Call
import retrofit2.http.*

interface ProductoApiService {

    @GET("api/Productos")
    fun getProductos(): Call<List<Producto>>

    @GET("api/Categorias")
    fun getCategorias(): Call<List<Categoria>>

    @POST("api/Productos")
    fun crearProducto(@Body producto: Producto): Call<Producto>

    @PUT("api/Productos/{id}")
    fun actualizarProducto(@Path("id") id: Int, @Body producto: Producto): Call<Void>

    @DELETE("api/Productos/{id}")
    fun desactivarProducto(@Path("id") id: Int): Call<Void>

    companion object {
        val instance: ProductoApiService by lazy {
            RetrofitClient.retrofitInstance.create(ProductoApiService::class.java)
        }
    }
}