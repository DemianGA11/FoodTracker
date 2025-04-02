package com.example.foodtracker.modelos


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.BaseAdapter

class AlimentosAdapter(private val context: Context, private val alimentos: List<Alimento>) : BaseAdapter() {

    override fun getCount(): Int {
        return alimentos.size
    }

    override fun getItem(position: Int): Any {
        return alimentos[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
        val alimento = alimentos[position]

        val txtNombre = view.findViewById<TextView>(android.R.id.text1)
        val txtDetalles = view.findViewById<TextView>(android.R.id.text2)

        txtNombre.text = alimento.nombre
        txtDetalles.text = "${alimento.cantidad} ${alimento.unidad} - Vence: ${alimento.fechaCaducidad}"

        return view
    }
}