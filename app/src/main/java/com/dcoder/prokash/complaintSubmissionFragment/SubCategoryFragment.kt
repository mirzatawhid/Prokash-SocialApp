package com.dcoder.prokash.complaintSubmissionFragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.dcoder.prokash.R
import com.dcoder.prokash.adapter.SubCategoryAdapter
import com.dcoder.prokash.databinding.FragmentCategoryBinding
import com.dcoder.prokash.databinding.FragmentSubCategoryBinding
import com.dcoder.prokash.model.SubCategoryModel
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import kotlin.math.log

class SubCategoryFragment : Fragment() {


    private var _binding: FragmentSubCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ComplaintSubmissionViewModel

    private lateinit var subCategoryList: List<SubCategoryModel>
    private lateinit var subCategoryAdapter: SubCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSubCategoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(ComplaintSubmissionViewModel::class.java)
        //initialize sub category data
        subCategoryList = listOf(
            SubCategoryModel("Damage Roads", R.drawable.sub_category_damage_roads, "transportation"),
            SubCategoryModel("Damage Vehicle", R.drawable.sub_category_damage_vehicle, "transportation"),
            SubCategoryModel("Traffic Jam", R.drawable.sub_category_traffic_jam, "transportation"),
            SubCategoryModel("Lack of Roads", R.drawable.sub_category_lack_of_roads, "transportation"),
            SubCategoryModel("Lack of Bridges", R.drawable.sub_category_lack_of_bridges, "transportation"),
            SubCategoryModel("Drainage System", R.drawable.sub_category_drainage_system, "transportation"),
            SubCategoryModel("Lack of Hospital", R.drawable.sub_category_lack_of_hospital, "institution"),
            SubCategoryModel("Lack of Educational Institute", R.drawable.sub_category_lack_of_school, "institution"),
            SubCategoryModel("Lack of Playground", R.drawable.sub_category_lack_of_playground, "institution"),
            SubCategoryModel("Poor Infrastructure", R.drawable.sub_category_poor_infrastructure, "institution"),
            SubCategoryModel("Poor Hospital Facilities", R.drawable.sub_category_poor_hospital, "institution"),
            SubCategoryModel("Poor School Facilities", R.drawable.sub_category_poor_school, "institution"),
            SubCategoryModel("Poor Park Management", R.drawable.sub_category_poor_park, "institution"),
            SubCategoryModel("Plastic Factory", R.drawable.sub_category_plastic_factory, "illegal occurrence"),
            SubCategoryModel("Food Adulteration", R.drawable.sub_category_food_adulteration, "illegal occurrence"),
            SubCategoryModel("Price Hiking", R.drawable.sub_category_price_hiking, "illegal occurrence"),
            SubCategoryModel("Deforestation", R.drawable.sub_category_deforestation, "natural"),
            SubCategoryModel("Mosquito", R.drawable.sub_category_mosquito, "natural"),
            SubCategoryModel("Garbage", R.drawable.sub_category_garbage, "pollution"),
            SubCategoryModel("Air", R.drawable.sub_category_air, "pollution"),
            SubCategoryModel("Water", R.drawable.sub_category_water, "pollution"),
            SubCategoryModel("Others", R.drawable.icon_category_other, "others")
        )
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        filterByCategory(viewModel.category.value!!)



    }
    
    private fun filterByCategory(category: String){
        Log.d("subC", "filterByCategory: "+category)
        val filterList = subCategoryList.filter { it.category == category }
        Log.d("subC", "filterByCategory: "+filterList)
        subCategoryAdapter = SubCategoryAdapter(this,filterList)
        binding.subCategoryGridview.adapter = subCategoryAdapter
    }

}