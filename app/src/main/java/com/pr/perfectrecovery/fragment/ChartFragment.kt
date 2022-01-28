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

    private var count = 0f
    private fun initView() {
        //曲线图表
        val data: LineData = getData()
        // add some transparency to the color with "& 0x90FFFFFF"
        setupChart(viewBinding.chart1, data)

        StatusLiveData.data.observe(requireActivity(), {
            addEntry(data, viewBinding.chart1, it.distance.toFloat())
            setData(it)
        })

        initBarChart()
//        val barCharts = BarCharts()
//        val list = ArrayList<Int>()
//        list.add(790)
//        val barData = barCharts.getBarData(list)
//        barCharts.showBarChart(viewBinding.barChart, barData, false)
        viewBinding.constraintlayout2.setOnClickListener {
            addEntry(450, BarEntry(450f, 0f))
        }
    }

    private fun initBarChart() {
        viewBinding.barChart.apply {
            setDrawBorders(true) //显示边界
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
                setDrawGridLines(false);  //是否绘制X轴上的网格线（背景里面的竖线）
                //axisRight.isEnabled = false//隐藏右侧Y轴   默认是左右两侧都有Y轴
                granularity = 1f
                labelCount = 100
                /*valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                      //TODO 自定义X轴label格式
                    }
                }*/
            }
            //保证Y轴从0开始，不然会上移一点
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
        }
    }

    //这里要进行图像绘制，所以要切回UI线程，否则会报错
    private fun addEntry(value: Int, entry: BarEntry) {
        //第一次查询要添加一个空的BarDataSet
        if (viewBinding.barChart.barData == null) {
            viewBinding.barChart.data =
                BarData(BarDataSet(mutableListOf<BarEntry>(), "测温点").apply {
                    // 柱子的颜色
                    when {
                        value in 401..599 -> {
                            color = ContextCompat.getColor(requireContext(), R.color.color_37B48B)
                        }
                        value < 400 -> {
                            color = ContextCompat.getColor(requireContext(), R.color.color_FDC457)
                        }
                        value > 600 -> {
                            color =
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.color_text_selected
                                )
                        }
                    }
                    // 设置点击某个柱子时，柱子的颜色
//                    highLightColor = ContextCompat.getColor(mContext, R.color.xiancaizi)
                    //barDataSet.setHighlightEnabled(false);//选中柱子是否高亮显示  默认为true
                    viewBinding.barChart.invalidate()
                })
        }
        viewBinding.barChart.apply {
            barData.addEntry(entry, 0)
            //通知数据已经改变
            //lineData.notifyDataChanged()
            notifyDataSetChanged()
            //设置在图表中显示的最大X轴数量
            setVisibleXRangeMaximum(30f)
            //当图表中显示的X轴数量超过30时，就开始向右移动
            // moveViewToX(barData.entryCount.toFloat() - 30)
            //这里用29是因为30的话，最后一条柱子只显示了一半
            moveViewToX(barData.entryCount.toFloat() - 29)
            invalidate()
        }
    }

    private fun setData(data: BaseDataDTO) {
        viewBinding.tvLungTotal.text = "/150"
        viewBinding.tvLungCount.text = "${data.qySum}"
        viewBinding.tvHeartTotal.text = "/10"
        viewBinding.tvHeartCount.text = "${data.prSum}"
    }

    private fun getData(): LineData {
        val values = ArrayList<Entry>()
        values.add(Entry(0f, 180f))
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

    private fun setupChart(chart: LineChart, data: LineData) {
        (data.getDataSetByIndex(0) as LineDataSet).circleHoleColor = Color.TRANSPARENT
        (data.getDataSetByIndex(0) as LineDataSet).setDrawCircles(false)
        (data.getDataSetByIndex(0) as LineDataSet).valueFormatter =
            IValueFormatter { value, entry, dataSetIndex, viewPortHandler ->
                //val df = DecimalFormat(".00")
                //                return df.format(value) + "%";
                value.toString() + ""
            }

        // no description text
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
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
        //是否缩放X轴
        chart.isScaleXEnabled = true
        //X轴缩放比例
//        chart.scaleX = 1f
        // 图表左边的y坐标轴线
        val leftAxis: YAxis = chart.axisLeft
        chart.setVisibleXRangeMaximum(20f)
        leftAxis.textColor = Color.WHITE
        // 最大值
        leftAxis.mAxisMaximum = 180f
        // 最小值
        leftAxis.mAxisMinimum = 180f
        leftAxis.setDrawGridLines(true)
        val xAxis: XAxis = chart.xAxis
//        xAxis.setLabelCount(20, false) // 设置X轴的刻度数量，第二个参数表示是否平均分配
        xAxis.granularity = 10f
        xAxis.granularity = 1f
//        xAxis.labelCount = subjects.size
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
        lineChart.moveViewToAnimated(entryCount - 4f, yValues, YAxis.AxisDependency.LEFT, 1000);
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