package com.dcoder.prokash

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.dcoder.prokash.complaintSubmissionFragment.CategoryFragment
import com.dcoder.prokash.complaintSubmissionFragment.DetailsFragment
import com.dcoder.prokash.complaintSubmissionFragment.EvidenceFragment
import com.dcoder.prokash.complaintSubmissionFragment.LocationPickingFragment
import com.dcoder.prokash.complaintSubmissionFragment.SubCategoryFragment
import com.dcoder.prokash.databinding.ActivityComplaintSubmissionBinding
import org.maplibre.android.MapLibre

class ComplaintSubmissionActivity : AppCompatActivity() {
    private lateinit var binding:ActivityComplaintSubmissionBinding

    private lateinit var viewModel: ComplaintSubmissionViewModel

    private lateinit var anim:Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintSubmissionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        MapLibre.getInstance(this)

        viewModel = ViewModelProvider(this).get(ComplaintSubmissionViewModel::class.java)
        anim = AnimationUtils.loadAnimation(this,R.anim.complaint_submit_bar_anim)
        anim.fillAfter=true

        if (!checkPermissions()) {
            requestPermissions()
        }

        if (viewModel.selectedTab.value == null) {
            Log.d("ComplaintSubmissionActivity", "Setting initial fragment")
            viewModel.setSelectedTab(1)
        }

        viewModel.selectedTab.observe(this, Observer { tab ->
            Log.d("selected", "onCreate: "+tab)
            when (tab) {
                1 -> evidenceSelected()
                2 -> categorySelected()
                3 -> subCategorySelected()
                4 -> locationSelected()
                5 -> detailSelected()
            }
        })


        binding.evidenceLayout.setOnClickListener {
            if (viewModel.selectedTab.value != 1 || viewModel.selectedTab.value != null && viewModel.evidence.value != null) {
                viewModel.setSelectedTab(1)
            }
        }

        binding.categoryLayout.setOnClickListener {
            if (viewModel.selectedTab.value != 2 && viewModel.evidence.value!=null) {
                viewModel.setSelectedTab(2)
            }
        }

        binding.subcategoryLayout.setOnClickListener{
            if (viewModel.selectedTab.value != 3 &&  viewModel.category.value!=null){
                viewModel.setSelectedTab(3)
            }
        }

        binding.locationLayout.setOnClickListener{
            if (viewModel.selectedTab.value != 4 && viewModel.subCategory.value!=null){
                viewModel.setSelectedTab(4)
            }
        }

        binding.detailLayout.setOnClickListener{
            if (viewModel.selectedTab.value != 5 && viewModel.locationLongitude.value!=null && viewModel.locationLatitude.value!=null){
                viewModel.setSelectedTab(5)
            }
        }

        binding.submitBtnBack.setOnClickListener{
            startActivity(Intent(this@ComplaintSubmissionActivity,MainActivity::class.java))
            finish()
        }

    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),102
        )
    }

    private fun checkPermissions(): Boolean {

        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        )
        val storagePermission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED

    }

    private fun replaceFragment(fragment : Fragment){
        val trans = supportFragmentManager.beginTransaction().replace(R.id.fragment_container,fragment).commit()
    }


    private fun evidenceSelected(){
        if (viewModel.selectedTab.value!=1) return
        Log.d("complaintSubmissionActivity", "evidenceSelected: inside")
        binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence_selected)
        binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
        binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
        binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
        binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

        binding.evidenceTxt.visibility = View.VISIBLE
        binding.categoryTxt.visibility = View.GONE
        binding.subcategoryTxt.visibility = View.GONE
        binding.locationTxt.visibility = View.GONE
        binding.detailTxt.visibility = View.GONE

        binding.evidenceLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
        binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.locationLayout.setBackgroundResource(android.R.color.transparent)
        binding.detailLayout.setBackgroundResource(android.R.color.transparent)

        binding.evidenceLayout.startAnimation(anim)

        replaceFragment(EvidenceFragment())

    }

    private fun categorySelected(){
        if (viewModel.selectedTab.value != 2) return
        binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
        binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category_selected)
        binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
        binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
        binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

        binding.evidenceTxt.visibility = View.GONE
        binding.categoryTxt.visibility = View.VISIBLE
        binding.subcategoryTxt.visibility = View.GONE
        binding.locationTxt.visibility = View.GONE
        binding.detailTxt.visibility = View.GONE

        binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
        binding.categoryLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
        binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.locationLayout.setBackgroundResource(android.R.color.transparent)
        binding.detailLayout.setBackgroundResource(android.R.color.transparent)

        binding.categoryLayout.startAnimation(anim)

        replaceFragment(CategoryFragment())
        Log.d("viewmodel", "onCreate: "+viewModel.evidence.value)
    }

    private fun subCategorySelected(){
        if (viewModel.selectedTab.value != 3) return
        binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
        binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
        binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory_selected)
        binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
        binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

        binding.evidenceTxt.visibility=View.GONE
        binding.categoryTxt.visibility=View.GONE
        binding.subcategoryTxt.visibility=View.VISIBLE
        binding.locationTxt.visibility=View.GONE
        binding.detailTxt.visibility=View.GONE

        binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
        binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.subcategoryLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
        binding.locationLayout.setBackgroundResource(android.R.color.transparent)
        binding.detailLayout.setBackgroundResource(android.R.color.transparent)

        binding.subcategoryLayout.startAnimation(anim)

        replaceFragment(SubCategoryFragment())
    }

    private fun locationSelected(){
        binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
        binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
        binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
        binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location_selected)
        binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

        binding.evidenceTxt.visibility=View.GONE
        binding.categoryTxt.visibility=View.GONE
        binding.subcategoryTxt.visibility=View.GONE
        binding.locationTxt.visibility=View.VISIBLE
        binding.detailTxt.visibility=View.GONE

        binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
        binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.locationLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
        binding.detailLayout.setBackgroundResource(android.R.color.transparent)

        binding.locationLayout.startAnimation(anim)
        replaceFragment(LocationPickingFragment())

    }

    private fun detailSelected(){
        binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
        binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
        binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
        binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
        binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail_selected)

        binding.evidenceTxt.visibility=View.GONE
        binding.categoryTxt.visibility=View.GONE
        binding.subcategoryTxt.visibility=View.GONE
        binding.locationTxt.visibility=View.GONE
        binding.detailTxt.visibility=View.VISIBLE

        binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
        binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
        binding.locationLayout.setBackgroundResource(android.R.color.transparent)
        binding.detailLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)

        binding.detailLayout.startAnimation(anim)

        replaceFragment(DetailsFragment())
    }

//    override fun onResume() {
//        super.onResume()
//        if(viewModel.selectedTab.value==1 || viewModel.selectedTab.value==null){
//            evidenceSelected()
//        }else if(viewModel.selectedTab.value == 2){
//            categorySelected()
//        }else if(viewModel.selectedTab.value == 3){
//            subCategorySelected()
//        }else if(viewModel.selectedTab.value == 4){
//            locationSelected()
//        }else if(viewModel.selectedTab.value == 5){
//            detailSelected()
//        }
//    }

}