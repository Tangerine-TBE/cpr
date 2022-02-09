package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ToastUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivitySingleBinding
import com.pr.perfectrecovery.fragment.ChartFragment
import com.pr.perfectrecovery.fragment.CycleFragment
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.TimeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 单人模式
 */
class SingleActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleBinding
    private var counter: Counter? = null
    private var mTrainingBean: TrainingBean? = null

    //训练模式成绩结果类
    private val mTrainingDTO = TrainingDTO()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)
        mTrainingBean = intent.getSerializableExtra("single") as TrainingBean
        initView()
        initViewPager()
    }

    private var isStart = false
    private fun initView() {
        binding.bottom.ivBack.setOnClickListener { finish() }
        binding.tvName.text = mTrainingBean?.name

        if (mTrainingBean?.isCheck == true) {
            counter = Counter()
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
        //定时器
        binding.bottom.ivStart.setOnClickListener {
            isStart = !isStart

            if (time <= 0) {
                ToastUtils.showShort("本次练习已结束")
                return@setOnClickListener
            }

            if (isStart) {
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_START, "", null))
                EventBus.getDefault()
                    .post(MessageEventData(BaseConstant.EVENT_SINGLE_CHART_START, "", null))
                cycleFragment.start()
                binding.bottom.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.bottom.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                counter?.let { mHandler.post(it) }
            } else {
                cycleFragment.stop()
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
                binding.bottom.ivStart.setBackgroundResource(R.drawable.start_play_hight)
                binding.bottom.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
                counter?.let { mHandler.removeCallbacks(it) }

                TrainResultActivity.start(this, TrainingDTO("测试名称"))
                finish()
            }
        }

        StatusLiveData.data.observe(this, Observer {
            binding.tvBattery.text = "${it.electricity}%"
        })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        if (event.code == BaseConstant.EVENT_SINGLE_DATA_CYCLE) {
            //循环次数
            binding.tvCycle.text = "${event.cycleCount}"
        } else if (event.code == BaseConstant.EVENT_CPR_DISCONNENT) {
            cycleFragment.bluetoothDisconnected()
        }
    }

    private lateinit var cycleFragment: CycleFragment;
    private fun initViewPager() {
        val fragments = mutableListOf<Fragment>()
        cycleFragment = CycleFragment.newInstance(mTrainingBean!!.isVoice, mTrainingBean!!.isBeat)
        fragments.add(cycleFragment)
        fragments.add(ChartFragment.newInstance())

        binding.ctChart.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.ctCurve.setOnClickListener { binding.viewPager.currentItem = 1 }
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

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
//                binding.tvPage.text = "${position + 1}/${fragments.size}"
                binding.ctChart.isChecked = false
                binding.ctCurve.isChecked = false
                if (position == 0)
                    binding.ctChart.isChecked = true
                else
                    binding.ctCurve.isChecked = true
            }
        })
    }

    override fun onDestroy() {
        counter?.let { mHandler.removeCallbacks(it) }
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private var time: Long = 1000 * 60 * 2
    private val mHandler = object : Handler(Looper.getMainLooper()) {}

    private inner class Counter : Runnable {
        override fun run() {
            mHandler.postDelayed(this, 1000);//一秒钟循环计时一次
            if (time <= 0) {
                binding.bottom.ivStart.setBackgroundResource(R.drawable.start_play_hight)
                binding.bottom.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
                mHandler.removeCallbacks(counter!!)
                cycleFragment.stop()
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
            }
            binding.tvTime.text = TimeUtils.timeParse(time)
            time -= 1000
        }
    }
}