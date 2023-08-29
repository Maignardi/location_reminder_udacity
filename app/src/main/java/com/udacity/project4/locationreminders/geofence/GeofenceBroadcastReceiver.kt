package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        geofencingEvent?.let {
            if (it.hasError()) {
                Log.e("GeofenceBroadcastReceiver", "Geofencing error: ${it.errorCode}")
                return
            }

            if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                GeofenceTransitionsJobIntentService.enqueueWork(context, intent)
            }
        }
    }
}