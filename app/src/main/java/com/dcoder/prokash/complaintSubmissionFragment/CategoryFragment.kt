package com.dcoder.prokash.complaintSubmissionFragment

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.dcoder.prokash.R
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.dcoder.prokash.databinding.FragmentCategoryBinding


class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ComplaintSubmissionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(viewModel.category.value !=null){
            when(viewModel.category.value){
                "transportation" -> binding.transportation.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
                "institution" -> binding.institution.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
                "illegal occurrence" -> binding.illegalOccurrence.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
                "natural" -> binding.natural.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
                "pollution" -> binding.pollution.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
                "others" -> binding.others.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_selected))
            }
        }

        binding.transportation.setOnClickListener{
            viewModel.setCategory("transportation")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }

        binding.institution.setOnClickListener{
            viewModel.setCategory("institution")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }

        binding.illegalOccurrence.setOnClickListener{
            viewModel.setCategory("illegal occurrence")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }

        binding.natural.setOnClickListener{
            viewModel.setCategory("natural")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }

        binding.pollution.setOnClickListener{
            viewModel.setCategory("pollution")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }

        binding.others.setOnClickListener{
            viewModel.setCategory("others")
            viewModel.setSelectedTab(3)
            replaceFragment(SubCategoryFragment())
        }


    }

    private fun replaceFragment(fragment : Fragment){
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}