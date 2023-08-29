package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

class ReminderDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderDescriptionBinding

    companion object {
        const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = R.layout.activity_reminder_description
        binding = DataBindingUtil.setContentView(this, layoutId)

        val reminderDataItem = intent.getSerializableExtra(EXTRA_ReminderDataItem) as? ReminderDataItem
        if (reminderDataItem == null) {
            finish()
            return
        }

        binding.reminderDataItem = reminderDataItem

        val mapView = binding.reminderMapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            val location = reminderDataItem.location

            if (location != null && location.contains(",")) {
                val latLngArray = location.split(",")
                if (latLngArray.size == 2) {
                    val latitude = latLngArray[0].toDouble()
                    val longitude = latLngArray[1].toDouble()

                    val latLng = LatLng(latitude, longitude)

                    val markerOptions = MarkerOptions().position(latLng).title("Local do Lembrete")
                    googleMap.addMarker(markerOptions)

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.reminderMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.reminderMapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.reminderMapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.reminderMapView.onLowMemory()
    }
}