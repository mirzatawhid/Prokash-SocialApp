package com.dcoder.prokash.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.dcoder.prokash.R
import com.dcoder.prokash.complaintSubmissionFragment.LocationPickingFragment
import com.dcoder.prokash.data.model.SubCategoryModel
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel

class SubCategoryAdapter(private val fragment: Fragment, private val subCategoryList: List<SubCategoryModel>, private val viewModel: ComplaintSubmissionViewModel) : BaseAdapter(){
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
        val itemImage = view.findViewById<ImageView>(R.id.sub_category_image)
        val itemCard = view.findViewById<CardView>(R.id.sub_category_card)

        itemName.text = item.name
        itemImage.setImageResource(item.imageResourceId)

        itemCard.setOnClickListener{
            viewModel.setSubCategory(item.name.lowercase())
            viewModel.setSelectedTab(4)
            this@SubCategoryAdapter.fragment.parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LocationPickingFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

}