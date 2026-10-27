package com.maithear.reach

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class InicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        val cardProductos = findViewById<CardView>(R.id.cardModuloProductos)
        val navProductos = findViewById<LinearLayout>(R.id.navProductos)

        cardProductos.setOnClickListener {
            val intent = Intent(this, ProductosActivity::class.java)
            startActivity(intent)
        }

        navProductos.setOnClickListener {
            val intent = Intent(this, ProductosActivity::class.java)
            startActivity(intent)
        }
    }
}