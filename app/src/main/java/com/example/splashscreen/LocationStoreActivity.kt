package com.example.splashscreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.location_store)

        map = findViewById(R.id.mapView)
        setupMap()

        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        val token = "Bearer ${getStoredToken()}"
        getSupplier(token)
        Navigation()
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        val compassOverlay = CompassOverlay(this, map)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)

        val mapController = map.controller
        mapController.setZoom(6.0)
        val indonesiaCenter = GeoPoint(-2.5489, 118.0149)
        mapController.setCenter(indonesiaCenter)
    }

    private fun Navigation() {
        val buttonProduk = findViewById<CardView>(R.id.home)
        val buttonProfile = findViewById<ImageButton>(R.id.profile)
        val buttonHistory = findViewById<ImageButton>(R.id.history)

        buttonProduk.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        buttonHistory.setOnClickListener {
            startActivity(Intent(this, CartCompleteActivity::class.java))
        }

        buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }

    }

    private fun getSupplier(token: String) {
        RetrofitClient.instance.getSupplier(token)
            .enqueue(object : Callback<SupplierResponse> {
                override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                    if (response.isSuccessful) {
                        allSuppliers = response.body()?.data ?: emptyList()
                        Log.d("SupplierResponse", "Suppliers: $allSuppliers")
                        addStoreMarkers()
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
        allSuppliers.forEach { supplier ->
            val coordinates = cityCoordinates[supplier.kota]
            if (coordinates != null) {
                addMarker(coordinates, supplier)
            } else {
                Log.w("LocationWarning", "Coordinates not found for city: ${supplier.kota}")
            }
        }
    }

    private fun addMarker(point: GeoPoint, supplier: SupplierData) {
        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = supplier.nama_toko
            snippet = "${supplier.alamat}\n${supplier.deskripsi}\nRating: ${supplier.rating}/5"

            // Set click listener for the marker
            setOnMarkerClickListener { marker, mapView ->
                val intent = Intent(this@LocationStoreActivity, TokoActivity::class.java).apply {
                    putExtra("supplierId", supplier.id.toString())
                    putExtra("supplierName", supplier.nama_toko)
                    putExtra("supplierKota", supplier.kota)
                    putExtra("supplierNegara", supplier.negara)
                    putExtra("supplierProvinsi", supplier.provinsi)
                    putExtra("supplierRating", supplier.rating)
                    putExtra("supplierImage", supplier.buyer?.id.toString())
                }
                startActivity(intent)
                true // Consume the event
            }
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