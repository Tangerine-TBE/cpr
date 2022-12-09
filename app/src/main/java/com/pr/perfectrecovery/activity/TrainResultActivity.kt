package com.pr.perfectrecovery.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ToastUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.bean.Coordinates
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivityTrainResultBinding
import com.pr.perfectrecovery.fragment.LineChartUtils
import com.pr.perfectrecovery.utils.TimeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.HashMap
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * 训练结果-操作明细 成绩结果
 * Time 2022年2月8日22:14:36
 * author lrz
 */

class TrainResultActivity : BaseActivity(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {
    private lateinit var viewBinding: ActivityTrainResultBinding
    private val TAG = TrainResultActivity::class.java.simpleName
    private val values = ArrayList<BarEntry>()

    companion object {
        val DATADTO = "dataDTO"
        val PDF_FLAG = "pdf_flag"
        var isMulti = false
        fun start(context: Context, trainingDTO: TrainingDTO, multi: Boolean? = false) {
            val intent = Intent(context, TrainResultActivity::class.java)
            isMulti = multi == true
            intent.putExtra(DATADTO, trainingDTO)
            context.startActivity(intent)
        }

        fun start(context: Context, trainingDTO: TrainingDTO, flagPDF: Boolean) {
            val intent = Intent(context, TrainResultActivity::class.java)
            intent.putExtra(DATADTO, trainingDTO)
            intent.putExtra(PDF_FLAG, flagPDF)
            context.startActivity(intent)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityTrainResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initData()
        initView()
        viewBinding.mainLayout.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                viewBinding.bar.onTouchEvent(event)
                viewBinding.chart.onTouchEvent(event)
                viewBinding.chart1.onTouchEvent(event)
                viewBinding.chart2.onTouchEvent(event)
                return true
            }
        })
    }

    private fun <A> splitList(
        list: MutableList<A>
    ): Map<String, MutableList<A>> {
        val num = list.size / 3
        var index = 1
        val listSize = list.size //list 长度
        val stringListHashMap = HashMap<String, MutableList<A>>() //用户封装返回的多个list
        var stringlist: MutableList<A> = java.util.ArrayList()
        //用于承装每个等分list
        for (i in 0 until listSize) {                        //for循环依次放入每个list中
            stringlist.add(list[i]) //先将string对象放入list,以防止最后一个没有放入
            if ((i + 1) % num == 0 || i + 1 == listSize) { //如果l+1 除以 要分的份数 为整除,或者是最后一份,为结束循环.那就算作一份list,
                if (index >= 4) {
                    stringListHashMap["3"]?.addAll(stringlist)
                } else {
                    stringListHashMap["$index"] = stringlist //将这一份放入Map中.
                }
                index++
                stringlist = java.util.ArrayList() //新建一个list,用于继续存储对象
            }
        }
        return stringListHashMap //将map返回
    }


    @SuppressLint("CheckResult")
    private fun initScatterChart(
        scatterChart: ScatterChart,
        lineChartY1: ArrayList<Float>,
        lineChartY2: ArrayList<Float>,
        type: Int
    ) {
        /*1.主要对按压的数值进行坐标分析*/
        /*2.对源数据进行三种类型的划为 1.几近过期的 2.中间生成的 3.最近生成的*/
        /*3.这里简单点，对数据进行按照时间节点均等划分为三份，一一对应上方所说*/
        /*4.对于几近过期的数据点位采用蓝色区分，中间生成的数据点位采用橙色划分，最近生成的数据点位用红色划分*/
        /*5.对于三种类型数据出现重叠时，一般会按照时间节点进行累积*/
        /*6.x轴为按压单位为按压次数 0~120 y轴为按压深度 0~8*/

        /*1.初始化坐标轴控件*/
        /*2.x轴为置于上方，y轴为置于右方*/
        scatterChart.description.isEnabled = false
        scatterChart.setTouchEnabled(false)
        scatterChart.isHighlightPerTapEnabled = false
        scatterChart.isHighlightPerDragEnabled = false
        // enable scaling and dragging
        scatterChart.isDragEnabled = true
        scatterChart.setScaleEnabled(false)
        scatterChart.setDrawGridBackground(false)
        scatterChart.setPinchZoom(false)
        val l: Legend = scatterChart.legend
        l.isEnabled = false
        val yl: YAxis = scatterChart.axisRight
        yl.axisLineColor = Color.WHITE
        yl.axisMinimum = 2f // this replaces setStartAtZero(true)
        yl.axisMaximum = 8f
        yl.granularity = 2f
        yl.isInverted = true
        yl.textColor = Color.WHITE
        yl.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                return when (value.toString()) {
                    "0.0" -> {
                        return ""
                    }
                    "2.0" -> {
                        return ""
                    }
                    "4.0" -> {
                        return if (type == 2) "4CM" else "400ML"
                    }
                    "6.0" -> {
                        return if (type == 2) "6CM" else "600ML"
                    }
                    "8.0" -> {
                        return ""
                    }
                    "10.0" -> {
                        return ""
                    }

                    else -> {
                        return ""
                    }
                }
            }
        }
        scatterChart.axisLeft.isEnabled = false
        val xl: XAxis = scatterChart.xAxis
        xl.axisLineColor = Color.WHITE
        xl.textColor = Color.WHITE
        xl.axisMinimum = 3f //90~130怎么分配的呢
        xl.granularity =3f
        xl.axisMaximum = 12f
        xl.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                return when (value.toString()) {
                    "0.0" -> {
                        return ""
                    }
                    "3.0" -> {
                        return ""
                    }
                    "6.0" -> {
                        return if (type == 2) "100CPM" else "6VPM"
                    }
                    "9.0" -> {
                        return if (type == 2) "120CPM" else "8VPM"
                    }
                    "12.0" -> {
                        return ""
                    }
                    "15.0" -> {
                        return ""
                    }
                    else -> {
                        return ""
                    }
                }
            }
        }
        io.reactivex.Observable.create<ArrayList<ArrayList<Entry>>> {
            val values1 = ArrayList<Entry>()
            val values2 = ArrayList<Entry>()
            val values3 = ArrayList<Entry>()
            val beans = ArrayList<Coordinates>()
            for (item in lineChartY1.indices) {
                /*筛选y坐标出来,这里由于只能插入一个情况相同的x作为键，有很大可能是有两个相同x的，所以不用hashmap 用一个实体类进行存储以及排序*/
                if (item == 0 || item % 2 == 0) {
                    val coordinates = Coordinates()
                    coordinates.y = lineChartY1[item + 1]
                    coordinates.x = lineChartY2[item + 1]
                    beans.add(coordinates)
                }
            }
            //对有效坐标进行切割
            val coordinatesMap = splitList(beans)
            val beanSetValue1 = coordinatesMap["1"]
            val beanSetValue2 = coordinatesMap["2"]
            val beanSetValue3 = coordinatesMap["3"]
            if (beanSetValue1?.isNotEmpty()!!) {
                val sortedList = beanSetValue1.sortedBy { bean -> bean.x }
                for (item in sortedList) {
                    val y = item.y
                    val x = item.x
                    val entry = Entry()
                    entry.x = x
                    entry.y = y
                    values1.add(entry)
                }
            }
            if (beanSetValue2?.isNotEmpty()!!) {
                val sortedList = beanSetValue2.sortedBy { bean -> bean.x }
                for (item in sortedList) {
                    val y = item.y
                    val x = item.x
                    val entry = Entry()
                    entry.x = x
                    entry.y = y
                    values2.add(entry)
                }
            }
            if (beanSetValue3?.isNotEmpty()!!) {
                val sortedList = beanSetValue3.sortedBy { bean -> bean.x }
                for (item in sortedList) {
                    val x = item.x
                    val y = item.y
                    val entry = Entry()
                    entry.x = x
                    entry.y = y
                    values3.add(entry)
                }
            }
            val valuesDataSet = ArrayList<ArrayList<Entry>>()
            valuesDataSet.add(values1)
            valuesDataSet.add(values2)
            valuesDataSet.add(values3)
            it.onNext(valuesDataSet)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            if (it.isNotEmpty()) {
                var values1 = it[0] //几近过期
                var values2 = it[1] //中间时期
                var values3 = it[2] //最近时期
                //对于原点坐标进行剔除处理
                values1 = filterValues(values1)
                values2 = filterValues(values2)
                values3 = filterValues(values3)
                //不合格坐标进行压缩处理
                val set1 = ScatterDataSet(values3, "DS 1")
                set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                set1.color = ColorTemplate.COLORFUL_COLORS[0]
                set1.setDrawValues(false)
                set1.scatterShapeSize = 15f

                val set2 = ScatterDataSet(values2, "DS 2")
                set2.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                set2.color = Color.BLUE
                set2.setDrawValues(false)

                set2.scatterShapeSize = 25f
                val set3 = ScatterDataSet(values1, "DS 2")
                set3.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                set3.color = Color.YELLOW
                set3.setDrawValues(false)

                set3.scatterShapeSize = 35f

                val dataSets = ArrayList<IScatterDataSet>()
                /*这里数值不能改变*/
                dataSets.add(set3)
                dataSets.add(set2)
                dataSets.add(set1)
                val data = ScatterData(dataSets)
                scatterChart.data = data
                scatterChart.invalidate()
            }

        }, {
            Log.e("Throwable", "${it.message}")
        })
        /*3.初始化坐标系绘制归属线,划分数据*/


        /*4.判断是否需要限制线*/

    }

    private fun filterValues(values: java.util.ArrayList<Entry>) :java.util.ArrayList<Entry>{
        val arrayList = java.util.ArrayList<Entry>();
        for (item in values.indices) {
            if (values[item].x.toInt() != 0 || values[item].y.toInt() != 0) {
                arrayList.add(values[item])
            }
        }
        return arrayList;
    }

    private fun initChartView(lineChart: LineChart, lineData: LineData) {
        lineChart.description.isEnabled = false
        lineChart.isDragEnabled = true
        lineChart.setTouchEnabled(true)
        lineChart.setDrawBorders(false)
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isHighlightPerTapEnabled = false
        lineChart.isHighlightPerDragEnabled = false
        lineChart.setBorderWidth(0f)
        lineChart.setDrawGridBackground(false)
        lineChart.setNoDataText("暂无数据")
        val xAxis: XAxis = lineChart.xAxis
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
//        xAxis.setLabelCount(30, true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        leftAxis.mAxisMinimum = 0f
        leftAxis.mAxisMaximum = 10f
        leftAxis.textColor = Color.WHITE

        val rightAxis: YAxis = lineChart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        lineData.setDrawValues(false)
        xAxis.setDrawGridLines(false)
        lineData.setValueTextColor(Color.WHITE)
        xAxis.isEnabled = false
        leftAxis.isEnabled = true
        leftAxis.gridColor = Color.TRANSPARENT
        leftAxis.zeroLineColor = Color.TRANSPARENT
        leftAxis.setDrawGridLines(true)
        leftAxis.setValueFormatter { value, axis ->
            ""//${value.toInt()}
        }
        rightAxis.isEnabled = false
        xAxis.textColor = Color.WHITE

        //设置限制线 70代表某个该轴某个值，也就是要画到该轴某个值上
        val limitLine = LimitLine(3.3f)
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

        val limitLine2 = LimitLine(6.6f)
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
        //左边Y轴添加限制线
        leftAxis.addLimitLine(limitLine)
        leftAxis.addLimitLine(limitLine2)
        val l = lineChart.legend
        l.isEnabled = false
        // set data
        lineChart.data = lineData
        // do not forget to refresh the chart
        // holder.chart.invalidate();
        lineChart.animateX(750)
    }

    private fun initBarChart(barChart: BarChart): BarDataSet {
        barChart.setTouchEnabled(true)
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.isHighlightPerTapEnabled = false
        barChart.isHighlightPerDragEnabled = false
        barChart.setDrawBorders(false) //显示边界
        barChart.setDrawBarShadow(false) //设置每个直方图阴影为false
        barChart.setDrawValueAboveBar(false) //这里设置为true每一个直方图的值就会显示在直方图的顶部
        barChart.description.isEnabled = false //设置描述文字不显示，默认显示
        barChart.setDrawGridBackground(false) //设置不显示网格
        barChart.   //setBackgroundColor(Color.parseColor("#F3F3F3")) //设置图表的背景颜色
        legend.isEnabled = false //设置不显示比例图
        // if more than 60 entries are displayed in the chart, no values will be
        // drawn

        barChart.isHighlightFullBarEnabled = false
//            scaleX = 1.5f
        //x轴设置
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM//X轴的位置 默认为上面
            setDrawGridLines(false)  //是否绘制X轴上的网格线（背景里面的竖线）
            //axisRight.isEnabled = false//隐藏右侧Y轴   默认是左右两侧都有Y轴
            granularity = 1f // only intervals of 1 day
            mAxisMinimum = 0f
        }
        barChart.xAxis.setLabelCount(3, false)
        barChart.xAxis.isEnabled = false
        barChart.axisLeft.isEnabled = true
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisLeft.gridColor = Color.TRANSPARENT
        barChart.axisLeft.labelCount = 3
        barChart.axisLeft.axisMaxLabels = 3
        barChart.axisLeft.mAxisMaximum = 10f
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.setValueFormatter { value, axis ->
            ""//${value.toInt()}
        }
        barChart.isScaleXEnabled = false                             //支持x轴缩放
        barChart.isScaleYEnabled = false

        // if more than 60 entries are displayed in the chart, no values will be
        //保证Y轴从0开始，不然会上移一点
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.axisMinimum = 0f
        val mBarDataSet = BarDataSet(values, barChart.toString())
        //set1.setColors(*ColorTemplate.VORDIPLOM_COLORS)
        mBarDataSet.setDrawValues(false)
        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(mBarDataSet)
        colors.add(
            ContextCompat.getColor(this, R.color.tran)
        )
        mBarDataSet.colors = colors
        val barData = BarData(dataSets)
        barData.addEntry(BarEntry(0f, 9.9f), 0)
        barChart.data = barData
        return mBarDataSet
