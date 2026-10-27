package com.maithear.reach

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maithear.reach.adapter.ProductoAdapter
import com.maithear.reach.model.Producto
import com.maithear.reach.network.ProductoApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductosActivity : AppCompatActivity() {

    private lateinit var rvProductos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos)

        rvProductos = findViewById(R.id.recyclerViewProductos)
        rvProductos.layoutManager = GridLayoutManager(this, 2)

        val fabAgregar = findViewById<FloatingActionButton>(R.id.fabAgregarProducto)
        fabAgregar.setOnClickListener {
            val intent = Intent(this, FormProductoActivity::class.java)
            startActivity(intent)
        }
        val navInicio = findViewById<android.widget.LinearLayout>(R.id.navInicio)
        navInicio.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
            finish()
        }

        obtenerProductosDesdeApi()
    }

    override fun onResume() {
        super.onResume()
        obtenerProductosDesdeApi()
    }

    private fun obtenerProductosDesdeApi() {
        ProductoApiService.instance.getProductos().enqueue(object : Callback<List<Producto>> {
            override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                if (response.isSuccessful) {
                    val listaProductos = response.body()

                    if (!listaProductos.isNullOrEmpty()) {
                        val adapter = ProductoAdapter(listaProductos)
                        rvProductos.adapter = adapter
                    } else {
                        Toast.makeText(this@ProductosActivity, "No hay productos registrados", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ProductosActivity, "Error servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Producto>>, t: Throwable) {
                Toast.makeText(
                    this@ProductosActivity,
                    "Error real: ${t.javaClass.simpleName} - ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}