package com.example.splashscreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yourapp.network.RetrofitClient
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LocationStoreActivity : AppCompatActivity() {

    private var allSuppliers: List<SupplierData> = listOf()

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    // Approximate coordinates for Indonesian cities
    private val cityCoordinates = mapOf(
        "Bogor" to GeoPoint(-6.5971, 106.8060),
        "Jakarta" to GeoPoint(-6.2088, 106.8456),
        "Surabaya" to GeoPoint(-7.2575, 112.7521),
        "Bandung" to GeoPoint(-6.9175, 107.6191),
        "Semarang" to GeoPoint(-6.9667, 110.4167),
        "Yogyakarta" to GeoPoint(-7.7956, 110.3695),
        "Malang" to GeoPoint(-7.9666, 112.6326),
        "Cirebon" to GeoPoint(-6.7063, 108.5570),
        "Tangerang" to GeoPoint(-6.1783, 106.6319),
        "Bekasi" to GeoPoint(-6.2383, 106.9756)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.location_store)

        // Initialize map
        map = findViewById(R.id.mapView)
        setupMap()

        // Request permissions
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
        // Fetch suppliers
        val token = "Bearer ${getStoredToken()}" // Replace with actual token retrieval
        getSupplier(token)

    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Add location overlay
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        // Add compass
        val compassOverlay = CompassOverlay(this, map)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)

        // Set default zoom and center on Indonesia
        val mapController = map.controller
        mapController.setZoom(6.0)
        val indonesiaCenter = GeoPoint(-2.5489, 118.0149) // Center of Indonesia
        mapController.setCenter(indonesiaCenter)
    }

    private fun getSupplier(token: String) {
        RetrofitClient.instance.getSupplier(token)
            .enqueue(object : Callback<SupplierResponse> {
                override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                    if (response.isSuccessful) {
                        allSuppliers = response.body()?.data ?: emptyList()
                        Log.d("SupplierResponse", "Suppliers: $allSuppliers")
                        addStoreMarkers() // Add markers after data is fetched
                    } else {
                        Log.e("SupplierError", "Failed to fetch supplier data: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                    Log.e("SupplierError", "Error: ${t.message}")
                }
            })
    }

    private fun addStoreMarkers() {
        allSuppliers.forEach { store ->
            val coordinates = cityCoordinates[store.kota]
            if (coordinates != null) {
                addMarker(
                    coordinates,
                    store.nama_toko,
                    "${store.alamat}\n${store.deskripsi}\nRating: ${store.rating}/5"
                )
            } else {
                Log.w("LocationWarning", "Coordinates not found for city: ${store.kota}")
            }
        }
    }

    private fun addMarker(point: GeoPoint, title: String, snippet: String) {
        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            this.snippet = snippet
        }
        map.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) {
                        locationOverlay.enableMyLocation()
                    }
                }
            }
        }
    }
}
