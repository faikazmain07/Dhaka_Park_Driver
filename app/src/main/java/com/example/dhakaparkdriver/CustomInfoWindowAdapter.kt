package com.example.dhakaparkdriver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

// Note: We are no longer passing a list of data to the adapter.
// All the data it needs will come directly from the Marker object itself.
class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    // This function inflates our custom layout and populates it with data.
    private fun setInfoWindowText(marker: Marker, view: View) {
        // Safely get the title, provide a default if it's null
        val title = marker.title ?: "Unknown Spot"
        // Safely get the snippet, provide a default if it's null
        val snippet = marker.snippet ?: "No details available"

        // Find the TextViews in our custom layout
        val tvTitle = view.findViewById<TextView>(R.id.tv_spot_name)
        val tvSnippet = view.findViewById<TextView>(R.id.tv_details) // We'll update the XML to use this ID

        // Set the text
        tvTitle.text = title
        tvSnippet.text = snippet
    }

    // This is called by the map to get the view for the entire info window (including the frame).
    override fun getInfoWindow(marker: Marker): View? {
        // Inflate our custom layout file
        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
        setInfoWindowText(marker, view)
        return view
    }

    // This is called to get the view for just the *contents* of the default info window.
    // We return null because we are providing the entire window view in getInfoWindow().
    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}