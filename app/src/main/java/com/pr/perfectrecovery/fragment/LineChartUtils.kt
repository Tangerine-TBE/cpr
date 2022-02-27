package com.pr.perfectrecovery.fragment

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IValueFormatter

object LineChartUtils {

    fun setLineChart(chart: LineChart, lineData: LineData) {
        (lineData.getDataSetByIndex(0) as LineDataSet).circleHoleColor = Color.TRANSPARENT
        (lineData.getDataSetByIndex(0) as LineDataSet).setDrawCircles(false)
        chart.setDrawGridBackground(false)
        // no description text
        chart.description.isEnabled = false
        chart.setNoDataText("no data")
        // enable touch gestures
        chart.setTouchEnabled(false)

        // enable scaling and dragging
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)

        val xl: XAxis = chart.xAxis
        xl.setAvoidFirstLastClipping(true)
        xl.axisMinimum = 0f
        xl.setDrawGridLines(false)
        xl.isEnabled = false

        val leftAxis: YAxis = chart.axisLeft
        leftAxis.isInverted = true
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        leftAxis.mAxisMaximum = 10f
        leftAxis.isEnabled = false
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(false)

        val rightAxis: YAxis = chart.axisRight
        rightAxis.isEnabled = false
        rightAxis.setDrawGridLines(false)

        val l = chart.legend
        l.isEnabled = false
        // don't forget to refresh the drawing

        // don't forget to refresh the drawing
        chart.invalidate()
    }
}