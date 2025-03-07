@file:Suppress("DEPRECATION")

package com.dcoder.prokash.complaintSubmissionFragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcoder.prokash.R
import com.dcoder.prokash.adapter.AddressSuggestionAdapter
import com.dcoder.prokash.data.api.NominatimClient
import com.dcoder.prokash.data.model.NominatimResponse
import com.dcoder.prokash.databinding.FragmentLocationPickingBinding
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import java.io.File


class LocationPickingFragment : Fragment() {

    private var _binding: FragmentLocationPickingBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var addressAdapter: AddressSuggestionAdapter
    private var addressList = mutableListOf<NominatimResponse>()

    private val debounceScope = CoroutineScope(Dispatchers.Main + Job())
    private var searchJob: Job? = null

    private val viewModel: ComplaintSubmissionViewModel by activityViewModels()

    private val REQUEST_CHECK_SETTINGS = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLocationPickingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().osmdroidTileCache = File(context?.cacheDir, "osmdroid")
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().osmdroidTileCache = File(context?.cacheDir, "osmdroid")

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setBuiltInZoomControls(true)
        binding.mapView.setMultiTouchControls(true)

        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(15.0) // Set default zoom level
        // Set default map center (latitude, longitude)
        var startPoint = GeoPoint(23.8041, 90.4152)
        if (viewModel.locationLongitude.value!=null && viewModel.locationLatitude.value!=null){
            startPoint = GeoPoint(viewModel.locationLatitude.value!!,viewModel.locationLongitude.value!!)
        }
        mapController.setCenter(startPoint)

        addressAdapter = AddressSuggestionAdapter(addressList) { selectedAddress ->
            val lat = selectedAddress.lat.toDouble()
            val lon = selectedAddress.lon.toDouble()
            val geoPoint = GeoPoint(lat, lon)
            binding.mapView.controller.setCenter(geoPoint)
            binding.mapView.controller.setZoom(18.0)
            binding.recyclerView.visibility = View.GONE
            //showBottomSheet(geoPoint)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = addressAdapter


        binding.queryText.addTextChangedListener { text ->
            val query = text.toString()
            if (query.isNotEmpty()) {
                binding.recyclerView.visibility = View.VISIBLE
                searchAddresses(query)
            } else {
                addressList.clear()
                addressAdapter.notifyDataSetChanged()
                binding.recyclerView.visibility = View.GONE
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


                binding.gpsButton.setOnClickListener {
                    if (!isInternetAvailable(requireContext())) {
                        Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (isGpsEnabled || isNetworkEnabled) {
                        // Location is enabled, check permissions and move to current location
                        if (checkPermissions()) {
                            moveToCurrentLocation()
                        } else {
                            requestPermissions()
                        }
                    } else {
                        // Request to enable location services
                        enableLocationServices()
                    }
                }


        binding.mapView.addMapListener(object : MapListener {
            private var dragHandler: android.os.Handler? = null
            private val dragEndRunnable = Runnable {
                // This runs after dragging stops
                val centerPoint = binding.mapView.mapCenter
                onDragEnd(centerPoint)
            }
            override fun onScroll(event: ScrollEvent?): Boolean {
                // Called during a scroll event
                if (event != null) {
                    val center = binding.mapView.mapCenter as GeoPoint
                    // Update the selected location while scrolling (optional)
                    onDragging(center)

                    // Reset the drag-end handler to detect when dragging stops
                    dragHandler?.removeCallbacks(dragEndRunnable)
                    dragHandler = android.os.Handler()
                    dragHandler?.postDelayed(dragEndRunnable, 1000) // 500ms delay to detect drag end
                }
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                // Called during a zoom event
                if (event != null) {
                    val center = binding.mapView.mapCenter as GeoPoint
                    // Update the selected location after zooming
                    onDragging(center)

                    // Reset the drag-end handler to detect when dragging stops
                    dragHandler?.removeCallbacks(dragEndRunnable)
                    dragHandler = android.os.Handler()
                    dragHandler?.postDelayed(dragEndRunnable, 1000)
                }
                return true
            }

            private fun onDragging(centerPoint: IGeoPoint) {
                // Update UI dynamically while dragging

            }

            private fun onDragEnd(centerPoint: IGeoPoint) {
                // Handle the selected location based on the new center of the map
                showBottomSheet(centerPoint)
            }
        })


    }

    // Function to check if internet is available (Wi-Fi or Mobile Data)
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // Function to request GPS enable dialog
    private fun enableLocationServices() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show dialog if needed

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS is enabled
            Toast.makeText(requireContext(), "GPS is already enabled", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show GPS enable dialog
                    exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Open settings if dialog is not available
                Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }


    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Show a rationale to the user
            Toast.makeText(
                requireContext(),
                "Location permission is required to use this feature.",
                Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            101
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED || grantResults[2] == PackageManager.PERMISSION_GRANTED)
            ) {
                moveToCurrentLocation()
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                binding.mapView.controller.setCenter(geoPoint)
                binding.mapView.controller.setZoom(20)
                showBottomSheet(geoPoint)
            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun searchAddresses(query: String) {
        searchJob?.cancel() // Cancel the previous job if a new query is typed

        searchJob = debounceScope.launch {
            delay(1000) // Wait for 1 second before making the API call

            try {
                val response = NominatimClient.searchService.searchAddress(query)
                addressList.clear()
                addressList.addAll(response)
                addressAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d("edit error", "searchAddresses: ${e.cause}")
            }
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun showBottomSheet(centerPoint: IGeoPoint) {

        // Create a BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        // Inflate the bottom sheet layout
        val bottomSheetView: View = LayoutInflater.from(requireContext())
            .inflate(R.layout.location_bottom_sheet_layout,null)

        // Set the content view for the dialog
        bottomSheetDialog.setContentView(bottomSheetView)

        // Find views in the bottom sheet and set up actions
        val closeButton: ImageView = bottomSheetView.findViewById(R.id.sheet_close)
        val addressTextView: TextView = bottomSheetView.findViewById(R.id.sheet_address)
        val locationTextView: TextView = bottomSheetView.findViewById(R.id.sheet_coordinate)
        val submitButton: Button = bottomSheetView.findViewById(R.id.sheet_submit)

        searchJob?.cancel() // Cancel the previous job if a new query is typed

        searchJob = debounceScope.launch {
            try {
                val response = NominatimClient.reverseService.reverseGeocode(
                    centerPoint.latitude,
                    centerPoint.longitude
                )

                addressTextView.text = response.display_name
                locationTextView.text = "${centerPoint.latitude}, ${centerPoint.longitude}"
                bottomSheetDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d("edit error", "showBottomSheet: ${e.message}")
            }
        }
        closeButton.setOnClickListener {
            bottomSheetDialog.dismiss() // Close the dialog when the button is clicked
        }

        submitButton.setOnClickListener {
            //select the coordinate and move to next step
            viewModel.setLocation(centerPoint.longitude,centerPoint.latitude)
            viewModel.setSelectedTab(5)
            bottomSheetDialog.cancel()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetailsFragment())
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceScope.cancel()
    }


}
