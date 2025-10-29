package com.sibsutis.autodimka

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {

    private lateinit var fusedClient: FusedLocationProviderClient

    private val campuses = listOf(
        Pair(55.013182, 82.950606), // корпус 1
        Pair(55.013813, 82.950037), // корпус 2
        Pair(55.013871, 82.948363),  // корпус 3
        Pair(55.017077, 82.949839) // корпус 5
    )

    private val radiusMeters = 85.0 // допустимый радиус

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val button = findViewById<Button>(R.id.btn_mark)

        button.setOnClickListener {
            checkLocationAndMark()
        }
    }

    private fun checkLocationAndMark() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                if (isInCampus(lat, lon)) {
                    Toast.makeText(this, "✅ Вы в университете! Отметка отправлена.", Toast.LENGTH_LONG).show()
                    // Здесь позже добавим отправку данных на сервер
                    val database = FirebaseDatabase.getInstance()
                    val attendanceRef = database.getReference("attendance")

// Пример данных о студенте
                    val studentId = "STU123" // позже заменим на реальный ID из авторизации

                    val record = mapOf(
                        "studentId" to studentId,
                        "latitude" to lat,
                        "longitude" to lon,
                        "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )

// сохраняем в базу
                    attendanceRef.push().setValue(record)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Отметка сохранена!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "❌ Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(this, "Вы не на месте!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isInCampus(lat: Double, lon: Double): Boolean {
        return campuses.any { (campLat, campLon) ->
            distance(lat, lon, campLat, campLon) <= radiusMeters
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000 // радиус Земли в метрах
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
