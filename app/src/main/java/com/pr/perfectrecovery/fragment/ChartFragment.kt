package com.pr.perfectrecovery.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.LogUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
import org.greenrobot.eventbus.EventBus


class ChartFragment : Fragment() {
    private lateinit var viewBinding: ChartFragmentBinding

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
        initView()
    }

    private fun initView() {
        //曲线图表
        val data: LineData = getData()
        // add some transparency to the color with "& 0x90FFFFFF"
        setupChart(viewBinding.chart1, data)

        StatusLiveData.data.observe(requireActivity(), {
            addEntry(data, viewBinding.chart1, it.distance.toFloat())
            LogUtils.e("距离值： ${it.distance.toFloat()}")
        })

    }

    private fun getData(): LineData {
        val values = ArrayList<Entry>()
//        for (i in 0 until count) {
////            float val = (float) (Math.random() * range) + 3;
//            val va = (Math.random() / range).toFloat()
//            values.add(Entry(i.toFloat(), va))
//        }
        values.add(Entry(0f, 180f))
        // create a dataset and give it a type
        val lineDataSet = LineDataSet(values, "DataSet 1")
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);
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
        // chart.setDrawHorizontalGrid(false);
        //
        // enable / disable grid background
        chart.setDrawGridBackground(false)
        //        chart.getRenderer().getGridPaint().setGridColor(Color.WHITE & 0x70FFFFFF);

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

        if (yValues < 120) {

        }

        if (yValues > 130) {

        }
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