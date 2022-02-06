package com.pr.perfectrecovery.fragment

import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IValueFormatter
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.ChartFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.ChartViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ScoringConfigBean
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.w3c.dom.Entity

/**
 * 曲线
 */
class ChartFragment : Fragment() {
    private lateinit var viewBinding: ChartFragmentBinding
    private lateinit var configBean: ScoringConfigBean

    companion object {
        fun newInstance() = ChartFragment()
    }

    private lateinit var viewModel: ChartViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = ChartFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        EventBus.getDefault().register(this)
        viewModel = ViewModelProvider(this).get(ChartViewModel::class.java)
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ScoringConfigBean::class.java)
        initView()
    }

    private fun initView() {

        viewBinding.tvLungTotal.text = "/${configBean.tidalFrequencyEnd}"
        viewBinding.tvHeartTotal.text = "/${configBean.depthFrequencyEnd}"
        //曲线图表
        val data: LineData = getData(8)
        val data1: LineData = getData(180)
        val data2: LineData = getData(120)

        //柱状图
        val barCharts = BarCharts()
        val list = ArrayList<Int>()
        list.add(0)
//        val barData = barCharts.getBarData(list)
//        barCharts.showBarChart(viewBinding.barChart, barData, true)
        // add some transparency to the color with "& 0x90FFFFFF"
        LineChartUtils.setLineChart(viewBinding.lineChart, data, 8)
        LineChartUtils.setLineChart(viewBinding.lineChart1, data1, 180)
        LineChartUtils.setLineChart(viewBinding.lineChart2, data2, 120)
        StatusLiveData.data.observe(requireActivity(), {
            setData(it)
            //数据清空
            val entryCount = (data1.getDataSetByIndex(0) as LineDataSet).entryCount
            if (entryCount == 30) {
                (data.getDataSetByIndex(0) as LineDataSet).entries.clear()
                (data1.getDataSetByIndex(0) as LineDataSet).entries.clear()
                (data2.getDataSetByIndex(0) as LineDataSet).entries.clear()
                //lineData.clearValues()
                //通知数据已经改变
                data.notifyDataChanged()
                data.notifyDataChanged()
                data.notifyDataChanged()

                viewBinding.lineChart.notifyDataSetChanged()
                viewBinding.lineChart.invalidate()
                viewBinding.lineChart1.notifyDataSetChanged()
                viewBinding.lineChart1.invalidate()
                viewBinding.lineChart2.notifyDataSetChanged()
                viewBinding.lineChart2.invalidate()
            }

            addEntry(data, viewBinding.lineChart, it.cf.toFloat())
            addEntry(data1, viewBinding.lineChart1, it.distance.toFloat())
            addEntry(data2, viewBinding.lineChart2, it.pf.toFloat())

//            val entryCount2 = (barData.getDataSetByIndex(0) as BarDataSet).entryCount
//            if(entryCount2 == 10){
//                (barData.getDataSetByIndex(0) as BarDataSet).clear()
//            }
            addBarEntry(it.bpValue)
        })

        initBarChart()
        viewBinding.constraintlayout2.setOnClickListener {
            addBarEntry(Random().nextInt(800))
        }
    }

    private fun initBarChart() {
        viewBinding.barChart.apply {
            setDrawBorders(false) //显示边界
            setDrawBarShadow(false) //设置每个直方图阴影为false
            setDrawValueAboveBar(true) //这里设置为true每一个直方图的值就会显示在直方图的顶部
            description.isEnabled = false //设置描述文字不显示，默认显示
            setDrawGridBackground(false) //设置不显示网格
            //setBackgroundColor(Color.parseColor("#F3F3F3")) //设置图表的背景颜色
            legend.isEnabled = false //设置不显示比例图
            setScaleEnabled(true) //设置是否可以缩放
            //x轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM//X轴的位置 默认为上面
                setDrawGridLines(false)  //是否绘制X轴上的网格线（背景里面的竖线）
                //axisRight.isEnabled = false//隐藏右侧Y轴   默认是左右两侧都有Y轴
                granularity = 1f // only intervals of 1 day
                labelCount = 100
                /*valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                      //TODO 自定义X轴label格式
                    }
                }*/
            }

            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            // if more than 60 entries are displayed in the chart, no values will be
            // drawny
//            setMaxVisibleValueCount(6)
            //保证Y轴从0开始，不然会上移一点
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
        }
    }

    private val values = ArrayList<BarEntry>()

    //这里要进行图像绘制，所以要切回UI线程，否则会报错
    private fun addBarEntry(value: Int) {
        //第一次查询要添加一个空的BarDataSet
        var set1: BarDataSet? = null
        if (viewBinding.barChart.barData == null) {
            values.add(BarEntry(790f, 0f))
            set1 = BarDataSet(values, "Data Set")
            set1.setColors(*ColorTemplate.VORDIPLOM_COLORS)
            set1.setDrawValues(false)

            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(set1)

            val data = BarData(dataSets)
            viewBinding.barChart.data = data
//            viewBinding.barChart.setFitBars(true)
            data.barWidth = 0.3f
        }
        viewBinding.barChart.apply {
            //barData.addEntry(BarEntry(value.toFloat(), 0f), 0)
            //通知数据已经改变
            //lineData.notifyDataChanged()
//            set1 = data.getDataSetByIndex(0) as BarDataSet
//            when {
//                value in 401..600 -> {
//                    set1!!.color = ContextCompat.getColor(requireContext(), R.color.color_37B48B)
//                }
//                value < 400 -> {
//                    set1!!.color = ContextCompat.getColor(requireContext(), R.color.color_FDC457)
//                }
//                value > 600 -> {
//                    set1!!.color =
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.color_text_selected
//                        )
//                }
//            }
            val entryCount = (data.getDataSetByIndex(0) as BarDataSet).entryCount
            data.addEntry(BarEntry(entryCount.toFloat(), value.toFloat()), 0)
            data.notifyDataChanged()
            notifyDataSetChanged()
            //设置在图表中显示的最大X轴数量
            setVisibleXRangeMaximum(30f)
            //这里用29是因为30的话，最后一条柱子只显示了一半
            moveViewToX(barData.entryCount.toFloat() - 29)
//            moveViewToAnimated(entryCount - 4f, value.toFloat(), YAxis.AxisDependency.RIGHT, 1000)
        }
        viewBinding.barChart.invalidate()
    }

    private fun setData(data: BaseDataDTO) {
        viewBinding.tvLungTotal.text = "/150"
        viewBinding.tvLungCount.text = "${data.qySum}"
        viewBinding.tvHeartTotal.text = "/10"
        viewBinding.tvHeartCount.text = "${data.prSum}"
    }

    private fun getData(value: Int): LineData {
        val values = ArrayList<Entry>()
        values.add(Entry(0f, value.toFloat()))
        // create a dataset and give it a type
        val lineDataSet = LineDataSet(values, "DataSet 1")
        lineDataSet.lineWidth = 1.75f
        lineDataSet.circleRadius = 5f
        lineDataSet.circleHoleRadius = 2.5f
        lineDataSet.valueTextColor = Color.WHITE
        lineDataSet.color = Color.parseColor("#3DB38E")
        lineDataSet.setCircleColor(Color.parseColor("#3DB38E"))
        lineDataSet.highLightColor = Color.parseColor("#3DB38E")
        lineDataSet.setDrawValues(true)
        // create a data object with the data sets
        return LineData(lineDataSet)
    }

    private fun setLineChart(chart: LineChart, data: LineData, isSort: Boolean) {
        (data.getDataSetByIndex(0) as LineDataSet).circleHoleColor = Color.TRANSPARENT
        (data.getDataSetByIndex(0) as LineDataSet).setDrawCircles(false)
        (data.getDataSetByIndex(0) as LineDataSet).valueFormatter =
            IValueFormatter { value, entry, dataSetIndex, viewPortHandler ->
                //val df = DecimalFormat(".00")
                //                return df.format(value) + "%";
                "${value.toInt()}"
            }

        // no description text
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        //设置规模Y启用
        chart.isScaleYEnabled = false
        //设置规模X启用
        chart.isScaleXEnabled = false
//        chart.axisLeft.axisMinimum = 0f
//        chart.axisRight.axisMinimum = 0f
        // enable touch gestures
        chart.setTouchEnabled(false)
        // enable scaling and dragging
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)
        chart.setBackgroundColor(Color.TRANSPARENT)
        // set custom chart offsets (automatic offset calculation is hereby disabled)
        chart.setViewPortOffsets(10f, 0f, 10f, 0f)
        // add data
        chart.data = data
        // get the legend (only possible after setting data)
        val l = chart.legend
        l.isEnabled = false
        chart.xAxis.textColor = Color.WHITE
        chart.axisLeft.isEnabled = false
        chart.axisLeft.spaceTop = 10f
        chart.axisLeft.spaceBottom = 10f
        chart.axisRight.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.axisLeft.spaceBottom = 0f

        //是否缩放X轴
        chart.isScaleXEnabled = true
        //X轴缩放比例
