package com.example.travelcompanion.ui.analysis_prediction

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.example.travelcompanion.R

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    //Empty constructor for XML inflation
    constructor(context: Context) : this(context, R.layout.marker_view)

    private val tvMarker: TextView = findViewById(R.id.tvMarker)
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvMarker.text = "Value: ${e?.y?.toInt()}"
        super.refreshContent(e, highlight)
    }
}