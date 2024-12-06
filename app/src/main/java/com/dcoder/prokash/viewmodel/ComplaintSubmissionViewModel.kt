package com.dcoder.prokash.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ComplaintSubmissionViewModel: ViewModel() {

    private var _evidence = MutableLiveData<Uri?>()
    private var _isImage = MutableLiveData<Boolean?>()
    private var _category = MutableLiveData<String>()
    private var _subCategory = MutableLiveData<String>()
    private var _locationLongitude = MutableLiveData<Double>()
    private var _locationLatitude = MutableLiveData<Double>()
    private var _title = MutableLiveData<String>()
    private var _description = MutableLiveData<String>()
    private var _anonymous = MutableLiveData<Boolean>()
    private var _selectedTab = MutableLiveData<Int>()
    private var _date = MutableLiveData<String>()

    val evidence: LiveData<Uri?> = _evidence
    val isImage: LiveData<Boolean?> = _isImage
    val category: LiveData<String> = _category
    val subCategory: LiveData<String> = _subCategory
    val locationLongitude: LiveData<Double> = _locationLongitude
    val locationLatitude: LiveData<Double> = _locationLatitude
    val title: LiveData<String> = _title
    val description: LiveData<String> = _description
    val anonymous: LiveData<Boolean> = _anonymous
    val selectedTab: LiveData<Int> = _selectedTab
    val date: LiveData<String> = _date

    fun setEvidence(newEvidence: Uri?){
        _evidence.value = newEvidence
    }

    fun setCategory(newCategory:String){
        _category.value = newCategory
    }

    fun setSubCategory(newSubCategory:String){
        _subCategory.value = newSubCategory
    }

    fun setLocation(longitude:Double,latitude:Double){
        _locationLongitude.value = longitude
        _locationLatitude.value = latitude
    }

    fun setTitle(newTitle:String){
        _title.value = newTitle
    }

    fun setDescription(newDescription:String){
        _description.value = newDescription
    }

    fun setAnonymous(newAnonymous:Boolean){
        _anonymous.value = newAnonymous
    }

    fun setSelectedTab(newSelectedTab:Int){
        if(_selectedTab.value != newSelectedTab) {
            _selectedTab.value = newSelectedTab
        }
    }

    fun setIsImage(new: Boolean?){
        _isImage.value = new
    }

    fun setDate(newDate:String){
        _date.value = newDate
    }

}