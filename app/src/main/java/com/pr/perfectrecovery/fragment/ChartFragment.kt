package com.pr.perfectrecovery.fragment

import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.ChartFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.ChartViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * 曲线
 */
class ChartFragment : Fragment() {
    private lateinit var viewBinding: ChartFragmentBinding
    private lateinit var configBean: ConfigBean

    companion object {
        fun newInstance() = ChartFragment()
    }

    private lateinit var viewModel: ChartViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = ChartFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        EventBus.getDefault().register(this)
        viewModel = ViewModelProvider(this).get(ChartViewModel::class.java)
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        initView()
    }

    //吹气量统计
    private var qyValue = 0

    private fun initView() {
        //曲线图表
        val data: LineData = getData(0f)
        val data1: LineData = getData(DataVolatile.preDistance.toFloat())
        val data2: LineData = getData(0f)
        // add some transparency to the color with "& 0x90FFFFFF"
        initLineChart(viewBinding.lineChart, data)
        LineChartUtils.setLineChart(viewBinding.lineChart1, data1)
        initLineChart(viewBinding.lineChart2, data2)
        StatusLiveData.data.observe(requireActivity()) {
            setData(it)
            addEntry(data, viewBinding.lineChart, it.cf.toFloat())
            addEntry(data1, viewBinding.lineChart1, it.distance.toFloat())
            addEntry(data2, viewBinding.lineChart2, it.pf.toFloat())
            if (qyValue != it.qySum) {
                qyValue = it.qySum
                val qyMax = DataVolatile.max(DataVolatile.QY_valueSet, true)
                addBarEntry(DataVolatile.qyValue(DataVolatile.QY_valueSet2), qyMax)
            } else {
                addBarEntry(0, 0)
            }
            //吹气错误数统计
            viewBinding.tvLungCount.text =
                "${(it.ERR_QY_CLOSE + it.ERR_QY_HIGH + it.ERR_QY_LOW + it.ERR_QY_DEAD)}"
            //按压错误数统计
            viewBinding.tvHeartCount.text =
                "${(it.ERR_PR_POSI + it.ERR_PR_LOW + it.ERR_PR_HIGH)}"
            //按压总数
            viewBinding.tvLungTotal.text = "/${it.qySum}"
            viewBinding.tvHeartTotal.text = "/${it.prSum}"
        }

        initBarChart()
        viewBinding.constraintlayout2.setOnClickListener {
            addBarEntry(Random().nextInt(800), 30)
        }

        viewBinding.constraintlayout.setOnClickListener {
            addEntry(data, viewBinding.lineChart, 0f)
        }

        viewBinding.constraintlayout3.setOnClickListener {
            val random = (1..100).random()
            addEntry(data, viewBinding.lineChart1, setValue(random).toFloat())
        }
        setViewData()
    }

    private fun setValue(value: Int): Int {
        val depth = DataVolatile.preDistance - value
        if (depth > 0 && depth > DataVolatile.preDistance - 5) {
            return 0
        } else if (depth > DataVolatile.PR_HIGH_VALUE) {
            return 7
        } else if (depth in DataVolatile.PR_LOW_VALUE..DataVolatile.PR_HIGH_VALUE) {
            return 5
        } else if (depth < DataVolatile.PR_LOW_VALUE - 5) {
            return 4
        } else if (depth < DataVolatile.PR_LOW_VALUE - 10) {
            return 3
        } else if (depth < DataVolatile.PR_LOW_VALUE - 15) {
            return 2
        } else if (depth < DataVolatile.PR_LOW_VALUE - 20) {
            return 1
        }
        return 0
    }

    private fun setViewData() {
        viewBinding.tvDepth.text = "${configBean.depth}cm"
        viewBinding.tvDepthEnd.text = "${configBean.depthEnd}cm"
        viewBinding.tvDepthFrequency.text = "${configBean.depthFrequency}cpm"
        viewBinding.tvDepthFrequencyEnd.text = "${configBean.depthFrequencyEnd}cpm"
        viewBinding.tvTidalFrequency.text = "${configBean.tidalFrequency}vpm"
        viewBinding.tvTidalFrequencyEnd.text = "${configBean.tidalFrequencyEnd}vpm"
        viewBinding.tvTidalVolume.text = "${configBean.tidalVolume}ml"
        viewBinding.tvTidalVolumeEnd.text = "${configBean.tidalVolumeEnd}ml"
    }

    private fun initLineChart(lineChart: LineChart, lineData: LineData) {
        // apply styling
        // holder.chart.setValueTypeface(mTf);
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setDrawGridBackground(false)
        lineChart.setNoDataText("no data")
        val xAxis: XAxis = lineChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
//        xAxis.setLabelCount(30, true)
        xAxis.position = XAxisPosition.BOTTOM

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.setLabelCount(5, false)
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        val rightAxis: YAxis = lineChart.axisRight
        rightAxis.setLabelCount(5, false)
        rightAxis.setDrawGridLines(false)
        rightAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        xAxis.isEnabled = false
        leftAxis.isEnabled = false
        rightAxis.isEnabled = false
        xAxis.textColor = Color.WHITE

        val l = lineChart.legend
        l.isEnabled = false

        // set data
        lineChart.data = lineData

        // do not forget to refresh the chart
        // holder.chart.invalidate();
        lineChart.animateX(750)
    }

    var mBarDataSet: BarDataSet? = null
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
            setTouchEnabled(false)
