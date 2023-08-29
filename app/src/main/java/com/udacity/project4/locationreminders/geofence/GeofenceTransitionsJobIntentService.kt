package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1234
        private const val CHANNEL_ID = "geofence_channel_id"
        private const val CHANNEL_NAME = "Geofence Notifications"
        private const val CHANNEL_DESCRIPTION = "Notificações para eventos de geofence"
        private const val NOTIFICATION_ID = 1

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, JOB_ID, intent)
        }

        fun addGeofenceForReminder(context: Context, reminderDataItem: ReminderDataItem) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.e("Add Geofence", "Device location is turned off.")
                return
            }

            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Add Geofence", "Required permissions not granted.")
                    // Aqui, você pode considerar pedir as permissões ou notificar o usuário
                    return
                }
            }

            val location = reminderDataItem.location
            if (location.isNullOrEmpty() || !location.contains(",")) {
                Log.e("Add Geofence", "Location is invalid. Cannot create Geofence.")
                return
            }

            val latLngArray = location.split(",").map { it.toDoubleOrNull() }
            if (latLngArray.size != 2 || latLngArray.any { it == null }) {
                Log.e("Add Geofence", "Location format is invalid. Cannot create Geofence.")
                return
            }

            val (latitude, longitude) = latLngArray
            val geofence = Geofence.Builder()
                .setRequestId(reminderDataItem.id)
                .setCircularRegion(latitude!!, longitude!!, 1000f) // Raio definido como 1000 metros
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(30000) // Tempo de permanência de 30 segundos para a transição DWELL
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofence(geofence)
                .build()

            val geofencePendingIntent: PendingIntent by lazy {
                val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            val geofencingClient = LocationServices.getGeofencingClient(context)
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.i("Add Geofence", "Geofence added for: ${geofence.requestId}")
                }
                addOnFailureListener {
                    Log.e("Add Geofence", "Failed to add geofence: ${it.message}")
                }
            }
        }

        fun checkDeviceLocationSettingsAndStartGeofence(activity: Activity, context: Context, reminderDataItem: ReminderDataItem, resolve: Boolean = true) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(activity)
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {
                    try {
                        exception.startResolutionForResult(activity, REQUEST_TURN_DEVICE_LOCATION_ON)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Handle the error here
                    }
                } else {
                    Toast.makeText(context, "Please turn on your device location.", Toast.LENGTH_LONG).show()
                }
            }

            locationSettingsResponseTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    addGeofenceForReminder(context, reminderDataItem)
                }
            }
        }

        fun setupAllGeofences(context: Context) {
            val remindersLocalRepository = getKoin().get<ReminderDataSource>()
            CoroutineScope(Dispatchers.IO).launch {
                val result = remindersLocalRepository.getReminders()
                if (result is Result.Success<List<ReminderDTO>>) {
                    for (reminder in result.data) {
                        addGeofenceForReminder(context, ReminderDataItem(reminder.title, reminder.description, reminder.location, reminder.latitude, reminder.longitude, reminder.id))
                    }
                }
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                Log.e("GeofenceTransitionsJobIntentService", "Geofencing error: ${geofencingEvent.errorCode}")
                return
            }
        }

        if (geofencingEvent != null) {
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

                Log.d("GeofenceService", "Geofence transition detected: ${geofencingEvent.geofenceTransition}")

                if (geofencingEvent != null) {
                    geofencingEvent.triggeringGeofences?.let { triggeringGeofences ->
                        sendNotification(triggeringGeofences)
                    }
                }
            }
        }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        val requestId = triggeringGeofences.firstOrNull()?.requestId ?: return
        Log.d("GeofenceService", "sendNotification called for requestId: $requestId")

        val remindersLocalRepository: ReminderDataSource by inject()
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            val result = remindersLocalRepository.getReminder(requestId)
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data

                val notificationManager = ContextCompat.getSystemService(
                    applicationContext, NotificationManager::class.java) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = CHANNEL_DESCRIPTION
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                fun convertDtoToDataItem(dto: ReminderDTO): ReminderDataItem {
                    return ReminderDataItem(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        location = dto.location,
                        latitude = dto.latitude,
                        longitude = dto.longitude
                    )
                }

                val intent = ReminderDescriptionActivity.newIntent(applicationContext, convertDtoToDataItem(reminderDTO))
                val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(reminderDTO.title)
                    .setContentText(reminderDTO.description)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(requestId.hashCode(), notification)
            }
        }
    }

}