//            data.barWidth = 0.3f
//            addBarEntry(0, 100)
    }

    private val colors = ArrayList<Int>()
    private fun getData(data: ArrayList<Float>, isBezier: Boolean): LineData {
        val values = ArrayList<Entry>()
        for (item in data.indices) {
            val entry = Entry()
            if (item == 0 || item % 2 == 0) {
                entry.y = data[item + 1]
                entry.x = data[item]
                values.add(entry)
            }
        }

        val VORDIPLOM_COLORS = intArrayOf(
            Color.rgb(61, 179, 142)
        )
        val lineDataSet = LineDataSet(values, "data set1")
        lineDataSet.lineWidth = 1.0f
        lineDataSet.circleRadius = 0f
        lineDataSet.circleHoleRadius = 0f
        lineDataSet.valueTextColor = Color.WHITE
        lineDataSet.color = Color.parseColor("#3DB38E")
//        lineDataSet.setCircleColor(Color.parseColor("#3DB38E"))
        lineDataSet.circleColors = VORDIPLOM_COLORS.asList()
        lineDataSet.highLightColor = Color.parseColor("#3DB38E")
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawCircles(false)
        lineDataSet.axisDependency = YAxis.AxisDependency.LEFT
//        lineDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        if (isBezier) {
            lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val sets = ArrayList<ILineDataSet>()
//        val d = LineDataSet(values, "")
//        d.lineWidth = 0f
//        d.circleRadius = 0f
//        d.circleHoleRadius = 0f
//        d.valueTextColor = Color.TRANSPARENT
//        d.color = Color.TRANSPARENT
//        d.setCircleColor(Color.TRANSPARENT)
//        d.highLightColor = Color.TRANSPARENT
//        d.setDrawValues(false)
//        d.setDrawCircles(false)
//
//        d.axisDependency = YAxis.AxisDependency.LEFT
//        d.mode = LineDataSet.Mode.CUBIC_BEZIER
//        d.highLightColor = Color.argb(0, 0, 0, 0)
//        //d.setCircleColor(Color.argb(0, 0, 0, 0))
//        d.color = Color.argb(0, 0, 0, 0)
//        d.addEntry(Entry(0f, 99f))
//        sets.add(d)
        lineDataSet.addEntry(Entry(0f, 9.8f))
        sets.add(lineDataSet)
        // create a data object with the data sets
        return LineData(sets)
    }

    private var filterValue = 0
    private fun addBarEntry(barChart: BarChart, value2: Float, mBarDataSet: BarDataSet) {
        if (barChart.barData != null) {
            val entryCount = (barChart.data.getDataSetByIndex(0) as BarDataSet).entryCount
            if (value2 > 0) {
                barChart.data.addEntry(BarEntry(entryCount.toFloat(), value2.toFloat()), 0)
                when {
                    value2 < 3 -> {
                        colors.add(
                            ContextCompat.getColor(this, R.color.color_FDC457)
                        )
                    }
                    value2.toInt() in 3..6 -> {
                        colors.add(
                            ContextCompat.getColor(this, R.color.color_37B48B)
                        )
                    }
                    value2 > 6 -> {
                        colors.add(
                            ContextCompat.getColor(
                                this, R.color.color_text_selected
                            )
                        )
                    }
                }
            } else {
                //延迟移动度
                colors.add(
                    ContextCompat.getColor(this, R.color.color_FDC457)
                )
                barChart.data.addEntry(BarEntry(entryCount.toFloat(), 0f), 0)
            }
            mBarDataSet.colors = colors
        }
        barChart.invalidate()
    }

    private fun initView() {
        /*有点傻，但也就先这样吧*/
        viewBinding.top.tvCoodinare.visibility = View.VISIBLE
        viewBinding.top.tvCoodinare.text = "坐标"
        viewBinding.top.tvCoodinare.background =
            AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
        viewBinding.top.tvDel.visibility = View.VISIBLE
        viewBinding.top.tvDel.text = "数值"
        viewBinding.top.tvDel.background =
            AppCompatResources.getDrawable(baseContext, R.color.color_37B48B)
        viewBinding.top.tvRight.visibility = View.VISIBLE
        viewBinding.top.tvRight.text = "曲线"
        viewBinding.top.tvRight.background =
            AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
        viewBinding.top.tvDel.setOnClickListener {
            viewBinding.top.tvRight.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            viewBinding.top.tvDel.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_37B48B)
            viewBinding.top.tvCoodinare.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewBinding.nsContent.visibility = View.VISIBLE
            viewBinding.mainLayout.visibility = View.GONE
            viewBinding.coodinareLayout.visibility = View.GONE
        }
        viewBinding.top.tvRight.setOnClickListener {
            viewBinding.top.tvRight.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_37B48B)
            viewBinding.top.tvDel.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            viewBinding.top.tvCoodinare.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            requestedOrientation = if (isPad()) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            viewBinding.mainLayout.visibility = View.VISIBLE
            viewBinding.nsContent.visibility = View.GONE
            viewBinding.coodinareLayout.visibility = View.GONE

        }
        viewBinding.top.tvCoodinare.setOnClickListener {
            viewBinding.top.tvRight.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            viewBinding.top.tvDel.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_text_bg_normal)
            viewBinding.top.tvCoodinare.background =
                AppCompatResources.getDrawable(baseContext, R.color.color_37B48B)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewBinding.mainLayout.visibility = View.GONE
            viewBinding.nsContent.visibility = View.GONE
            viewBinding.coodinareLayout.visibility = View.VISIBLE

        }
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.ivExport.visibility = View.VISIBLE
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.ivExport.visibility = View.VISIBLE
        val data: LineData = getData(trainingDTO.lineChartYData, false)
        Log.e("lineData", "${data.entryCount}")
        val data1: LineData = getData(trainingDTO.lineChartYData1, true)
        val data2: LineData = getData(trainingDTO.lineChartYData2, false)
        initChartView(viewBinding.chart, data)
        initBarChart(viewBinding.bar)
        LineChartUtils.setLineChart(viewBinding.chart1, data1, 6, 9)
        viewBinding.chart1.data = data1
        viewBinding.chart1.setVisibleXRangeMaximum(30f)
        viewBinding.chart1.isDragEnabled = true
        viewBinding.chart1.setTouchEnabled(true)
        viewBinding.chart1.setDrawBorders(false)
        viewBinding.chart1.setScaleEnabled(false)
        viewBinding.chart1.setPinchZoom(false)
        viewBinding.chart1.isHighlightPerTapEnabled = false
        viewBinding.chart1.isHighlightPerDragEnabled = false
        initChartView(viewBinding.chart2, data2)
        viewBinding.chart.setVisibleXRangeMaximum(30f)
        viewBinding.chart2.setVisibleXRangeMaximum(30f)
        viewBinding.bar.setVisibleXRangeMaximum(30f)
        /*1气压 2.按压*/
        initScatterChart(
            viewBinding.scatterChart, trainingDTO.lineChartYData1, trainingDTO.lineChartYData2, 2
        )
        initScatterChart(
            viewBinding.scatterChart1,trainingDTO.barChartData, trainingDTO.lineChartYData, 1
        )

    }

    private fun isPad(): Boolean {

        val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay;
        val dm = DisplayMetrics();
        display.getMetrics(dm)
        val x = (dm.widthPixels / dm.xdpi).toDouble().pow(2.0)
        val y = (dm.heightPixels / dm.ydpi).toDouble().pow(2.0)
        val screenInches = sqrt(x + y)
        return screenInches >= 7.0

    }

    private fun exportNoReportChart() {
        val data: LineData = getData(trainingDTO.lineChartYData, false)
        val data1: LineData = getData(trainingDTO.lineChartYData1, true)
        val data2: LineData = getData(trainingDTO.lineChartYData2, false)
        val dataset = initBarChart(viewBinding.layoutExportNoCheck.barChart)
        initChartView(viewBinding.layoutExportNoCheck.lineChart, data)
        LineChartUtils.setLineChart(viewBinding.layoutExportNoCheck.lineChart1, data1, 6, 9)
        viewBinding.layoutExportNoCheck.lineChart1.data = data1
        viewBinding.layoutExportNoCheck.lineChart1.isDragEnabled = true
        viewBinding.layoutExportNoCheck.lineChart1.setTouchEnabled(true)
        viewBinding.layoutExportNoCheck.lineChart1.setDrawBorders(false)
        viewBinding.layoutExportNoCheck.lineChart1.setScaleEnabled(false)
        viewBinding.layoutExportNoCheck.lineChart1.setPinchZoom(false)
        viewBinding.layoutExportNoCheck.lineChart1.isHighlightPerTapEnabled = false
        viewBinding.layoutExportNoCheck.lineChart1.isHighlightPerDragEnabled = false
        initChartView(viewBinding.layoutExportNoCheck.lineChart2, data2)
        for (item in trainingDTO.barChartData.indices) {

            if (item == 0 || item % 2 == 0) {
                addBarEntry(
                    viewBinding.layoutExportNoCheck.barChart,
                    trainingDTO.barChartData[item + 1],
                    dataset
                )
            }

        }
        Log.e("BarData", "${dataset.entryCount}")

    }

    private fun exportReportChart() {
        val data: LineData = getData(trainingDTO.lineChartYData, false)
        val data1: LineData = getData(trainingDTO.lineChartYData1, true)
        val data2: LineData = getData(trainingDTO.lineChartYData2, false)
        val dataset = initBarChart(viewBinding.layoutExport.barChart)
        initChartView(viewBinding.layoutExport.lineChart, data)
        LineChartUtils.setLineChart(viewBinding.layoutExport.lineChart1, data1, 6, 9)
        viewBinding.layoutExport.lineChart1.data = data1
        viewBinding.layoutExport.lineChart1.isDragEnabled = true
        viewBinding.layoutExport.lineChart1.setTouchEnabled(true)
        viewBinding.layoutExport.lineChart1.setDrawBorders(false)
        viewBinding.layoutExport.lineChart1.setScaleEnabled(false)
        viewBinding.layoutExport.lineChart1.setPinchZoom(false)
        viewBinding.layoutExport.lineChart1.isHighlightPerTapEnabled = false
        viewBinding.layoutExport.lineChart1.isHighlightPerDragEnabled = false
        initChartView(viewBinding.layoutExport.lineChart2, data2)
        for (item in trainingDTO.barChartData.indices) {
            addBarEntry(
                viewBinding.layoutExport.barChart, trainingDTO.barChartData[item], dataset
            )
        }
    }


    var trainingDTO = TrainingDTO()
    private fun initPermissions() {
        EasyPermissions.requestPermissions(
            this,
            "获取手机文件读写权限",
            123,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private fun getManager() {
        val alertDialog: AlertDialog //生成一个对话框 可跳转设置里手动开启权限
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this, R.style.DialogStyle) //嫌麻烦，样式可设为null
        builder.setPositiveButton(getString(R.string.authorize_msg), null)
        builder.setTitle(getString(R.string.authorize_title_msg))
        builder.setMessage(getString(R.string.file_manger_msg))
        builder.setCancelable(false)
        alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { v ->
            alertDialog.dismiss() //去获取文件管理
            val intent: Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 0x99)
        }
    }

    private fun initData() {
        trainingDTO = intent.getSerializableExtra(DATADTO) as TrainingDTO
        viewBinding.bottom.ivExport.setOnClickListener {
            val hasStoragePermission = hasStoragePermission()
            if (hasStoragePermission) {
                exPortPDF(trainingDTO.name, trainingDTO.isCheck)
            } else {
                ToastUtils.showShort("暂未授权文件读写")
                if (EasyPermissions.hasPermissions(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//判断当前手机系统版本
                        if (Environment.isExternalStorageManager()) initPermissions()
                        else getManager()
                    } else initPermissions()
                } else {
                    initPermissions()
                }
            }
        }
        val scoreTotal: Float = trainingDTO.getScoreTotal()
        var scoreStar = scoreTotal / 20.0f
        /*-------------------------------start 导出PDF------------------------------*/
        setExportData(scoreStar, scoreTotal, trainingDTO.isCheck)
        /*-------------------------------end 导出PDF------------------------------*/
        //判断模式- false 训练  true 考核
        if (trainingDTO.isCheck) {
            viewBinding.layoutCheck.root.visibility = View.VISIBLE
            viewBinding.groupPr.visibility = View.VISIBLE
            viewBinding.groupQy.visibility = View.VISIBLE
            viewBinding.gruops.visibility = View.GONE
            viewBinding.layoutCheck.tvName.text = "${trainingDTO.name}"
            viewBinding.layoutCheck.tvScale.text = "${trainingDTO.prCount}:${trainingDTO.qyCount}"
            //操作时长倒计时
            viewBinding.layoutCheck.tvCountdown.text = TimeUtils.timeParse(trainingDTO.operateTime)
            //流程分数
            viewBinding.layoutCheck.tvProcess.text =
                "${if (isMulti) 0 else trainingDTO.processScore}分"
            //按压分数
            viewBinding.layoutCheck.tvPress.text = "${trainingDTO.pressScore}分"
            //扣分
            viewBinding.layoutCheck.tvDeduction.text = "${trainingDTO.deduction}分"
            //吹气分数
            viewBinding.layoutCheck.tvBlowNumber.text = "${trainingDTO.blowScore}分"

            /*-----------------------------总结得分项----------------------------------*/
            //按压得分
            viewBinding.layoutCheck.tvPressScore.text =
                "${getNoMoreThanTwoDigits(trainingDTO.getPrScore())}"
            //中断扣分
            viewBinding.layoutCheck.tvInterruptScore.text =
                "${getNoMoreThanTwoDigits(trainingDTO.getTimeOutScore())}"
            //通气得分
            viewBinding.layoutCheck.tvVentilationScore.text =
                "${getNoMoreThanTwoDigits(trainingDTO.getQyScore())}"
            //流程分数
            viewBinding.layoutCheck.tvProcessScore2.text = "${processCheck(trainingDTO)}"
            viewBinding.layoutCheck.ratingBar.visibility = View.INVISIBLE
            viewBinding.layoutCheck.ratingBar2.visibility = View.INVISIBLE
            viewBinding.layoutCheck.ratingBar3.visibility = View.INVISIBLE
            when {
                scoreTotal < 60 -> {
                    viewBinding.layoutCheck.ratingBar.visibility = View.VISIBLE
                }
                scoreTotal in 60.0..80.0 -> {
                    viewBinding.layoutCheck.ratingBar2.visibility = View.VISIBLE
                }
                else -> {
                    viewBinding.layoutCheck.ratingBar3.visibility = View.VISIBLE
                }
            }

            //分数星星配置
            viewBinding.layoutCheck.ratingBar.rating = scoreStar
            viewBinding.layoutCheck.ratingBar2.rating = scoreStar
            viewBinding.layoutCheck.ratingBar3.rating = scoreStar

            //总得分
            viewBinding.layoutCheck.tvScore.text =
                "${if (scoreTotal > 0) getNoMoreThanTwoDigits(scoreTotal) else 0.0}"
            //总分数
            if (trainingDTO.isCheck) {
                trainingDTO.score = getNoMoreThanTwoDigits(scoreTotal).toFloat()
            }

            //按压超次
            viewBinding.tvOutBoutCount.text = "${trainingDTO.prManyCount}次"
            //按压少次
            viewBinding.tvSmallBoutCount.text = "${trainingDTO.prLessCount}次"
            //吹气多次
            viewBinding.tvBlowBoutCount.text = "${trainingDTO.qyManyCount}次"
            //吹气少次
            viewBinding.tvBlowSmallCount.text = "${trainingDTO.qyLessCount}次"
        } else {
            viewBinding.gruops.visibility = View.VISIBLE
            viewBinding.tvTrainName.text = trainingDTO.name
            viewBinding.tvTime.text = TimeUtils.formatDate(trainingDTO.operateTime)
        }

        //循环次数
        viewBinding.tvCycleCount.text = "${trainingDTO.cycleCount}"

        //按压错误数
        viewBinding.tvLungCount.text = "${trainingDTO.pressErrorCount}"
        viewBinding.tvLungCountC.text = "${trainingDTO.pressErrorCount}"

        //按压总数
        viewBinding.tvLungTotal.text = "/${trainingDTO.prSum.toInt()}"
        viewBinding.tvLungTotal1.text = "/${trainingDTO.prSum.toInt()}"
        //按压位置错误
        viewBinding.tvLocation.text = "${trainingDTO.err_pr_posi}"
        //按压不足
        viewBinding.tvInsufficient.text = "${trainingDTO.err_pr_low}"
        //按压过大
        viewBinding.tvPressBig.text = "${trainingDTO.err_pr_high}"
        //按压未回弹
        viewBinding.tvRebound.text = "${trainingDTO.err_pr_unback}"
        //按压超时统计时间
        viewBinding.tvPressTime.text = "${TimeUtils.timeParse(trainingDTO.timeOutTotal)}"
        //平均每分钟按压次数
        viewBinding.tvAverageCount.text = "平均：${trainingDTO.getPressAverageTimes()}次/分"
        //按压频率合格率
        viewBinding.tvClock1.text = "${trainingDTO.getPressRate()}%"
        //回弹合格率
        viewBinding.tvPressPercentage.text = "${trainingDTO.getReboundRate()}%"
        //按压深度合格率
        viewBinding.tvPressEnd.text = "${trainingDTO.getDepthRate()}%"
        //按压平均深度
        viewBinding.tvPressBottom.text = "平均：${trainingDTO.getPressAverageDepth()}mm"
        //整体按压百分比
        viewBinding.tvPressCenter.text = "${trainingDTO.getPressTime()}%"
        //吹气错误数
        viewBinding.tvHeartCount.text = "${trainingDTO.blowErrorCount.toInt()}"
        viewBinding.tvHeartCount2.text = "${trainingDTO.blowErrorCount.toInt()}"
        //吹气总数
        viewBinding.tvHeartTotal.text = "/${trainingDTO.qySum}"
        viewBinding.tvHeartTotal2.text = "/${trainingDTO.qySum}"
        //吹气错误
        viewBinding.tvAirway.text = "${trainingDTO.err_qy_close}"
        //吹气不足
        viewBinding.tvCInsufficient.text = "${trainingDTO.err_qy_low}"
        //吹气过大
        viewBinding.tvBLowBig.text = "${trainingDTO.err_qy_high}"
        //吹气进胃
        viewBinding.tvIntoStomach.text = "${trainingDTO.err_qy_dead}"
        //平均吹气每分钟次数
        viewBinding.tvBlowAverageCount.text = "平均：${trainingDTO.getBlowAverage()}次/分"
        //吹气频率百分比
        viewBinding.tvClock2.text = "${trainingDTO.getBlowRate()}%"
        //通气合格率
        viewBinding.tvBlow.text = "${trainingDTO.getVentilationAmount()}%"
        //吹气平均值
        viewBinding.tvBlowEnd.text = "平均：${trainingDTO.getBlowAverageNumber()}ml"
    }

    private fun setExportData(scoreStar: Float, scoreTotal: Float, check: Boolean) {
        if (!check) {
            exportNoReportChart()
            viewBinding.layoutExportNoCheck.tvScoreSetting.text =
                "分数设定：  " + "流程 ${if (isMulti) 0 else trainingDTO.processScore}分" + "  按压${trainingDTO.pressScore}分" + "  中断${trainingDTO.deduction}分" + "  通气${trainingDTO.blowScore}分"

            viewBinding.layoutExportNoCheck.tvName.text = "学员姓名：   ${trainingDTO.name}"
            viewBinding.layoutExportNoCheck.tvTime.text =
                "操作时间：${TimeUtils.stampToDate(trainingDTO.startTime)}"
            viewBinding.layoutExportNoCheck.tvOperationTime.text =
                "操作时长： ${TimeUtils.timeParse(trainingDTO.operateTime)}"
            viewBinding.layoutExportNoCheck.tvCycle.text = "循环次数：  ${trainingDTO.cycleCount}"
            viewBinding.layoutExportNoCheck.tvModel.text =
                if (trainingDTO.isCheck) "操作模式： 考核" else "操作模式： 训练"
//            viewBinding.layoutExportNoCheck.tvCycleSetting.text =
//                "循环定义：   ${trainingDTO.prCount}:${trainingDTO.qyCount}"

            viewBinding.layoutExportNoCheck.tvCycleCount1.text = "${trainingDTO.cycleCount}"

            viewBinding.layoutExportNoCheck.tvPrCount1.text =
                Html.fromHtml("<b>(<font color=\"#FC7574\">${trainingDTO.pressErrorCount}</font>/<font>${trainingDTO.prSum})</font></b>")

            viewBinding.layoutExportNoCheck.tvQyCount1.text =
                Html.fromHtml("<b>(<font color=\"#FC7574\">${trainingDTO.blowErrorCount.toInt()}</font>/<font>${trainingDTO.qySum})</font></b>")
            //按压位置错误
            viewBinding.layoutExportNoCheck.tvLocation1.text = "位置错误：${trainingDTO.err_pr_posi}次"
            //按压过大
            viewBinding.layoutExportNoCheck.tvPressBig1.text = "按压过大：${trainingDTO.err_pr_high}次"
            //按压不足
            viewBinding.layoutExportNoCheck.tvInsufficient1.text = "按压不足：${trainingDTO.err_pr_low}次"
            //按压未回弹
            viewBinding.layoutExportNoCheck.tvRebound1.text = "回弹不足：${trainingDTO.err_pr_unback}次"
            //按压超时统计时间
            viewBinding.layoutExportNoCheck.tvPressTime.text =
                "${TimeUtils.timeParse(trainingDTO.timeOutTotal)}"
            //平均每分钟按压次数
            viewBinding.layoutExportNoCheck.tvAverageCount1.text =
                "平均按压频率：${trainingDTO.getPressAverageTimes()}次/分"
            //按压频率合格率
            viewBinding.layoutExportNoCheck.tvClock11.text = "${trainingDTO.getPressRate()}%"
            //回弹合格率
            viewBinding.layoutExportNoCheck.tvPressPercentage1.text =
                "${trainingDTO.getReboundRate()}%"
            //按压深度合格率
            viewBinding.layoutExportNoCheck.tvPressEnd1.text = "${trainingDTO.getDepthRate()}%"
            //按压平均深度
            viewBinding.layoutExportNoCheck.tvPressBottom1.text =
                "平均按压深度：${trainingDTO.getPressAverageDepth()}mm"
            //整体按压百分比
            viewBinding.layoutExportNoCheck.tvPressCenter1.text =
                "按压比：${trainingDTO.getPressTime()}%"
            //吹气错误
            viewBinding.layoutExportNoCheck.tvAirway1.text = "气道错误：${trainingDTO.err_qy_close}次"
            //吹气不足
            viewBinding.layoutExportNoCheck.tvCInsufficient1.text =
                "通气不足：${trainingDTO.err_qy_low}次"
            //吹气过大
            viewBinding.layoutExportNoCheck.tvBLowBig1.text = "通气过大：${trainingDTO.err_qy_high}次"
            //吹气进胃
            viewBinding.layoutExportNoCheck.tvIntoStomach1.text = "通气进胃：${trainingDTO.err_qy_dead}次"
            //平均吹气每分钟次数
            viewBinding.layoutExportNoCheck.tvBlowAverageCount1.text =
                "平均通气频率：${trainingDTO.getBlowAverage()}次/分"
            //吹气频率百分比
            viewBinding.layoutExportNoCheck.tvClock21.text = "${trainingDTO.getBlowRate()}%"
            //通气合格率
            viewBinding.layoutExportNoCheck.tvBlow1.text = "${trainingDTO.getVentilationAmount()}%"
            //吹气平均值
            viewBinding.layoutExportNoCheck.tvBlowEnd.text =
                "平均潮气量：${trainingDTO.getBlowAverageNumber()}ml"
        } else {
            exportReportChart()
            viewBinding.layoutExport.tvScoreSetting.text =
                "分数设定：  " + "流程 ${if (isMulti) 0 else trainingDTO.processScore}分" + "  按压${trainingDTO.pressScore}分" + "  中断${trainingDTO.deduction}分" + "  通气${trainingDTO.blowScore}分"

            viewBinding.layoutExport.tvName.text = "学员姓名：   ${trainingDTO.name}"
            viewBinding.layoutExport.tvTime.text =
                "操作时间：${TimeUtils.stampToDate(trainingDTO.startTime)}"
            viewBinding.layoutExport.tvOperationTime.text =
                "操作时长： ${TimeUtils.timeParse(trainingDTO.operateTime)}"
            viewBinding.layoutExport.tvCycle.text = "循环次数：  ${trainingDTO.cycleCount}"
            viewBinding.layoutExport.tvModel.text =
                if (trainingDTO.isCheck) "操作模式： 考核" else "操作模式： 训练"
            viewBinding.layoutExport.tvCycleSetting.text =
                "循环定义： ${trainingDTO.prCount}:${trainingDTO.qyCount}"

            //按压得分
            viewBinding.layoutExport.tvPressScore1.text =
                "按压得分： ${getNoMoreThanTwoDigits(trainingDTO.getPrScore())}"
            //中断扣分
            viewBinding.layoutExport.tvInterruptScore1.text =
                "中断扣分： ${getNoMoreThanTwoDigits(trainingDTO.getTimeOutScore())}"
            //通气得分
            viewBinding.layoutExport.tvVentilationScore1.text =
                "通气得分： ${getNoMoreThanTwoDigits(trainingDTO.getQyScore())}"
            //流程分数
            viewBinding.layoutExport.tvProcessScore2.text = "流程分数： ${processCheck(trainingDTO)}"
            viewBinding.layoutExport.ratingBar1.visibility = View.INVISIBLE
            viewBinding.layoutExport.ratingBar2.visibility = View.INVISIBLE
            viewBinding.layoutExport.ratingBar3.visibility = View.INVISIBLE
            when {
                scoreTotal < 60 -> {
                    viewBinding.layoutExport.ratingBar1.visibility = View.VISIBLE
                }
                scoreTotal in 60.0..80.0 -> {
                    viewBinding.layoutExport.ratingBar2.visibility = View.VISIBLE
                }
                else -> {
                    viewBinding.layoutExport.ratingBar3.visibility = View.VISIBLE
                }
            }

            //分数星星配置
            viewBinding.layoutExport.ratingBar1.rating = scoreStar
            viewBinding.layoutExport.ratingBar2.rating = scoreStar
            viewBinding.layoutExport.ratingBar3.rating = scoreStar
            //总得分
            viewBinding.layoutExport.tvScore.text =
                "${if (scoreTotal > 0) getNoMoreThanTwoDigits(scoreTotal) else 0.0}"

            viewBinding.layoutExport.checkBox1.isChecked = trainingDTO.check1
            viewBinding.layoutExport.checkBox2.isChecked = trainingDTO.check2
            viewBinding.layoutExport.checkBox3.isChecked = trainingDTO.check3
            viewBinding.layoutExport.checkBox4.isChecked = trainingDTO.check4
            viewBinding.layoutExport.checkBox5.isChecked = trainingDTO.check5
            viewBinding.layoutExport.checkBox6.isChecked = trainingDTO.check6
            viewBinding.layoutExport.checkBox7.isChecked = trainingDTO.check7
            viewBinding.layoutExport.checkBox8.isChecked = trainingDTO.check8
            viewBinding.layoutExport.checkBox9.isChecked = trainingDTO.check9
            viewBinding.layoutExport.checkBox10.isChecked = trainingDTO.check10

            viewBinding.layoutExport.tvCycleCount1.text = "${trainingDTO.cycleCount}"

            viewBinding.layoutExport.tvPrCount1.text =
                Html.fromHtml("<b>(<font color=\"#FC7574\">${trainingDTO.pressErrorCount}</font>/<font>${trainingDTO.prSum})</font></b>")

            viewBinding.layoutExport.tvQyCount1.text =
                Html.fromHtml("<b>(<font color=\"#FC7574\">${trainingDTO.blowErrorCount.toInt()}</font>/<font>${trainingDTO.qySum})</font></b>")
            //按压超次
            viewBinding.layoutExport.tvOutBoutCount1.text = "按压超次：${trainingDTO.prManyCount}次"
            //按压少次
            viewBinding.layoutExport.tvSmallBoutCount1.text = "按压少次：${trainingDTO.prLessCount}次"
            //吹气多次
            viewBinding.layoutExport.tvBlowBoutCount1.text = "通气超次：${trainingDTO.qyManyCount}次"
            //吹气少次
            viewBinding.layoutExport.tvBlowSmallCount1.text = "通气少次：${trainingDTO.qyLessCount}次"
            //按压位置错误
            viewBinding.layoutExport.tvLocation1.text = "位置错误：${trainingDTO.err_pr_posi}次"
            //按压过大
            viewBinding.layoutExport.tvPressBig1.text = "按压过大：${trainingDTO.err_pr_high}次"
            //按压不足
            viewBinding.layoutExport.tvInsufficient1.text = "按压不足：${trainingDTO.err_pr_low}次"
            //按压未回弹
            viewBinding.layoutExport.tvRebound1.text = "回弹不足：${trainingDTO.err_pr_unback}次"
            //按压超时统计时间
            viewBinding.layoutExport.tvPressTime.text =
                "${TimeUtils.timeParse(trainingDTO.timeOutTotal)}"
            //平均每分钟按压次数
            viewBinding.layoutExport.tvAverageCount1.text =
                "平均按压频率：${trainingDTO.getPressAverageTimes()}次/分"
            //按压频率合格率
            viewBinding.layoutExport.tvClock11.text = "${trainingDTO.getPressRate()}%"
            //回弹合格率
            viewBinding.layoutExport.tvPressPercentage1.text = "${trainingDTO.getReboundRate()}%"
            //按压深度合格率
            viewBinding.layoutExport.tvPressEnd1.text = "${trainingDTO.getDepthRate()}%"
            //按压平均深度
            viewBinding.layoutExport.tvPressBottom1.text =
                "平均按压深度：${trainingDTO.getPressAverageDepth()}mm"
            //整体按压百分比
            viewBinding.layoutExport.tvPressCenter1.text = "按压比：${trainingDTO.getPressTime()}%"
            //吹气错误
            viewBinding.layoutExport.tvAirway1.text = "气道错误：${trainingDTO.err_qy_close}次"
            //吹气不足
            viewBinding.layoutExport.tvCInsufficient1.text = "通气不足：${trainingDTO.err_qy_low}次"
            //吹气过大
            viewBinding.layoutExport.tvBLowBig1.text = "通气过大：${trainingDTO.err_qy_high}次"
            //吹气进胃
            viewBinding.layoutExport.tvIntoStomach1.text = "通气进胃：${trainingDTO.err_qy_dead}次"
            //平均吹气每分钟次数
            viewBinding.layoutExport.tvBlowAverageCount1.text =
                "平均通气频率：${trainingDTO.getBlowAverage()}次/分"
            //吹气频率百分比
            viewBinding.layoutExport.tvClock21.text = "${trainingDTO.getBlowRate()}%"
            //通气合格率
            viewBinding.layoutExport.tvBlow1.text = "${trainingDTO.getVentilationAmount()}%"
            //吹气平均值
            viewBinding.layoutExport.tvBlowEnd.text =
                "平均潮气量：${trainingDTO.getBlowAverageNumber()}ml"
        }
        /*输出曲线图*/

    }

    /**
     * 对入参保留最多两位小数(舍弃末尾的0)，如:
     * 3.345->3.34
     * 3.40->3.4
     * 3.0->3
     */
    private fun getNoMoreThanTwoDigits(number: Float): String {
        val format = DecimalFormat("0.#")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(number)
    }

    private fun processCheck(trainingDTO: TrainingDTO): Float {
        //该页面禁止点击事件
        viewBinding.layoutCheck.check.checkBox1.isClickable = false
        viewBinding.layoutCheck.check.checkBox2.isClickable = false
        viewBinding.layoutCheck.check.checkBox3.isClickable = false
        viewBinding.layoutCheck.check.checkBox4.isClickable = false
        viewBinding.layoutCheck.check.checkBox5.isClickable = false
        viewBinding.layoutCheck.check.checkBox6.isClickable = false
        viewBinding.layoutCheck.check.checkBox7.isClickable = false
        viewBinding.layoutCheck.check.checkBox8.isClickable = false
        viewBinding.layoutCheck.check.checkBox9.isClickable = false
        viewBinding.layoutCheck.check.checkBox10.isClickable = false

        viewBinding.layoutCheck.check.checkBox1.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox2.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox3.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox4.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox5.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox6.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox7.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox8.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox9.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox10.setOnCheckedChangeListener(
            onCheckedChangeListener
        )

        viewBinding.layoutCheck.check.checkBox1.isChecked = trainingDTO.check1
        viewBinding.layoutCheck.check.checkBox2.isChecked = trainingDTO.check2
        viewBinding.layoutCheck.check.checkBox3.isChecked = trainingDTO.check3
        viewBinding.layoutCheck.check.checkBox4.isChecked = trainingDTO.check4
        viewBinding.layoutCheck.check.checkBox5.isChecked = trainingDTO.check5
        viewBinding.layoutCheck.check.checkBox6.isChecked = trainingDTO.check6
        viewBinding.layoutCheck.check.checkBox7.isChecked = trainingDTO.check7
        viewBinding.layoutCheck.check.checkBox8.isChecked = trainingDTO.check8
        viewBinding.layoutCheck.check.checkBox9.isChecked = trainingDTO.check9
        viewBinding.layoutCheck.check.checkBox10.isChecked = trainingDTO.check10

        if (trainingDTO.processScore > 0) {
            return ((trainingDTO.processScore / 10) * listCheck.size)
        }
        return 0f
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size)
        //ToastUtils.showShort("用户授权成功")
        if (trainingDTO != null) {
            exPortPDF(trainingDTO.name, trainingDTO.isCheck)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size)
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).setTitle("提示").setRationale("是否前往设置中，开启文件读写权限！").build()
                .show()
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    override fun onRationaleDenied(requestCode: Int) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            val yes = "文件写入已授权"
            val no = "文件写入未授权"
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(
                this, if (hasStoragePermission()) yes else no, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        /*先在xml布局中设置InVisisble,当获取完宽高信息后再进行Gone操作*/
        measureHeight = if (trainingDTO.isCheck) {
            viewBinding.layoutExport.clExportContent.measuredHeight
        } else {
            viewBinding.layoutExportNoCheck.clExportContent.measuredHeight
        }
        measureWidth = if (trainingDTO.isCheck) {
            viewBinding.layoutExport.clExportContent.measuredWidth
        } else {
            viewBinding.layoutExportNoCheck.clExportContent.measuredWidth
        }
        viewBinding.layoutExportNoCheck.root.visibility = View.GONE
        viewBinding.layoutExport.root.visibility = View.GONE
    }

    private var measureHeight = 0
    private var measureWidth = 0

    /**
     * 导出当前页为PDF
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun exPortPDF(fileName: String?, isCheck: Boolean) {
        showLoadingDialog()
        /**/
        val path =
            Environment.getExternalStorageDirectory().path + File.separator + "${fileName + "_" + System.currentTimeMillis()}.pdf"
        //创建pdf文本
        val pdfDocument = PdfDocument()
        //分页
        val pageInfo = PdfDocument.PageInfo.Builder(
            measureWidth, measureHeight, 1
        ).create()
        val page2 = pdfDocument.startPage(pageInfo)
        val canvas = page2.canvas
