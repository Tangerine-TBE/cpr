package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.adapter.MultiActAdapter
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.ActivityMultiBinding
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.utils.TimeUtils
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * desc   : 多模块界面适
 * author : hunger
 * date   : 2022/2/24
 * version: 1.0
 */
class MultiActivity : BaseActivity() {
    private lateinit var binding: ActivityMultiBinding
    private var adapter: MultiActAdapter? = null
    private var dataList = mutableListOf<BaseDataDTO>()
    private var isStart = false
    private var mTrainingBean: TrainingBean? = null
    private val mHandler = object : Handler(Looper.getMainLooper()) {}
    private var time: Long = 0
    private var timeZero: Long = 0
    private var counter = Counter()
    private var dataSize = 0
    private var currentDataDTO: BaseDataDTO? = null

    companion object {
        private const val TAG = "MultiActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)
        mTrainingBean = intent.getSerializableExtra(BaseConstant.TRAINING_BEAN) as TrainingBean
        dataSize = mTrainingBean?.list?.size!!
        adapter = MultiActAdapter()
        initView()
        showData()
    }
    private fun initView() {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        val configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        time = (configBean.operationTime * 1000).toLong()

        // 先初始化几个占位数据
        for (i in 0 until 6) {
            var item = BaseDataDTO()
            if (i < dataSize) {
                val bean = mTrainingBean?.list?.get(i)
                item.mac = bean?.mac.toString()
                item.isStart = false
                item.distance = 0
                item.bpValue = 0
            } else {
                item.mac = BaseConstant.FAKE_MAC
                item.isStart = false
                item.distance = 0
                item.bpValue = 0
            }

            dataList.add(item)
        }

        adapter?.setList(dataList)

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.actMulRecycler.layoutManager = layoutManager
        binding.actMulRecycler.adapter = adapter

        val timeDrawable = if (mTrainingBean?.isCheck == true) resources.getDrawable(R.mipmap.icon_wm_countdown) else resources.getDrawable(R.mipmap.icon_wm_time)
        binding.tvTime.setCompoundDrawablesWithIntrinsicBounds(
            timeDrawable,
            null,
            null,
            null
        )

        binding.oprLayout.ivBack.setOnClickListener { finish() }
        binding.oprLayout.ivStart.setOnClickListener {
            isStart = !isStart
            if (isStart) {
                DataVolatile.isStart = true
                val iterator = dataList.iterator()
                while (iterator.hasNext()) {
                    iterator.next().isStart = true
                }
                adapter?.setList(dataList)

                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_START, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                binding.tvTime.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvModel.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvCycle.setTextColor(resources.getColor(R.color.color_37B48B))
                counter.let { mHandler.post(it) }
            } else {
                val iterator = dataList.iterator()
                while (iterator.hasNext()) {
                    iterator.next().isStart = false
                }
                adapter?.setList(dataList)

                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
                DataVolatile.isStart = false
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.start_play_hight)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
                counter.let { mHandler.removeCallbacks(it) }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        when (event.code) {
            BaseConstant.EVENT_CPR_TIMEING -> {
                counter.let { mHandler.post(it) }
            }
        }
    }

    private fun showData() {
        StatusLiveData.data.observe(this, Observer {
            Log.e(TAG, "mac: ${it.mac}; distance: ${it.distance}, bpValue :${it.bpValue}", )
            updateData(it)
        })
    }

    private fun updateData(data:BaseDataDTO) {
        var index = -1
        adapter?.data?.forEach {
            if (it.mac == data.mac) {
                index = adapter?.data?.indexOf(it) ?: -1
            }
        }
        if (index != -1){
            adapter?.setData(index, data)
            Log.e(TAG, "updateData: index: ${index}, data is: ${data.toString()}", )
        }
    }

    var curStudentIndex = 0
    private inner class Counter : Runnable {
        override fun run() {
            mHandler.postDelayed(this, 1000);//一秒钟循环计时一次
            if (time <= 0) {
                mHandler.removeCallbacks(counter)
            }
            if (!mTrainingBean?.isCheck!!) {
                updateCycleData(timeZero)
                binding.tvTime.text = TimeUtils.timeParse(timeZero)
                timeZero += 1000
            } else {
                updateCycleData(time)
                binding.tvTime.text = TimeUtils.timeParse(time)
                time -= 1000
            }
        }
    }

    fun updateCycleData(time:Long) {
        if (time % BaseConstant.INTERVAL_TIME == 0L) {
            curStudentIndex %= dataSize
            mTrainingBean?.list?.get(curStudentIndex)?.let {
                binding.tvModel.text = "${it.count}"
                binding.tvCycle.text = adapter?.getCycleCount(it.mac).toString()
            }
            curStudentIndex ++
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        counter.let { mHandler.removeCallbacks(it) }
        EventBus.getDefault().unregister(this)
    }


}