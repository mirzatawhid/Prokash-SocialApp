package com.dcoder.prokash.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dcoder.prokash.R
import com.dcoder.prokash.data.model.NominatimResponse

class AddressSuggestionAdapter(
    private val addresses: List<NominatimResponse>,
    private val onItemClick: (NominatimResponse) -> Unit
) : RecyclerView.Adapter<AddressSuggestionAdapter.AddressViewHolder>() {

    class AddressViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addressText: TextView = view.findViewById(R.id.address_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.address_suggestion_layout, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]
        holder.addressText.text = address.display_name

        holder.itemView.setOnClickListener {
            onItemClick(address)
        }
    }

    override fun getItemCount(): Int = addresses.size
}