//        chart.scaleX = 1f
        // 图表左边的y坐标轴线
        val leftAxis: YAxis = chart.axisLeft
        chart.setVisibleXRangeMaximum(30f)
        leftAxis.textColor = Color.WHITE
        if (isSort) {
            // 最小值
            leftAxis.mAxisMinimum = 180f
            // 最大值
            leftAxis.mAxisMaximum = 180f
        } else {
            // 最小值
            leftAxis.mAxisMinimum = 120f
            // 最大值
            leftAxis.mAxisMaximum = 120f
        }
        leftAxis.setDrawGridLines(false)
        val xAxis: XAxis = chart.xAxis
//        xAxis.setLabelCount(30, false) // 设置X轴的刻度数量，第二个参数表示是否平均分配
//        xAxis.granularity = 30f
//        xAxis.labelCount = 30
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        // animate calls invalidate()...
        chart.animateX(1500)
    }

    /**
     * 动态添加数据
     * 在一个LineChart中存放的折线，其实是以索引从0开始编号的
     *
     * @param yValues y值
     */
    private var x = 20
    private fun addEntry(lineData: LineData, lineChart: LineChart, yValues: Float) {
        val entryCount = (lineData.getDataSetByIndex(0) as LineDataSet).entryCount
        val entry = Entry(
            entryCount.toFloat(), yValues
        )
        // 创建一个点
        lineData.addEntry(entry, 0) // 将entry添加到指定索引处的折线中
        lineChart.data = lineData
        //通知数据已经改变
        lineData.notifyDataChanged()
        lineChart.notifyDataSetChanged()

        //把yValues移到指定索引的位置
        lineChart.moveViewToAnimated(entryCount - 4f, yValues, YAxis.AxisDependency.LEFT, 1000)
//        lineChart.moveViewToX((lineData.entryCount - 4).toFloat())/**/
        lineChart.invalidate()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        if (event.code == BaseConstant.EVENT_SINGLE_CHART_START) {
            //启动刷新界面数据
            viewBinding.ivLung.setImageDrawable(resources.getDrawable(R.mipmap.icon_wm_chart_lung_red))
            viewBinding.ivHeart.setImageDrawable(resources.getDrawable(R.mipmap.icon_wm_chart_heart_red))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

}