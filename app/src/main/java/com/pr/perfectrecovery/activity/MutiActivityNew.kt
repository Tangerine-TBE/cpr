package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.ActivityMultiNewBinding
import com.pr.perfectrecovery.databinding.CycleFragmentMultiItemBinding
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.utils.TimeUtils
import com.pr.perfectrecovery.view.DialChart07View
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MutiActivityNew : BaseActivity() {
    private lateinit var binding: ActivityMultiNewBinding
    private var dataList = mutableListOf<BaseDataDTO>()
    private var isStart = false
    private var mTrainingBean: TrainingBean? = null
    private val mHandler = object : Handler(Looper.getMainLooper()) {}
    private var time: Long = 0
    private var timeZero: Long = 0
    private var counter = Counter()
    private var dataSize = 0
    private var isCheck: Boolean = false
    private var mBaseDataDTO: BaseDataDTO? = null
    private var configBean = ConfigBean()
    private var cycleCount = 0
    //按压少次
    private var prLessCount: Int = 0
    //按压多次
    private var prManyCount: Int = 0
    //吹气少次
    private var qyLessCount: Int = 0
    //吹气多次
    private var qyManyCount: Int = 0
    //中断计时累加
    private var timeOutTotal: Long = 0
    private var prValue = 0
    private var qyValue = 0
    //通气频率
    private var qyRate = 0
    private var err_pr_low = 0
    private var err_pr_high = 0
    private var err_pr_posi = 0
    private var err_qr_unback = 0
    private var isTimeOut = false
    private var isTimeing = true
    /**处理循环次数- 以及考核 超次 少次 数据统计**/
    //当前是否为按压模式-吹气模式
    private var cyclePrCount = 0
    private var cycleQyCount = 0
    private var isPr = false
    private var isQy = false
    private var startTime: Long = 0
    private var endTime: Long = 0

    companion object {
        private const val TAG = "MutiActivityNew"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)
        mTrainingBean = intent.getSerializableExtra(BaseConstant.TRAINING_BEAN) as TrainingBean
        dataSize = mTrainingBean?.list?.size!!
        isCheck = mTrainingBean?.isCheck == true
        initView()
        initPosition()
        observeData()

    }

    private fun initView() {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        time = (configBean.operationTime * 1000).toLong()

        for (i in 0 until 6) {
            val item = BaseDataDTO()
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

        initDeviceView()

        val timeDrawable = if (mTrainingBean?.isCheck == true) resources.getDrawable(R.mipmap.icon_wm_countdown) else resources.getDrawable(
            R.mipmap.icon_wm_time)
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
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_START, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                binding.tvTime.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvModel.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvCycle.setTextColor(resources.getColor(R.color.color_37B48B))
                counter.let { mHandler.post(it) }
            } else {
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.start_play_hight)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
                counter.let { mHandler.removeCallbacks(it) }
            }
        }
    }

    private fun initDeviceView() {
        dataList.forEach {
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC)) {
                val item = getItemViewByMac(it.mac)
                initDeviceView(item)
            }
        }
    }

    private fun initDeviceView(view:CycleFragmentMultiItemBinding) {
        view.let {
            it.ivPress.visibility = View.INVISIBLE
            it.pressLayoutView.visibility = View.VISIBLE
            it.dashBoard.visibility = View.INVISIBLE
            it.dashBoard2.visibility = View.INVISIBLE
            it.chart.visibility = View.VISIBLE
            it.chartQy.visibility = View.VISIBLE
            setRate(it.chart, 0)
            setRate(it.chartQy, 0)
        }
    }

    private fun initPosition() {
        binding.item1.position2.text = "1"
        binding.item1.position1.visibility = View.GONE
        binding.item3.position2.text = "3"
        binding.item3.position1.visibility = View.GONE
        binding.item5.position2.text = "5"
        binding.item5.position1.visibility = View.GONE

        binding.item2.position1.text = "2"
        binding.item2.position2.visibility = View.GONE
        binding.item4.position1.text = "4"
        binding.item4.position2.visibility = View.GONE
        binding.item6.position1.text = "6"
        binding.item6.position2.visibility = View.GONE
    }

    private fun setViewDate(viewBinding:CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO?) {
        if (dataDTO != null) {
            mBaseDataDTO = dataDTO
            //计算循环次数
            cycle(dataDTO)
            //第一次按压或吹气才开始计时
            if(startTime <= 0 && (dataDTO.prSum != 0 || dataDTO.qySum != 0)){
                startTime = System.currentTimeMillis()
            }
            //按压
            pr(viewBinding, dataDTO)
            //吹气
            qy(viewBinding, dataDTO)
            //中断超时
            if (!isTimeOut && dataDTO.distance == DataVolatile.preDistance.toInt() && dataDTO.bpValue <= 0 && dataDTO.prSum > 0) {
                isTimeOut = true
                mHandler.removeCallbacks(counter)
                mHandler.postDelayed(counter, (configBean.interruptTime * 1000).toLong())
            }
            //更新循环次数
            if (prValue != dataDTO.prSum && isTimeing) {
                isTimeing = false
                EventBus.getDefault()
                    .post(
                        MessageEventData(
                            BaseConstant.EVENT_CPR_TIMEING,
                            "",
                            null
                        )
                    )
            }
        }
    }

    private fun cycle(dataDTO: BaseDataDTO) {
//        if ((cyclePrCount >= configBean.prCount && cycleQyCount >= configBean.qyCount) || (isPress && cycleQyCount > 0 && cyclePrCount > 0)) {
        if (cycleQyCount > 0 && !isQy && isPr) {
            if (isCheck) {
                if (cyclePrCount > configBean.prCount) {
                    //按压超次
                    prManyCount += cyclePrCount - configBean.prCount
                } else {
                    //按压少次
                    prLessCount += configBean.prCount - cyclePrCount
                }
                if (cycleQyCount > configBean.qyCount) {
                    //吹气超次
                    qyManyCount += cycleQyCount - configBean.qyCount
                } else {
                    //吹气少次
                    qyLessCount += configBean.qyCount - cycleQyCount
                }
                cyclePrCount = 0
                cycleQyCount = 0
            }
            cycleCount++
            //更新循环次数
            EventBus.getDefault()
                .post(
                    MessageEventData(
                        BaseConstant.EVENT_SINGLE_DATA_CYCLE,
                        "$cycleCount",
                        null
                    )
                )
        }
    }

    private fun pr(viewBinding:CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(viewBinding.chart, dataDTO.pf)
        viewBinding.pressLayoutView.smoothScrollTo(dataDTO.distance)
        //处理是否按压
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
//            stopOutTime()
            cyclePrCount++
            cycleQyCount = 0
            isPr = true
            isQy = false
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                viewBinding.ivPressAim.visibility = View.VISIBLE
//                mHandler3.removeCallbacksAndMessages(null)
//                mHandler3.postAtTime(Runnable {
//                    viewBinding.ivPressAim.visibility = View.INVISIBLE
//                }, 2000)
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                viewBinding.pressLayoutView.setUnBack()
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    err_pr_low = dataDTO.ERR_PR_LOW
                    viewBinding.pressLayoutView.setDown()
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                }
            }
        }
        //按压错误数统计
        viewBinding.tvPress.text =
            "${(dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH + dataDTO.ERR_PR_UNBACK)}"
        //按压总数
        viewBinding.tvPressTotal.text = "/${dataDTO.prSum}"
    }

    /**
     * 吹气状态
     */
    private fun qy(viewBinding:CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1) {
            viewBinding.ivAim.visibility = View.INVISIBLE
            if (qyValue != dataDTO.qySum) {
                val qyMax = DataVolatile.max(false)
                when {
                    qyMax in configBean.qyLow()..configBean.qyHigh() -> {//通气正常
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                    qyMax in configBean.qyHigh()..100 -> {//通气过大
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                    }
                    qyMax < configBean.qyLow() -> {//通气不足
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                    }
                    qyMax > configBean.qy_max -> {//吹气进胃
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                    }
                }
                //吹气变灰
//                mHandler1.removeCallbacks(blowRunnable)
//                mHandler1.postDelayed(blowRunnable, 2000)
//                stopOutTime()
            }
        } else {
            if (dataDTO.bpValue > 5) {
//                stopOutTime()
                viewBinding.ivAim.visibility = View.VISIBLE
//                mHandler4.removeCallbacksAndMessages(null)
//                mHandler4.postAtTime(this::setQyAimVisibility, 2000)
            }
        }
        //记录吹气超次少次
        if (qyValue != dataDTO.qySum) {
            cycleQyCount++
            isQy = true
            isPr = false
        }

        if (dataDTO.bpValue <= 0 && qyRate > 0) {
            //吹气频率清零
            qyRate = 0//用于清空数据
//            mHandler2.removeCallbacks(runnableCF)
//            mHandler2.postDelayed(runnableCF, 10000)
        } else {
            if (qyRate == 0 && dataDTO.bpValue > 0) {
                qyRate = dataDTO.bpValue
            }
        }

        qyValue = dataDTO.qySum
        //吹气频率
        setRate(viewBinding.chartQy, dataDTO.cf)
        //吹气错误数统计
        viewBinding.tvLungError.text =
            "${(dataDTO.ERR_QY_CLOSE + dataDTO.ERR_QY_HIGH + dataDTO.ERR_QY_LOW + dataDTO.ERR_QY_DEAD)}"
        viewBinding.tvLungTotal.text = "/${dataDTO.qySum}"
    }


    private fun getItemViewByMac(mac:String): CycleFragmentMultiItemBinding {
        var index = -1
        for (i in 0 until dataList.size) {
            if (TextUtils.equals(mac, dataList[i].mac))
                index = i
        }
        val view = when(index) {
            0 -> binding.item1
            1 -> binding.item2
            2 -> binding.item3
            3 -> binding.item4
            4 -> binding.item5
            5 -> binding.item6
            else -> {}
        }
        return view as CycleFragmentMultiItemBinding
    }

    private fun observeData() {
        StatusLiveData.data.observe(this, Observer {
            Log.e(TAG, "prindata: mac: ${it.mac}, distance: ${it.distance}", )
            val view = getItemViewByMac(it.mac)
            setViewDate(view, it)
        })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        when (event.code) {
            BaseConstant.EVENT_CPR_TIMEING -> {
                counter.let { mHandler.post(it) }
            }
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
//                binding.tvCycle.text = adapter?.getCycleCount(it.mac).toString()
            }
            curStudentIndex ++
        }
    }

    private fun setRate(view: DialChart07View, value: Int) {
        val max = 200
        val min = 0
        val p = value % (max - min + 1) + min
        val pf = p / 200f
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        counter.let { mHandler.removeCallbacks(it) }
        EventBus.getDefault().unregister(this)
    }
}