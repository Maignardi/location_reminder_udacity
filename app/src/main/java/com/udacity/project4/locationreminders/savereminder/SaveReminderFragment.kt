package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }


    override val _viewModel: BaseViewModel
        get() = viewModel

    // Injetando a instância específica de SaveReminderViewModel
    private val viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private var pendingGeofenceReminderId: String? = null

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = viewModel.reminderTitle.value
            val description = viewModel.reminderDescription.value
            val location = viewModel.reminderSelectedLocationStr.value

            // Dividindo a string de localização em latitude e longitude
            val locationParts = location?.split(",")?.map { it.trim() }
            val latitude = locationParts?.getOrNull(0)?.toDoubleOrNull()
            val longitude = locationParts?.getOrNull(1)?.toDoubleOrNull()

            // Logs para diagnóstico
            Log.d(
                TAG,
                "Title: $title, Description: $description, Location: $location, Latitude: $latitude, Longitude: $longitude"
            )

            val reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)

            if(!reminderDataItem.location.isNullOrEmpty() || reminderDataItem.latitude != null || reminderDataItem.longitude != null) {
                if (viewModel.validateEnteredData(reminderDataItem)) {
                    if (checkPermission(ACCESS_FINE_LOCATION)) {
                        if (checkPermission(ACCESS_BACKGROUND_LOCATION)) {
                            if (checkPermission(POST_NOTIFICATIONS)) {
                                if (viewModel.validateAndSaveReminder(reminderDataItem)) {
                                    addGeofence(reminderDataItem.id)
                                    viewModel.onClear()
                                } else {
                                    Log.e(TAG, "Falha na validação do lembrete")
                                }
                            } else {
                                postNotificationPermissionRequest.launch(POST_NOTIFICATIONS)
                            }
                        } else {
                            backgroundLocationPermissionRequest.launch(ACCESS_BACKGROUND_LOCATION)
                        }
                    } else {
                        fineLocationPermissionRequest.launch(ACCESS_FINE_LOCATION)
                    }
                }
            } else if(reminderDataItem.title.isNullOrEmpty()) {
                Snackbar.make(view, getString(R.string.err_enter_title), Snackbar.LENGTH_LONG).show()
            }
            else {
                Snackbar.make(view, getString(R.string.err_select_location), Snackbar.LENGTH_LONG).show()
            }
        }


        binding.selectedLocation.text = viewModel.reminderSelectedLocationStr.value
        viewModel.reminderSelectedLocationStr.observe(viewLifecycleOwner) { newValue ->
            binding.selectedLocation.text = newValue ?: "no saved location"
        }
        viewModel.showToast.observe(viewLifecycleOwner) { message ->
            message.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown()
            }
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(reminderId: String?, resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(binding.root, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                        checkDeviceLocationSettingsAndStartGeofence(reminderId)
                    }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence(reminderId)
            }
        }
    }

    private fun addGeofence(reminderId: String?) {
        checkDeviceLocationSettingsAndStartGeofence(reminderId)
        val location = viewModel.reminderSelectedLocationStr.value

        val locationParts = location?.split(",")?.map { it.trim() }
        val latitude = locationParts?.getOrNull(0)?.toDoubleOrNull()
        val longitude = locationParts?.getOrNull(1)?.toDoubleOrNull()

        if (reminderId != null && latitude != null && longitude != null) {
            val geofence = Geofence.Builder()
                .setRequestId(reminderId)
                .setCircularRegion(
                    latitude, longitude, 50f
                )
                .setExpirationDuration(TimeUnit.HOURS.toMillis(1) * 24 * 7)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fineLocationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "Geofences Added: ${geofence.requestId}")
                }
                addOnFailureListener {
                    Log.d(TAG, getString(R.string.geofences_not_added))
                    if (it.message != null) {
                        Log.d(TAG, it.message!!)
                    }
                }
            }
        } else {
            // Tratar o caso de valores nulos aqui
            Log.e(TAG, "Não foi possível adicionar o geofence: valores nulos.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            pendingGeofenceReminderId?.let {
                checkDeviceLocationSettingsAndStartGeofence(it, false)
            }
        }
    }


    val fineLocationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (!checkPermission(ACCESS_BACKGROUND_LOCATION)) {
                    backgroundLocationPermissionRequest.launch(ACCESS_BACKGROUND_LOCATION)
                } else {
                    if (!checkPermission(POST_NOTIFICATIONS)) {
                        postNotificationPermissionRequest.launch(POST_NOTIFICATIONS)
                    }
                }
            } else {
                Toast.makeText(context, R.string.location_required_error, Toast.LENGTH_SHORT).show()
            }
        }

    val backgroundLocationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                if (!checkPermission(POST_NOTIFICATIONS)) {
                    postNotificationPermissionRequest.launch(POST_NOTIFICATIONS)
                }
            } else {
                Toast.makeText(context, R.string.location_required_error, Toast.LENGTH_SHORT).show()
            }
        }

    val postNotificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                    showDialogForPostNotification(requireContext())
                } else {
                    Toast.makeText(context, R.string.location_required_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    private fun showDialogForPostNotification(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Action")
        builder.setMessage("Are you sure you want to proceed?")

        builder.setPositiveButton("Yes") { _, _ ->
            postNotificationPermissionRequest.launch(POST_NOTIFICATIONS)
        }

        builder.setNegativeButton("No") { dialog, _ ->
            view?.let {
                Snackbar.make(it, R.string.location_required_error, Snackbar.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.onClear()
    }
}