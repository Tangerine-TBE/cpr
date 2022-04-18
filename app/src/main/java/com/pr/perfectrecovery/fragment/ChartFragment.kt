package com.pr.perfectrecovery.fragment

import android.graphics.Color
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
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.ChartFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.ChartViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.math.roundToInt

/**
 * 曲线
 */
class ChartFragment : Fragment() {
    private lateinit var viewBinding: ChartFragmentBinding
    private lateinit var configBean: ConfigBean
    private val TAG = ChartFragment::class.java.simpleName

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
        val data: LineData = getData(0f, false)
        val data1: LineData = getData(0f, true)
        val data2: LineData = getData(0f, false)
//        depth_threshold_low = configBean.depth * 2 - 28    //下限值放大8mm  需要定义变量  上下限阈值要放缩
//        depth_threshold_high = configBean.depthEnd * 2 - 12  //上限值放大8mm
        depth_threshold_low = (configBean.prLow() * 1.4).toInt()   //下限值放大8mm  需要定义变量  上下限阈值要放缩
        depth_threshold_high = (configBean.prHigh() * 1.4).toInt()  //上限值放大8mm
        depth_Frequency_high = (configBean.depthFrequencyEnd)
        depth_Frequency_low = (configBean.depthFrequency)
        // add some transparency to the color with "& 0x90FFFFFF"
        initLineChart(viewBinding.lineChart, data)
        LineChartUtils.setLineChart(
            viewBinding.lineChart1,
            data1,
            6,
            9
        )
//        initLineChart(viewBinding.lineChart1, data1)
        initLineChart(viewBinding.lineChart2, data2)
        StatusLiveData.data.observe(requireActivity()) {
            if (it != null) {
                setData(it)
                addEntry(data, viewBinding.lineChart, getBlowFrequencyValue(it.cf))
                addEntry(data1, viewBinding.lineChart1, setValue(it.distance, it))
//                addEntry(data2, viewBinding.lineChart1, it)
                addEntry(data2, viewBinding.lineChart2, getFrequencyValue(it.pf))
                if (qyValue != it.qySum) {
                    qyValue = it.qySum
                    val qyMax = it.qyMaxValue
                    addBarEntry(it.qyValueSum, qyMax)
                } else {
                    addBarEntry(0, 0)
                }
            }
        }

        initBarChart()
        viewBinding.constraintlayout2.setOnClickListener {
//            addBarEntry(Random().nextInt(800), 20)
        }

        viewBinding.constraintlayout.setOnClickListener {
//            addEntry(data, viewBinding.lineChart, 0f)
            val random = (1..20).random()
            //addEntry(data2, viewBinding.lineChart, getBlowFrequencyValue(random))
        }

