package com.example.dhakaparkdriver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    // This function is responsible for creating the entire custom window view.
    override fun getInfoWindow(marker: Marker): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)

        // The marker's "snippet" will hold our detailed data.
        val snippetData = marker.snippet?.split("\n")

        val spotName = marker.title
        val availableSlots = snippetData?.get(0)
        val operatingHours = snippetData?.get(1)
        val price = snippetData?.get(2)

        val tvSpotName = view.findViewById<TextView>(R.id.tv_spot_name)
        val tvAvailableSlots = view.findViewById<TextView>(R.id.tv_available_slots)
        val tvOperatingHours = view.findViewById<TextView>(R.id.tv_operating_hours)
        val tvPrice = view.findViewById<TextView>(R.id.tv_price)

        tvSpotName.text = spotName
        tvAvailableSlots.text = availableSlots
        tvOperatingHours.text = operatingHours
        tvPrice.text = price

        return view
    }

    // This function can be used to just customize the contents of the default window frame.
    // We return null here to indicate that we want to use our custom getInfoWindow() above.
    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}