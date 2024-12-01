package com.dcoder.prokash.complaintSubmissionFragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.dcoder.prokash.R
import com.dcoder.prokash.databinding.FragmentLocationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.gestures.Utils.dpToPx
import com.mapbox.common.MapboxOptions
import com.mapbox.common.location.LocationProvider
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteOptions
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.mapbox.search.autocomplete.PlaceAutocompleteType
import com.mapbox.search.autofill.AddressAutofill
import com.mapbox.search.autofill.AddressAutofillOptions
import com.mapbox.search.autofill.AddressAutofillResult
import com.mapbox.search.autofill.AddressAutofillSuggestion
import com.mapbox.search.autofill.Query
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.result.SearchResult
import com.mapbox.search.ui.adapter.autocomplete.PlaceAutocompleteUiAdapter
import com.mapbox.search.ui.adapter.autofill.AddressAutofillUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.search.ui.view.place.SearchPlace
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.mapbox.search.ReverseGeoOptions

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    private lateinit var placeAutocomplete: PlaceAutocomplete

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var placeAutocompleteUiAdapter: PlaceAutocompleteUiAdapter

    private lateinit var queryEditText: EditText

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var searchPlaceView: SearchPlaceBottomSheetView

    private var ignoreNextQueryUpdate = false

    private var debounceJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val appContext = requireContext().applicationContext
        val applicationInfo = appContext.packageManager.getApplicationInfo(appContext.packageName, android.content.pm.PackageManager.GET_META_DATA)
        val metaData = applicationInfo.metaData

        MapboxOptions.accessToken = metaData?.getString("com.mapbox.token").toString()

        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placeAutocomplete = PlaceAutocomplete.create()

        queryEditText = binding.queryText

        mapView = binding.mapView
        mapView.mapboxMap.also { mapboxMap ->
            this.mapboxMap = mapboxMap

            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                mapView.location.updateSettings {
                    enabled = true
                }

                mapView.location.addOnIndicatorPositionChangedListener(object : OnIndicatorPositionChangedListener {
                    override fun onIndicatorPositionChanged(point: Point) {
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(14.0)
                                .build()
                        )

                        mapView.location.removeOnIndicatorPositionChangedListener(this)
                    }
                })
            }
        }

        mapMarkersManager = MapMarkersManager(mapView)