//        canvas.scale(1.1f, 1.1f);
        if (isCheck) {
            viewBinding.layoutExport.clExportContent.layoutParams.width = 1180
            viewBinding.layoutExport.clExportContent.layoutParams.height = 2120
            viewBinding.layoutExport.clExportContent.draw(canvas)

        } else {
            viewBinding.layoutExportNoCheck.clExportContent.layoutParams.width = 1180
            viewBinding.layoutExportNoCheck.clExportContent.layoutParams.height = 2120
            viewBinding.layoutExportNoCheck.clExportContent.draw(canvas)

        }
        pdfDocument.finishPage(page2)
        GlobalScope.launch(Dispatchers.IO) {
            //保存文件路径
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                pdfDocument.writeTo(FileOutputStream(file))
            } catch (e: Exception) {
                ToastUtils.showShort("成绩已导出异常")
                Log.e(TAG, e.message + "")
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                pdfDocument.close()
                ToastUtils.showShort("PDF成绩文件存放位置：${path}")
                hideLoadingDialog()
                if (isCheck) {
                    viewBinding.layoutExport.clExportContent.visibility = View.GONE
                } else {
                    viewBinding.layoutExportNoCheck.clExportContent.visibility = View.GONE
                }
            }
        }
    }

    private val listCheck = mutableListOf<Int>()
    private val onCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                listCheck.add(buttonView.id)
            }
        }
}