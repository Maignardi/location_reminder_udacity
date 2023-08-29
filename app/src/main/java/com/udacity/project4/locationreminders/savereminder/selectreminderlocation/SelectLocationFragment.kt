package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geofencePendingIntent: PendingIntent
    private lateinit var map: GoogleMap
    private var selectedLocation: LatLng? = null
    private val PERMISSIONS_REQUEST_CODE = 1
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 2
    private var geofenceIdCounter = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        binding.saveButton.isEnabled = false
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)
        saveLocationOnCache()
        fineLocationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        return binding.root
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        if (!::geofencePendingIntent.isInitialized) {
            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            geofencePendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return geofencePendingIntent
    }

    private fun saveLocationOnCache() {
        binding.saveButton.setOnClickListener {
            selectedLocation?.let { location ->
                val sharedPreferences = requireActivity().getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putFloat("latitude", location.latitude.toFloat())
                    putFloat("longitude", location.longitude.toFloat())
                    apply()
                }
                Toast.makeText(requireContext(), "Location saved", Toast.LENGTH_SHORT).show()

                addGeofenceForLocation(location) // Adicionando o geofence

                findNavController().popBackStack()
            } ?: run {
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addGeofenceForLocation(location: LatLng) {
        val geofenceId = "Geofence_${location.latitude}_${location.longitude}"

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(location.latitude, location.longitude, 100f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(30000)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getGeofencingClient(requireActivity())
                .addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener {
                    if (isAdded) { // Verifica se o fragmento ainda está anexado
                        Toast.makeText(requireContext(), "Geofence added", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    if (isAdded) { // Verifica se o fragmento ainda está anexado
                        Toast.makeText(requireContext(), "Failed to add geofence", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        enableMyLocation()
        setMapStyle(map)
        setMapClickListener(map)
        setPoiClickListener(map)
    }

    private fun setMapClickListener(map: GoogleMap) {
        map.setOnMapClickListener { location ->
            map.clear()
            map.addMarker(MarkerOptions().position(location))
            selectedLocation = location
            binding.saveButton.isEnabled = true
        }
    }

    private fun setPoiClickListener(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            map.addMarker(MarkerOptions().position(poi.latLng))
            selectedLocation = poi.latLng
            binding.saveButton.isEnabled = true
        }
    }

    private fun applyMapStyle(map: GoogleMap, styleId: Int) {
        val success = map.setMapStyle(context?.let { MapStyleOptions.loadRawResourceStyle(it, styleId) })
        if (!success) {
            println("Failed to apply map style.")
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        applyMapStyle(map, R.raw.map_standard)
    }

    private fun enableMyLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            map.isMyLocationEnabled = true
            requestCurrentLocation()
        } else {
            fineLocationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val fineLocationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(context, R.string.permission_denied_explanation, Toast.LENGTH_SHORT).show()
            }
        }


    private fun requestCurrentLocation() {
        map.isMyLocationEnabled = true
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }.addOnFailureListener {
                // Tratar falhas ao obter a localização
                Toast.makeText(requireContext(), "Error obtaining location", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Solicitar permissões se ainda não foram concedidas
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.normal_map -> map.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.hybrid_map -> map.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.satellite_map -> map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.terrain_map -> map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            R.id.dark_map -> applyMapStyle(map, R.raw.map_dark)
            R.id.night_map -> applyMapStyle(map, R.raw.map_night)
            R.id.retro_map -> applyMapStyle(map, R.raw.map_retro)
            R.id.silver_map -> applyMapStyle(map, R.raw.map_silver)
        }
        return true
    }
}