        viewBinding.constraintlayout4.setOnClickListener {
            val random = (1..200).random()
            //addEntry(data, viewBinding.lineChart2, getFrequencyValue(random))
        }
        setViewData()
    }

    private var depth_threshold_low = 0
    private var depth_threshold_high = 0
    private fun setValue(value: Int, data: BaseDataDTO): Float {
        val depth = data.preDistance - value
        Log.e("depth", "$depth")
//        depth_threshold_low = data.PR_LOW_VALUE * 2 - 28    //下限值放大8mm  需要定义变量  上下限阈值要放缩
//        depth_threshold_high = data.PR_HIGH_VALUE * 2 - 12  //上限值放大8mm
        if (depth == 0 || depth <= 8) {
            return 0f                         //    小于8的按压曲线归零
        } else if (depth <= depth_threshold_low) {
            return (6 / depth_threshold_low.toFloat() * depth.toFloat())       //  按压不足 显示区域0-6
        } else if (depth in depth_threshold_low..depth_threshold_high) {
            return (3 / (depth_threshold_high - depth_threshold_low).toFloat() * (depth.toFloat() - depth_threshold_low.toFloat()) + 6.0f)    //按压正确 显示区域6-9
        } else if (depth in depth_threshold_high..129) {
            return (1 / (129 - depth_threshold_high.toFloat()) * (depth - depth_threshold_high.toFloat()) + 9)                  // 按压过大 显示区域9-10
        } else if (depth >= 129) {
            return 9.5f   // 按压显示到极限高度10
        }
        return 0f
    }

    /**
     * 频率算法 0-200
     */
    private var depth_Frequency_low = 0
    private var depth_Frequency_high = 0
    private fun getFrequencyValue(depth: Int): Float {
        Log.e("getFrequencyValue depth", "$depth")
        var value = depth
        if (depth > 200) {
            value = 200
        }
        val result = when {
            value <= depth_Frequency_low -> {
                (6 / depth_Frequency_low.toFloat() * value.toFloat())//  按压不足 显示区域0-2
            }
            value in depth_Frequency_low..depth_Frequency_high -> {
                (3 / (depth_Frequency_high - depth_Frequency_low).toFloat() * (depth.toFloat() - depth_Frequency_low.toFloat()) + 6.0f)//显示区域2-8
            }
            value > depth_Frequency_high -> {
                (1 / ((depth_Frequency_high + 9) - depth_Frequency_high.toFloat()) * (depth.toFloat() - depth_Frequency_high.toFloat()) + 2)// 显示区域8-10
            }
            else -> 9.5f
        }
        return if (result > 10) 9.9f else result
    }

    /**
     * 吹气频率算法 0-200
     */
    private var blow_Frequency_low = 0
    private var blow_Frequency_high = 0
    private fun getBlowFrequencyValue(depth: Int): Float {
        var value = depth
        if (value > 20) {
            value = 20
        }
        Log.e("getFrequencyValue depth", "$value")
        val result = when {
            value <= blow_Frequency_low -> {
                (6 / blow_Frequency_low.toFloat() * value.toFloat())//显示区域0-2
            }
            value in blow_Frequency_low..blow_Frequency_high -> {
                (3 / (blow_Frequency_high - blow_Frequency_low).toFloat() * (depth.toFloat() - blow_Frequency_high.toFloat()) + 6.0f)//显示区域2-8
            }
            value > blow_Frequency_high -> {
                (1 / ((blow_Frequency_high + 9) - blow_Frequency_high.toFloat()) * (depth.toFloat() - blow_Frequency_high.toFloat()) + 6.5f)// 显示区域8-10
            }
            else -> 9.9f
        }
        return if (result > 10) 9.9f else result
    }

    private fun setViewData() {
        viewBinding.textView22.text = "${configBean.depth}"
        viewBinding.tvDepthEnd.text = "${configBean.depthEnd}"
        viewBinding.tvDepthFrequency.text = "${configBean.depthFrequency}"
        viewBinding.tvDepthFrequencyEnd.text = "${configBean.depthFrequencyEnd}"
        viewBinding.tvTidalFrequency.text = "${configBean.tidalFrequency}"
        viewBinding.tvTidalFrequencyEnd.text = "${configBean.tidalFrequencyEnd}"
        viewBinding.tvTidalVolume.text = "${configBean.tidalVolume}"
        viewBinding.tvTidalVolumeEnd.text = "${configBean.tidalVolumeEnd}"
    }

    private fun initLineChart(lineChart: LineChart, lineData: LineData) {
        // apply styling
        // holder.chart.setValueTypeface(mTf);
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setDrawGridBackground(false)
        lineChart.setNoDataText("暂无数据")
        val xAxis: XAxis = lineChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
//        xAxis.setLabelCount(30, true)
        xAxis.position = XAxisPosition.BOTTOM

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.setLabelCount(5, false)
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        leftAxis.mAxisMinimum = 0f
        leftAxis.mAxisMaximum = 10f
        leftAxis.textColor = Color.WHITE

        val rightAxis: YAxis = lineChart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        lineData.setDrawValues(false)
        lineData.setValueTextColor(Color.WHITE)
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
            isHighlightFullBarEnabled = false
//            scaleX = 1.5f
            //x轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM//X轴的位置 默认为上面
                setDrawGridLines(false)  //是否绘制X轴上的网格线（背景里面的竖线）
                //axisRight.isEnabled = false//隐藏右侧Y轴   默认是左右两侧都有Y轴
                granularity = 1f // only intervals of 1 day
                mAxisMinimum = 0f
            }
            xAxis.setLabelCount(12, false)
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisLeft.textColor = Color.WHITE
            axisLeft.labelCount = 4
            axisLeft.axisMaxLabels = 4
            axisLeft.mAxisMaximum = 1200f
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
            colors.add(
                ContextCompat.getColor(requireContext(), R.color.tran)
            )
            mBarDataSet!!.colors = colors
            val barData = BarData(dataSets)
            barData.addEntry(BarEntry(0f, 3.8f), 0)
            data = barData
