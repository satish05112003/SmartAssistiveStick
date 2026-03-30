package com.satis.caregiver.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class MapManager(private val context: Context) {
    fun openNavigation(lat: Double, lng: Double) {
        val uri = "google.navigation:q=$lat,$lng"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val webUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
            context.startActivity(webIntent)
        }
    }

    fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent)
    }
}
