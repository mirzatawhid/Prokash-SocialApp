package com.dcoder.prokash.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.dcoder.prokash.R
import com.dcoder.prokash.complaintSubmissionFragment.LocationFragment
import com.dcoder.prokash.model.SubCategoryModel
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import java.util.Locale

class SubCategoryAdapter(private val fragment: Fragment, private val subCategoryList: List<SubCategoryModel>) : BaseAdapter(){
    override fun getCount(): Int {
        return subCategoryList.size
    }

    override fun getItem(position: Int): Any {
        return subCategoryList[position]
    }

    override fun getItemId(position: Int): Long {
       return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent!!.context).inflate(R.layout.sub_category_layout, parent, false)
        val item = subCategoryList[position]

        val itemName = view.findViewById<TextView>(R.id.sub_category_text)
        val itemImage = view.findViewById<ImageView>(R.id.subcategory_image)
        val itemCard = view.findViewById<CardView>(R.id.sub_category_card)

        itemName.text = item.name
        itemImage.setImageResource(item.imageResourceId)

        itemCard.setOnClickListener{
            val viewModel: ComplaintSubmissionViewModel = ViewModelProvider(fragment).get(ComplaintSubmissionViewModel::class.java)
            viewModel.setSubCategory(item.name.lowercase())
            this@SubCategoryAdapter.fragment.parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LocationFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

}