package com.maithear.reach

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.maithear.reach.model.Producto
import com.maithear.reach.network.ProductoApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FormProductoActivity : AppCompatActivity() {

    private var productoId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_producto)

        val etSku = findViewById<EditText>(R.id.etSku)
        val etNombre = findViewById<EditText>(R.id.etNombreProducto)
        val etMarca = findViewById<EditText>(R.id.etMarcaProducto)
        val etPrecio = findViewById<EditText>(R.id.etPrecioProducto)
        val etStock = findViewById<EditText>(R.id.etStock)
        val etImagenUrl = findViewById<EditText>(R.id.etImagenUrl)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcionProducto)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarProducto)
        val btnDesactivar = findViewById<Button>(R.id.btnDesactivarProducto)

        productoId = intent.getIntExtra("PRODUCTO_ID", -1)
        if (productoId != -1) {
            etSku.setText(intent.getStringExtra("PRODUCTO_SKU"))
            etNombre.setText(intent.getStringExtra("PRODUCTO_NOMBRE"))
            etMarca.setText(intent.getStringExtra("PRODUCTO_MARCA"))
            etPrecio.setText(intent.getDoubleExtra("PRODUCTO_PRECIO", 0.0).toString())
            etStock.setText(intent.getIntExtra("PRODUCTO_STOCK", 0).toString())
            etImagenUrl.setText(intent.getStringExtra("PRODUCTO_IMAGEN"))
            etDescripcion.setText(intent.getStringExtra("PRODUCTO_DESC"))

            btnGuardar.text = "Actualizar Producto"
            btnDesactivar.visibility = View.VISIBLE
        }

        // CREAR O ACTUALIZAR
        btnGuardar.setOnClickListener {
            val sku = etSku.text.toString()
            val nombre = etNombre.text.toString()
            val marca = etMarca.text.toString()
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val stock = etStock.text.toString().toIntOrNull() ?: 0
            val imagenUrl = etImagenUrl.text.toString()
            val descripcion = etDescripcion.text.toString()

            val producto = Producto(
                id = if (productoId == -1) 0 else productoId,
                sku = sku.ifEmpty { "SKU-DEFAULT" },
                nombre = nombre,
                marca = marca,
                descripcion = descripcion,
                precio = precio,
                stock = stock,
                imagenUrl = imagenUrl
            )

            if (productoId == -1) {
                // POST: Crear
                ProductoApiService.instance.crearProducto(producto).enqueue(object : Callback<Producto> {
                    override fun onResponse(call: Call<Producto>, response: Response<Producto>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@FormProductoActivity, "¡Creado con éxito!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@FormProductoActivity, "Error al crear: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Producto>, t: Throwable) {
                        Toast.makeText(this@FormProductoActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                // PUT: Actualizar
                ProductoApiService.instance.actualizarProducto(productoId, producto).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@FormProductoActivity, "¡Actualizado con éxito!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@FormProductoActivity, "Error al actualizar: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(this@FormProductoActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        // ACCIÓN: DESACTIVAR / ELIMINAR (DELETE)
        btnDesactivar.setOnClickListener {
            ProductoApiService.instance.desactivarProducto(productoId).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FormProductoActivity, "Producto desactivado", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@FormProductoActivity, "Error al desactivar: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@FormProductoActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}