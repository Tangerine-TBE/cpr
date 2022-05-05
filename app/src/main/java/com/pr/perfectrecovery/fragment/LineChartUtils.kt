package com.pr.perfectrecovery.fragment

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData


object LineChartUtils {

    fun setLineChart(chart: LineChart, lineData: LineData, startNum: Int, endNum: Int) {
        Log.d("LineChartUtils", "startNum: $startNum endNum: $endNum")
        chart.setDrawGridBackground(false)
        // no description text
        chart.description.isEnabled = false
        chart.setNoDataText("")
        // enable touch gestures
        chart.setTouchEnabled(false)
        chart.setDrawBorders(false)
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
        xl.granularity = 0f
        xl.axisLineColor = Color.WHITE
        //设置限制线 70代表某个该轴某个值，也就是要画到该轴某个值上
        val limitLine = LimitLine(startNum.toFloat())
        //设置限制线的宽
        limitLine.lineWidth = 1f
        //设置限制线的颜色
        limitLine.lineColor = Color.parseColor("#3DB38E")
        //设置基线的位置
        limitLine.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
        limitLine.label = ""
        limitLine.textColor = Color.WHITE
        //设置限制线为虚线
        limitLine.enableDashedLine(10f, 10f, 0f)

        val limitLine2 = LimitLine(endNum.toFloat())
        //设置限制线的宽
        limitLine2.lineWidth = 1f
        //设置限制线的颜色
        limitLine2.lineColor = Color.parseColor("#3DB38E")
        //设置基线的位置
        limitLine2.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
        limitLine2.label = ""
        limitLine2.textColor = Color.WHITE
        //设置限制线为虚线
        limitLine2.enableDashedLine(10f, 10f, 0f)

        val leftAxis: YAxis = chart.axisLeft
        leftAxis.isInverted = true
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        leftAxis.mAxisMaximum = 10f
        leftAxis.labelCount = 10
        leftAxis.isEnabled = true
        leftAxis.textSize = 12f
        leftAxis.textColor = Color.WHITE
        leftAxis.gridColor = Color.TRANSPARENT
//        leftAxis.zeroLineColor = Color.TRANSPARENT
        leftAxis.setDrawGridLines(true)
        leftAxis.setValueFormatter { value, axis ->
            ""//${value.toInt()}
        }
        //左边Y轴添加限制线
        leftAxis.addLimitLine(limitLine)
        leftAxis.addLimitLine(limitLine2)
        val rightAxis: YAxis = chart.axisRight
        rightAxis.isEnabled = false
        rightAxis.setDrawGridLines(false)
//        rightAxis.mAxisMaximum = 5f
//        rightAxis.labelCount = 5
//        rightAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        val l = chart.legend
        l.isEnabled = false
        // don't forget to refresh the drawing

        // don't forget to refresh the drawing
        chart.invalidate()
    }
}