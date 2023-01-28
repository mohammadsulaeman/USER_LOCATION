package com.example.userlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.OnLocationClickListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraIdleListener
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class PicklocationActivity : AppCompatActivity(), OnMapReadyCallback,
    OnCameraIdleListener, OnLocationClickListener, OnCameraTrackingChangedListener,
    PermissionsListener {
    private val lastLocation: Location? = null
    private  var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var mapboxMap: MapboxMap? = null
    lateinit var autoCompleteTextView: TextView
    private var locationComponent: LocationComponent? = null
    lateinit var currentAddress: TextView
    lateinit var mapView: MapView
    lateinit var selectLocation: Button
    lateinit var backbutton: FloatingActionButton
    lateinit var currentLocation: FloatingActionButton

    @CameraMode.Mode
    private var cameraMode = CameraMode.TRACKING

    @RenderMode.Mode
    private var renderMode = RenderMode.GPS
    private var formToFill = 0
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_picklocation)
        autoCompleteTextView = findViewById(R.id.locationPicker_autoCompleteText)
        currentAddress = findViewById(R.id.locationPicker_currentAddress)
        selectLocation = findViewById(R.id.locationPicker_destinationButton)
        backbutton = findViewById(R.id.back_btn)
        currentLocation = findViewById(R.id.currentlocation)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        val intent = intent
        formToFill = intent.getIntExtra(FORM_VIEW_INDICATOR, -1)
        selectLocation.setOnClickListener(View.OnClickListener { view: View? -> selectLocation() })
        backbutton.setOnClickListener(View.OnClickListener { view: View? -> finish() })
        autoCompleteTextView.setOnClickListener(View.OnClickListener { v: View? -> setupAutocompleteTextView() })
    }

    private fun selectLocation() {
        val selectedLocation = mapboxMap!!.cameraPosition.target
        val selectedAddress = currentAddress.text.toString()
        val intent = Intent()
        intent.putExtra(FORM_VIEW_INDICATOR, formToFill)
        intent.putExtra(LOCATION_NAME, selectedAddress)
        intent.putExtra(LOCATION_LATLNG, selectedLocation)
        setResult(RESULT_OK, intent)
        finish()
    }

    @SuppressLint("Range")
    private fun setupAutocompleteTextView() {
        val intent = PlaceAutocomplete.IntentBuilder()
            .accessToken(
                (if (Mapbox.getAccessToken() != null) Mapbox.getAccessToken() else getString(
                    R.string.mapbox_access_token
                ))!!
            )
            .placeOptions(
                PlaceOptions.builder()
                    .backgroundColor(Color.parseColor("#EEEEEE"))
                    .limit(15)
                    .build(PlaceOptions.MODE_CARDS)
            )
            .build(this@PicklocationActivity)
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onMapReady(mapboxMap: MapboxMap) {
        this@PicklocationActivity.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.LIGHT) { style: Style? ->
            val uiSettings = mapboxMap.uiSettings
            uiSettings.isAttributionEnabled = false
            uiSettings.isLogoEnabled = false
            uiSettings.isCompassEnabled = false
            locationComponent = mapboxMap.locationComponent
            locationComponent!!.activateLocationComponent(
                LocationComponentActivationOptions
                    .builder(this@PicklocationActivity, style!!)
                    .useDefaultLocationEngine(true)
                    .locationEngineRequest(
                        LocationEngineRequest.Builder(750)
                            .setFastestInterval(750)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .build()
                    )
                    .build()
            )
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setStyle
            }
            locationComponent!!.isLocationComponentEnabled = true
            locationComponent!!.addOnLocationClickListener(this@PicklocationActivity)
            locationComponent!!.addOnCameraTrackingChangedListener(this@PicklocationActivity)
            locationComponent!!.cameraMode = cameraMode
            setRendererMode(renderMode)
            locationComponent!!.forceLocationUpdate(lastLocation)
            val position = CameraPosition.Builder()
                .target(
                    LatLng(
                        Objects.requireNonNull(
                            locationComponent!!.lastKnownLocation
                        )!!.latitude, locationComponent!!.lastKnownLocation!!
                            .longitude
                    )
                )
                .zoom(15.0)
                .tilt(20.0)
                .build()
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 100)
            currentLocation!!.setOnClickListener { view: View? ->
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(position),
                    1000
                )
            }
            mapboxMap.addOnCameraIdleListener(this@PicklocationActivity)
        }
    }

    private fun setRendererMode(@RenderMode.Mode mode: Int) {
        renderMode = mode
    }

    override fun onCameraIdle() {
        if (mapboxMap != null) {
            val reverseGeocode = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(
                    Point.fromLngLat(
                        mapboxMap!!.cameraPosition.target.longitude,
                        mapboxMap!!.cameraPosition.target.latitude
                    )
                )
                .build()
            reverseGeocode.enqueueCall(object : Callback<GeocodingResponse?> {
                override fun onResponse(
                    call: Call<GeocodingResponse?>,
                    response: Response<GeocodingResponse?>
                ) {
                    val results = Objects.requireNonNull(response.body())!!.features()
                    if (results.size > 0) {
                        val feature = results[0]
                        currentAddress!!.text = feature.placeName()
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse?>, throwable: Throwable) {
                    throwable.printStackTrace()
                }
            })
        }
    }

    override fun onCameraTrackingDismissed() {}
    override fun onCameraTrackingChanged(currentMode: Int) {
        cameraMode = currentMode
    }

    override fun onLocationComponentClick() {}
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)
            if (mapboxMap != null) {
                currentAddress.text = selectedCarmenFeature.placeName()
                val style = mapboxMap!!.style
                if (style != null) {
                    val geojsonSourceLayerId = "geojsonSourceLayerId"
                    val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                    source?.setGeoJson(
                        FeatureCollection.fromFeatures(
                            arrayOf(
                                Feature.fromJson(
                                    selectedCarmenFeature.toJson()
                                )
                            )
                        )
                    )
                    mapboxMap!!.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(
                                    LatLng(
                                        (Objects.requireNonNull(selectedCarmenFeature.geometry()) as Point).latitude(),
                                        (selectedCarmenFeature.geometry() as Point?)!!.longitude()
                                    )
                                )
                                .zoom(15.0)
                                .build()
                        ), 4000
                    )
                }
            }
        }
    }

    companion object {
        const val LOCATION_PICKER_ID = 78
        const val FORM_VIEW_INDICATOR = "FormToFill"
        const val LOCATION_NAME = "LocationName"
        const val LOCATION_LATLNG = "LocationLatLng"
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
    }

    override fun onExplanationNeeded(p0: MutableList<String>?) {}

    override fun onPermissionResult(p0: Boolean) {}
}