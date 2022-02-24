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
//    fun setLineChart(chart: LineChart, data: LineData) {
//        (data.getDataSetByIndex(0) as LineDataSet).circleHoleColor = Color.TRANSPARENT
//        (data.getDataSetByIndex(0) as LineDataSet).setDrawCircles(false)
//        (data.getDataSetByIndex(0) as LineDataSet).valueFormatter =
//            IValueFormatter { value, entry, dataSetIndex, viewPortHandler ->
//                //val df = DecimalFormat(".00")
//                //                return df.format(value) + "%";
//                "${value.toInt()}"
//            }
//
//        // no description text
//        chart.description.isEnabled = false
//        chart.setDrawGridBackground(false)
//        //设置规模Y启用
//        chart.isScaleYEnabled = false
//        //设置规模X启用
//        chart.isScaleXEnabled = false
////        chart.axisLeft.axisMinimum = 0f
////        chart.axisRight.axisMinimum = 0f
//        // enable touch gestures
//        chart.setTouchEnabled(false)
//        // enable scaling and dragging
//        chart.isDragEnabled = true
//        chart.setScaleEnabled(true)
//        chart.setNoDataText("")
//        // if disabled, scaling can be done on x- and y-axis separately
//        chart.setPinchZoom(false)
//        chart.setBackgroundColor(Color.TRANSPARENT)
//        // set custom chart offsets (automatic offset calculation is hereby disabled)
//        chart.setViewPortOffsets(10f, 0f, 10f, 0f)
//        // add data
//        chart.data = data
//        // get the legend (only possible after setting data)
//        val l = chart.legend
//        l.isEnabled = false
//        chart.axisRight.isEnabled = false
//        chart.axisLeft.isEnabled = false
//        chart.xAxis.isEnabled = false
//        chart.xAxis.textColor = Color.WHITE
//        chart.axisLeft.spaceTop = 10f
//        chart.axisLeft.spaceBottom = 10f
//        chart.axisLeft.spaceBottom = 0f
//
//        //是否缩放X轴
//        chart.isScaleXEnabled = true
//        //X轴缩放比例
////        chart.scaleX = 1f
//        // 图表左边的y坐标轴线
//        val leftAxis: YAxis = chart.axisLeft
//        val rightAxis: YAxis = chart.axisRight
//
////        chart.setVisibleXRangeMaximum(10f)
//        leftAxis.textColor = Color.WHITE
//        // 最小值
//        leftAxis.mAxisMinimum = 0f
//        rightAxis.mAxisMinimum = 0f
//        leftAxis.mAxisMaximum = 10f
//
//        leftAxis.setDrawGridLines(false)
//        val xAxis: XAxis = chart.xAxis
//        xAxis.axisMinimum = 0f
//        xAxis.setAvoidFirstLastClipping(true)
//        //保证Y轴从0开始，不然会上移一点
////        xAxis.setLabelCount(30, false) // 设置X轴的刻度数量，第二个参数表示是否平均分配
////        xAxis.granularity = 30f
////        xAxis.labelCount = 30
////        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        // animate calls invalidate()...
//        chart.animateX(1500)
//    }

    fun setLineChart(chart: LineChart, lineData: LineData) {
        (lineData.getDataSetByIndex(0) as LineDataSet).circleHoleColor = Color.TRANSPARENT
        (lineData.getDataSetByIndex(0) as LineDataSet).setDrawCircles(false)
        chart.setDrawGridBackground(false)
        // no description text
        chart.description.isEnabled = false

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
//        leftAxis.isEnabled = false
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