//            scaleX = 1.5f
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
            xAxis.setLabelCount(30, false)
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            setScaleMinima(1.5f, 1.0f)           //x轴默认放大1.2倍 要不然x轴数据展示不全
            isScaleXEnabled = true                             //支持x轴缩放
            isScaleYEnabled = false

            // if more than 60 entries are displayed in the chart, no values will be
            //保证Y轴从0开始，不然会上移一点
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
            mBarDataSet = BarDataSet(values, "Data Set")
            //set1.setColors(*ColorTemplate.VORDIPLOM_COLORS)
            mBarDataSet!!.setDrawValues(false)
            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(mBarDataSet!!)
            val barData = BarData(dataSets)
            data = barData
//            data.barWidth = 0.3f
        }
    }

    private val values = ArrayList<BarEntry>()
    private val colors = ArrayList<Int>()

    //这里要进行图像绘制，所以要切回UI线程，否则会报错
    private fun addBarEntry(value: Int, value2: Int) {
        Log.e("addBarEntry", "$value")
        viewBinding.barChart.apply {
            if (barData != null) {
//                barData.addEntry(BarEntry(value.toFloat(), 0f), 0)
                val entryCount = (data.getDataSetByIndex(0) as BarDataSet).entryCount
                data.addEntry(BarEntry(entryCount.toFloat(), value.toFloat()), 0)
                data.notifyDataChanged()
                when {
                    value2 in configBean.qyLow()..configBean.qyHigh() -> {
                        colors.add(
                            ContextCompat.getColor(requireContext(), R.color.color_37B48B)
                        )
                    }
                    value2 < configBean.qyLow() -> {
                        colors.add(
                            ContextCompat.getColor(requireContext(), R.color.color_FDC457)
                        )
                    }
                    value2 > configBean.qy_max -> {
                        colors.add(
                            ContextCompat.getColor(requireContext(), R.color.color_text_selected)
                        )
                    }
                }
                //给一个默认值
                if (colors.isEmpty()) {
                    colors.add(
                        ContextCompat.getColor(requireContext(), R.color.color_37B48B)
                    )
                }
                mBarDataSet!!.colors = colors
                notifyDataSetChanged()
                //设置在图表中显示的最大X轴数量
                setVisibleXRangeMaximum(30f)
                //这里用29是因为30的话，最后一条柱子只显示了一半
                moveViewToX(barData.entryCount.toFloat() - 29)
                //            moveViewToAnimated(entryCount - 4f, value.toFloat(), YAxis.AxisDependency.RIGHT, 1000)
//                val mMatrix = Matrix()
//                mMatrix.postScale(1.5f, 1f)
//                viewPortHandler.refresh(mMatrix, this, false)
//                animateY(1000)
            }
        }
        viewBinding.barChart.invalidate()
    }

    private fun setData(data: BaseDataDTO) {
        viewBinding.tvLungTotal.text = "/${configBean.prCount * configBean.cycles}"
        viewBinding.tvLungCount.text = "${data.qySum}"
        viewBinding.tvHeartTotal.text = "/${configBean.qyCount * configBean.cycles}"
        viewBinding.tvHeartCount.text = "${data.prSum}"
    }

    private fun getData(value: Float): LineData {
        val values = ArrayList<Entry>()
//        values.add(Entry(0f, value.toFloat()))
        // create a dataset and give it a type
        val lineDataSet = LineDataSet(values, "DataSet 1")
        lineDataSet.lineWidth = 1.2f
        lineDataSet.circleRadius = 0f
        lineDataSet.circleHoleRadius = 0f
        lineDataSet.valueTextColor = Color.WHITE
        lineDataSet.color = Color.parseColor("#3DB38E")
        lineDataSet.setCircleColor(Color.parseColor("#3DB38E"))
        lineDataSet.highLightColor = Color.parseColor("#3DB38E")
        lineDataSet.setDrawValues(true)
        lineDataSet.setDrawCircles(false)
        lineDataSet.axisDependency = YAxis.AxisDependency.LEFT
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val sets = ArrayList<ILineDataSet>()
        val d = LineDataSet(values, "")
        d.lineWidth = 0f
        d.circleRadius = 0f
        d.circleHoleRadius = 0f
        d.valueTextColor = Color.TRANSPARENT
        d.color = Color.TRANSPARENT
        d.setCircleColor(Color.TRANSPARENT)
        d.highLightColor = Color.TRANSPARENT
        d.setDrawValues(false)
        d.setDrawCircles(false)
        d.axisDependency = YAxis.AxisDependency.LEFT
        d.mode = LineDataSet.Mode.CUBIC_BEZIER
        d.highLightColor = Color.argb(0, 0, 0, 0)
        d.setCircleColor(Color.argb(0, 0, 0, 0))
        d.color = Color.argb(0, 0, 0, 0)
        d.addEntry(Entry(0f, value))

        sets.add(d)
        sets.add(lineDataSet)
        // create a data object with the data sets
        return LineData(sets)
    }

    /**
     * 动态添加数据
     * 在一个LineChart中存放的折线，其实是以索引从0开始编号的
     *
     * @param yValues y值
     */
    private fun addEntry(lineData: LineData, lineChart: LineChart, yValues: Float) {
        val entryCount = (lineData.getDataSetByIndex(1) as LineDataSet).entryCount
        val entry = Entry(
            entryCount.toFloat(), yValues
        )
        // 创建一个点
        lineData.addEntry(entry, 1) // 将entry添加到指定索引处的折线中
        lineChart.data = lineData
        //通知数据已经改变
        lineData.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        //通知数据已经改变
        lineData.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        //把yValues移到指定索引的位置
        lineChart.moveViewToAnimated(entryCount - 4f, yValues, YAxis.AxisDependency.LEFT, 1000)
        lineChart.setVisibleXRangeMaximum(30f)
//        lineChart.moveViewToX((lineData.entryCount - 4).toFloat())/**/
        lineChart.moveViewToX((lineData.entryCount - 29).toFloat())
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