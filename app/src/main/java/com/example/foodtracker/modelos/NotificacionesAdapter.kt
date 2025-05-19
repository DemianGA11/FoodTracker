package com.example.foodtracker.modelos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.foodtracker.R

class NotificacionesAdapter(context: Context, notificaciones: MutableList<Notificacion>) :
    ArrayAdapter<Notificacion>(context, 0, notificaciones) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_notificacion, parent, false)

        val notificacion = getItem(position)
        view.findViewById<TextView>(R.id.txtTitulo).text = notificacion?.titulo
        view.findViewById<TextView>(R.id.txtMensaje).text = notificacion?.mensaje
        view.findViewById<TextView>(R.id.txtFecha).text = notificacion?.fecha

        return view
    }
}
