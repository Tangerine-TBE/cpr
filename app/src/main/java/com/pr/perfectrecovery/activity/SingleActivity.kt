package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckedTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.ActivitySingleBinding
import com.pr.perfectrecovery.fragment.ChartFragment
import com.pr.perfectrecovery.fragment.CheckEventFragment
import com.pr.perfectrecovery.fragment.CycleFragment
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile01
import com.pr.perfectrecovery.utils.TimeUtils
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.abs

/**
 * 单人模式
 */
class SingleActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleBinding
    private var counter = Counter()
    private var mTrainingBean: TrainingBean? = null
    private var isShow = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)
//        DataVolatile.dataClear()
        mTrainingBean = intent.getSerializableExtra(BaseConstant.TRAINING_BEAN) as TrainingBean
        mTrainingBean?.isCheck?.let { DataVolatile01.setModel(it) }
        initView()
        initViewPager()
    }

    private var isStart = false
    var configBean: ConfigBean? = null
    private fun initView() {
        binding.bottom.ivBack.setOnClickListener { finish() }
        binding.tvName.text = mTrainingBean?.name
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        operateTime = (configBean!!.operationTime * 1000).toLong()
        if (mTrainingBean?.isCheck == true) {
            binding.tvTime.setCompoundDrawablesWithIntrinsicBounds(
                resources.getDrawable(R.mipmap.icon_wm_countdown),
                null,
                null,
                null
            )
        }
        //定时器
        binding.bottom.ivStart.setOnClickListener {
            isStart = !isStart
            if (isStart) {
//                DataVolatile.isStart = true
                EventBus.getDefault()
                    .post(MessageEventData(BaseConstant.EVENT_SINGLE_CHART_START, "", null))
                cycleFragment?.start()
                binding.bottom.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.bottom.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                binding.tvTime.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvCycle.setTextColor(resources.getColor(R.color.color_37B48B))
                if (mTrainingBean?.isCheck!!) {
                    binding.tvTime.setCompoundDrawablesWithIntrinsicBounds(
                        resources.getDrawable(R.mipmap.icon_wm_countdown),
                        null,
                        null,
                        null
                    )
                } else {
                    binding.tvTime.setCompoundDrawablesWithIntrinsicBounds(
                        resources.getDrawable(R.mipmap.icon_wm_time),
                        null,
                        null,
                        null
                    )
                }
                counter.let { mHandler.post(it) }
            } else {
                startResult()
            }
        }
    }

    private fun startResult() {
        val mTrainingDTO = cycleFragment?.stop()
        binding.bottom.ivStart.setBackgroundResource(R.drawable.start_play_hight)
        binding.bottom.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
        counter.let { mHandler.removeCallbacks(it) }
        mTrainingDTO?.isCheck = mTrainingBean!!.isCheck
        mTrainingDTO?.name = binding.tvName.text.toString().trim()
        mTrainingDTO?.cycleCount = binding.tvCycle.text.toString().trim().toInt()
        mTrainingDTO?.operateTime = if (operateTime2 > 0) operateTime2 else 0
        //检查页面 结果
        checkEventFragment?.getData().let {
            if (it != null) {
                mTrainingDTO?.check1 = it.check1
                mTrainingDTO?.check2 = it.check2
                mTrainingDTO?.check3 = it.check3
                mTrainingDTO?.check4 = it.check4
                mTrainingDTO?.check5 = it.check5
                mTrainingDTO?.check6 = it.check6
                mTrainingDTO?.check7 = it.check7
                mTrainingDTO?.check8 = it.check8
                mTrainingDTO?.check9 = it.check9
                mTrainingDTO?.check10 = it.check10
            }
        }

        if (mTrainingDTO != null) {
            TrainResultActivity.start(this, mTrainingDTO)
        }
        finish()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        when (event.code) {
            BaseConstant.EVENT_SINGLE_DATA_CYCLE -> {
                //循环次数
                binding.tvCycle.text = "${event.cycleCount}"
            }
            BaseConstant.EVENT_CPR_DISCONNENT -> {
                cycleFragment?.bluetoothDisconnected()
            }
            BaseConstant.EVENT_CPR_TIMEING -> {
                counter.let { mHandler.post(it) }
            }
            BaseConstant.EVENT_CPR_TIMEING -> {
                counter.let { mHandler.post(it) }
            }
            BaseConstant.EVENT_SINGLE_END -> {
                startResult()
            }
        }
    }

    private var cycleFragment: CycleFragment? = null
    private var checkEventFragment: CheckEventFragment? = CheckEventFragment.newInstance()
    private fun initViewPager() {
        var curItem = 0
        val isCheck = mTrainingBean?.isCheck
        val fragments = mutableListOf<Fragment>()
        val titleBtns = mutableListOf<CheckedTextView>()

        if (isCheck == true) {
            fragments.add(checkEventFragment!!)
            binding.ctEvent.visibility = View.VISIBLE
            val indexEvent = curItem++
            binding.ctEvent.setOnClickListener {
                isShow = false
                binding.viewPager.currentItem = indexEvent
            }
            titleBtns.add(binding.ctEvent)
        }

        cycleFragment = CycleFragment.newInstance(
            mTrainingBean!!.isVoice,
            mTrainingBean!!.isBeat,
            mTrainingBean!!.isCheck
        )
        fragments.add(cycleFragment!!)
        val indexChar = curItem++
        binding.ctChart.setOnClickListener { binding.viewPager.currentItem = indexChar }
        binding.ctChart.isChecked = indexChar == 0
        titleBtns.add(binding.ctChart)

        fragments.add(ChartFragment.newInstance())
        val indexCure = curItem++
        binding.ctCurve.setOnClickListener { binding.viewPager.currentItem = indexCure }
        titleBtns.add(binding.ctCurve)

        binding.viewPager.offscreenPageLimit = fragments.size
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }

        StatusLiveData.data.observe(this) {
            if (it != null) {
                binding.tvBattery.power = it.electricity
                if (!isShow && abs(it.preDistance - it.distance) > 10 && isCheck == true) {
                    isShow = true
                    binding.ctChart.isChecked = true
                    binding.ctCurve.isChecked = false
                    binding.ctEvent.isChecked = false
                    binding.viewPager.currentItem = 1
                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                titleBtns.forEach {
                    it.isChecked = false
                }
                titleBtns[position].isChecked = true
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        counter.let { mHandler.removeCallbacks(it) }
        cycleFragment = null
        checkEventFragment = null
//        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
        EventBus.getDefault().unregister(this)
    }

    //总操作时长
    private var operateTime: Long = 0

    //实际操作时长
    private var operateTime2: Long = 0
    private val mHandler = object : Handler(Looper.getMainLooper()) {}

    private inner class Counter : Runnable {
        override fun run() {
            mHandler.postDelayed(this, 1000)//一秒钟循环计时一次
            //训练模式 考核模式
            if (mTrainingBean?.isCheck!!) {
                operateTime -= 1000
                binding.tvTime.text = TimeUtils.timeParse(operateTime)
                if (operateTime <= 0) {
                    mHandler.removeCallbacksAndMessages(null)
                    startResult()
                }
            } else {
                binding.tvTime.text = TimeUtils.timeParse(operateTime2)
            }
            operateTime2 += 1000
        }
    }
}