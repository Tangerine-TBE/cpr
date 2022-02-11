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

        //曲线图表
        val data: LineData = getData(8)
        val data1: LineData = getData(180)
        val data2: LineData = getData(0)

        //柱状图
//        val barCharts = BarCharts()
//        val barData = barCharts.getBarData(list)
//        barCharts.showBarChart(viewBinding.barChart, barData, true)
        // add some transparency to the color with "& 0x90FFFFFF"
        LineChartUtils.setLineChart(viewBinding.lineChart, data, 8)
        LineChartUtils.setLineChart(viewBinding.lineChart1, data1, 180)
        LineChartUtils.setLineChart(viewBinding.lineChart2, data2, 0)
        StatusLiveData.data.observe(requireActivity(), {
            setData(it)
            addEntry(data, viewBinding.lineChart, it.cf.toFloat())
            addEntry(data1, viewBinding.lineChart1, it.distance.toFloat())
            addEntry(data2, viewBinding.lineChart2, it.pf.toFloat())
            addBarEntry(it.bpValue)
            //吹气错误数统计
            viewBinding.tvLungCount.text =
                "${(it.ERR_QY_CLOSE + it.ERR_QY_HIGH + it.ERR_QY_LOW + it.ERR_QY_DEAD)}"
            //按压错误数统计
            viewBinding.tvHeartCount.text =
                "${(it.ERR_PR_POSI + it.ERR_PR_LOW + it.ERR_PR_HIGH)}"
            //按压总数
            viewBinding.tvLungTotal.text = "/${it.qySum}"
            viewBinding.tvHeartTotal.text = "/${it.prSum}"
        })

        initBarChart()
        viewBinding.constraintlayout2.setOnClickListener {
            addBarEntry(Random().nextInt(800))
        }

        viewBinding.constraintlayout.setOnClickListener {
            addEntry(data, viewBinding.lineChart, 0f)
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
            setTouchEnabled(true)
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
//            data.barWidth = 0.3f
            // if more than 60 entries are displayed in the chart, no values will be
            // drawny
//            setMaxVisibleValueCount(6)
            //保证Y轴从0开始，不然会上移一点
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
            var set1: BarDataSet? = null
            set1 = BarDataSet(values, "Data Set")
            set1.setColors(*ColorTemplate.VORDIPLOM_COLORS)
            set1.setDrawValues(false)
            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(set1)
            val barData = BarData(dataSets)
            data = barData
            data.barWidth = 0.3f
        }
    }

    private val values = ArrayList<BarEntry>()

    //这里要进行图像绘制，所以要切回UI线程，否则会报错
    private fun addBarEntry(value: Int) {
        viewBinding.barChart.apply {
            if (barData != null) {
//                barData.addEntry(BarEntry(value.toFloat(), 0f), 0)
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
//        values.add(Entry(0f, value.toFloat()))
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
        //通知数据已经改变
        lineData.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        //把yValues移到指定索引的位置
        lineChart.moveViewToAnimated(entryCount - 4f, yValues, YAxis.AxisDependency.LEFT, 1000)
        lineChart.setVisibleXRangeMaximum(10f)
//        lineChart.moveViewToX((lineData.entryCount - 4).toFloat())/**/
        lineChart.moveViewToX((lineData.entryCount - 10).toFloat())
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