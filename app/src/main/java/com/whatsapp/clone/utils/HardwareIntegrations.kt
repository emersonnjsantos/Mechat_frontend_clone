package com.whatsapp.clone.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object HardwareIntegrations {

    /**
     * Reads real GPS location from device hardware sensors (GPS / Network).
     */
    @SuppressLint("MissingPermission")
    fun getDeviceGpsLocation(context: Context): Pair<Double, Double>? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val gpsLocation: Location? = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            null
        }

        val networkLocation: Location? = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }

        val bestLocation = gpsLocation ?: networkLocation
        return if (bestLocation != null) {
            Pair(bestLocation.latitude, bestLocation.longitude)
        } else {
            null
        }
    }

    /**
     * Reads real contacts saved on the phone via ContactsContract.
     */
    fun getPhoneContacts(context: Context): List<Pair<String, String>> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return emptyList()

        val contactsList = mutableListOf<Pair<String, String>>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext() && contactsList.size < 50) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) else "Sem nome"
                val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                contactsList.add(Pair(name, number))
            }
        }
        return contactsList
    }

    /**
     * Interacts with all other installed apps on the mobile device via Android Share Intent Chooser.
     */
    fun shareWithInstalledApps(context: Context, text: String, title: String = "Compartilhar mensagem via") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooserIntent = Intent.createChooser(sendIntent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooserIntent)
    }

    /**
     * Opens location in Google Maps or default map provider installed on device.
     */
    fun openMaps(context: Context, latitude: Double, longitude: Double) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Localização+do+WhatsApp)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Browser fallback
            val webUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
