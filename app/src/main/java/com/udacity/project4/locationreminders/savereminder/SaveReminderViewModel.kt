package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "latitude" || key == "longitude") {
            retrieveLocationFromPreferences()?.let {
                reminderSelectedLocationStr.value = "${it.latitude}, ${it.longitude}"
            }
        }
    }

    init {
        app.getSharedPreferences("location_prefs", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefsListener)
        retrieveLocationFromPreferences()?.let {
            reminderSelectedLocationStr.value = "${it.latitude}, ${it.longitude}"
        }
    }


    fun clearLocationFromPreferences() {
        val sharedPreferences = app.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("latitude")
            remove("longitude")
            apply()
        }
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
        clearLocationFromPreferences()
    }

    private fun retrieveLocationFromPreferences(): LatLng? {
        val sharedPreferences = app.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat("latitude", Float.NaN)
        val longitude = sharedPreferences.getFloat("longitude", Float.NaN)

        return if (!latitude.isNaN() && !longitude.isNaN()) {
            LatLng(latitude.toDouble(), longitude.toDouble())
        } else {
            null
        }
    }



    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem): Boolean {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
            return true
        }
        return false
    }


    fun onToastShown() {
        showToast.value = null
    }

    override fun onCleared() {
        super.onCleared()
        app.getSharedPreferences("location_prefs", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefsListener)
    }


    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            // Navegar para a lista de lembretes
            navigationCommand.value = NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}