//        mapboxMap.addOnMapLongClickListener {
//            reverseGeocoding(it)
//            return@addOnMapLongClickListener true
//        }


        searchResultsView = binding.searchResultsView

        searchResultsView.initialize(
            SearchResultsView.Configuration(
                commonConfiguration = CommonSearchViewConfiguration()
            )
        )

        placeAutocompleteUiAdapter = PlaceAutocompleteUiAdapter(
            view = searchResultsView,
            placeAutocomplete = placeAutocomplete
        )

        searchPlaceView = binding.searchPlaceView.apply {
            initialize(CommonSearchViewConfiguration())

            isFavoriteButtonVisible = false

            addOnCloseClickListener {
                hide()
                closePlaceCard()
            }

            addOnNavigateClickListener { searchPlace ->
                //selection work HERE
                //startActivity(geoIntent(searchPlace.coordinate))
            }

            addOnShareClickListener { searchPlace ->
                //startActivity(shareIntent(searchPlace))
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (checkPermissions()){
            mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                // Enable location component
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
                getCurrentLocation()
                //updateLocationText()
            }

            mapView.mapboxMap.subscribeCameraChanged {
                debounceJob?.cancel()
                debounceJob = lifecycleScope.launch {
                    delay(500) // Wait 500ms before triggering reverse geocoding
                    val centerPoint = mapView.mapboxMap.cameraState.center
                    reverseGeocoding(centerPoint)
                }
            }

        }else{
            requestPermissions()
        }

        placeAutocompleteUiAdapter.addSearchListener(object : PlaceAutocompleteUiAdapter.SearchListener {

            override fun onSuggestionsShown(suggestions: List<PlaceAutocompleteSuggestion>) {
                // Nothing to do
            }

            override fun onSuggestionSelected(suggestion: PlaceAutocompleteSuggestion) {
                openPlaceCard(suggestion)
            }

            override fun onPopulateQueryClick(suggestion: PlaceAutocompleteSuggestion) {
                queryEditText.setText(suggestion.name)
            }

            override fun onError(e: Exception) {
                // Nothing to do
            }
        })

        queryEditText.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreNextQueryUpdate) {
                    ignoreNextQueryUpdate = false
                } else {
                    closePlaceCard()
                }

                lifecycleScope.launchWhenStarted {
                    placeAutocompleteUiAdapter.search(text.toString())
                    searchResultsView.isVisible = text.isNotEmpty()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun afterTextChanged(s: Editable) {
                // Nothing to do
            }
        })

    }

    private fun reverseGeocoding(point: Point) {

        lifecycleScope.launchWhenStarted {
            lateinit var placeQuery: String
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(point.latitude(), point.longitude(), 1)
            if (!addresses.isNullOrEmpty()) {
                placeQuery = addresses[0].getAddressLine(0)
            }

            val response = placeAutocomplete.suggestions(placeQuery)
            response.onValue { suggestions ->
                if (suggestions.isEmpty()) {
                    Toast.makeText(requireContext(), "Suggestion is Empty.", Toast.LENGTH_SHORT).show()
                } else {
                    openPlaceCard(suggestions.first())
                }
            }.onError { error ->
                Log.d(LOG_TAG, "Reverse geocoding error", error)
                Toast.makeText(requireContext(), "Reverse Geocoding Error.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun openPlaceCard(suggestion: PlaceAutocompleteSuggestion) {
        ignoreNextQueryUpdate = true
        queryEditText.setText("")

        lifecycleScope.launchWhenStarted {
            placeAutocomplete.select(suggestion).onValue { result ->
                suggestion.coordinate?.let { mapMarkersManager.showMarker(it,requireContext()) }
                searchPlaceView.open(SearchPlace.createFromPlaceAutocompleteResult(result))
                hideKeyboard()
                searchResultsView.isVisible = false
            }.onError { error ->
                Log.d(LOG_TAG, "Suggestion selection error", error)
                Toast.makeText(requireContext(), "Suggestion Selection Error.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closePlaceCard() {
        searchPlaceView.hide()
        mapMarkersManager.clearMarkers()
    }


    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusView = view?.rootView
        if (currentFocusView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
        }
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(lon, lat))
                        .zoom(14.0)
                        .build()
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Unable to fetch current location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateLocationText() {
        val center = mapView.mapboxMap.cameraState.center
        var lon = center.longitude()
        var lat = center.latitude()
        val text = "Lat: $lat, Lng: $lon"
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }


    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            101
        )
    }

    private class MapMarkersManager(mapView: MapView) {

        private val mapboxMap = mapView.getMapboxMap()
        private val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        private val markers = mutableMapOf<Long, Point>()

        fun clearMarkers() {
            markers.clear()
            pointAnnotationManager.deleteAll()
        }

        fun showMarker(coordinate: Point,context: Context) {
            clearMarkers()

            val iconBitmap: Bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_marker // Replace with your drawable resource name
            )
            val annotationOptions = PointAnnotationOptions()
                .withPoint(coordinate)
                .withIconImage(iconBitmap) // Add your marker icon image to the map style
                .withIconSize(1.5) // Adjust the size of the marker icon as needed

            val annotation = pointAnnotationManager.create(annotationOptions)
            markers[annotation.id.toLong()] = coordinate

            // Update the camera to focus on the marker
//            CameraOptions.Builder()
//                .center(coordinate)
//                .padding(MARKERS_INSETS_OPEN_CARD)
//                .zoom(14.0)
//                .build().also {
//                    mapboxMap.setCamera(it)
//                }
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(coordinate)
                    .zoom(14.0)
                    .build())
        }
    }

    private companion object {

        const val PERMISSIONS_REQUEST_LOCATION = 0

        const val LOG_TAG = "AutocompleteUiActivity"

        val MARKERS_EDGE_OFFSET = dpToPx(64F).toDouble()
        val PLACE_CARD_HEIGHT = dpToPx(300F).toDouble()
        val MARKERS_TOP_OFFSET = dpToPx(88F).toDouble()

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_TOP_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )

        val REGION_LEVEL_TYPES = listOf(
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region
        )

        val DISTRICT_LEVEL_TYPES = REGION_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District
        )

        val LOCALITY_LEVEL_TYPES = DISTRICT_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality
        )

        private val ALL_TYPES = listOf(
            PlaceAutocompleteType.Poi,
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region,
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District,
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality,
            PlaceAutocompleteType.AdministrativeUnit.Neighborhood,
            PlaceAutocompleteType.AdministrativeUnit.Street,
            PlaceAutocompleteType.AdministrativeUnit.Address,
        )
    }

}