//            data.barWidth = 0.3f
//            addBarEntry(0, 100)
        }
    }

    private val values = ArrayList<BarEntry>()
    private val colors = ArrayList<Int>()
    private var filterValue = 0

    //这里要进行图像绘制，所以要切回UI线程，否则会报错
    private fun addBarEntry(value: Int, value2: Int) {
        if (value2 > 0) {
            Log.e("addBarEntry", "$value2")
        }
        viewBinding.barChart.apply {
            if (barData != null) {
                val entryCount = (data.getDataSetByIndex(0) as BarDataSet).entryCount
                if (value2 > 0) {
                    when {
                        value2 <= configBean.tidalVolume -> {
                            data.addEntry(BarEntry(entryCount.toFloat(), 1.0f), 0)
                        }
                        value2 <= configBean.tidalVolumeEnd -> {
                            data.addEntry(BarEntry(entryCount.toFloat(), 2.2f), 0)
                        }
                        value2 > configBean.tidalVolumeEnd -> {
                            data.addEntry(BarEntry(entryCount.toFloat(), 3.4f), 0)
                        }
                        else -> {
                            data.addEntry(BarEntry(entryCount.toFloat(), 3.6f), 0)
                        }
                    }
                    when {
                        value2 < configBean.tidalVolume -> {
                            colors.add(
                                ContextCompat.getColor(requireContext(), R.color.color_FDC457)
                            )
                        }
                        value2 <= configBean.tidalVolumeEnd -> {
                            colors.add(
                                ContextCompat.getColor(requireContext(), R.color.color_37B48B)
                            )
                        }
                        value2 > configBean.tidalVolumeEnd -> {
                            colors.add(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.color_text_selected
                                )
                            )
                        }
                    }
                } else {
                    //延迟移动度
                    if (filterValue == 0) {
                        filterValue = 1
                        colors.add(
                            ContextCompat.getColor(requireContext(), R.color.color_FDC457)
                        )
                        data.addEntry(BarEntry(entryCount.toFloat(), 0f), 0)
                    } else {
                        filterValue = 0
                    }
                }
                mBarDataSet!!.colors = colors
                data.notifyDataChanged()
                notifyDataSetChanged()
                //设置在图表中显示的最大X轴数量
                setVisibleXRangeMaximum(12f)
                //这里用29是因为30的话，最后一条柱子只显示了一半
                moveViewToX(barData.entryCount.toFloat() - 12)
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
        //吹气总数
        viewBinding.tvLungCount.text = "${(data.getQy_err_total())}"
        viewBinding.tvLungTotal.text = "/${data.qySum}"

        //按压总数
        viewBinding.tvHeartCount.text = "${(data.getPr_err_total())}"
        viewBinding.tvHeartTotal.text = "/${data.prSum}"
    }

    private fun getData(value: Float, isBezier: Boolean): LineData {
        val values = ArrayList<Entry>()
//        values.add(Entry(0f, value.toFloat()))
        // create a dataset and give it a type
        val VORDIPLOM_COLORS = intArrayOf(
            Color.rgb(61, 179, 142)
        )
        val lineDataSet = LineDataSet(values, "DataSet 1")
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
        lineDataSet.addEntry(Entry(0f, 9.5f))
        lineDataSet.addEntry(Entry(0f, 0f))
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
        Log.e(TAG, "addEntry: $yValues")
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
        lineChart.moveViewToAnimated(entryCount - 1f, yValues, YAxis.AxisDependency.LEFT, 800)
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