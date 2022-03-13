package com.pr.perfectrecovery.activity

import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivityMultiNewBinding
import com.pr.perfectrecovery.databinding.CycleFragmentMultiItemBinding
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.TimeUtils
import com.pr.perfectrecovery.view.DialChart07View
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.abs

class MutiActivityNew : BaseActivity() {
    private lateinit var binding: ActivityMultiNewBinding
    private var dataList = mutableListOf<BaseDataDTO>()
    private var isStart = false
    private var mTrainingBean: TrainingBean? = null
    private val headTimeHandler = object : Handler(Looper.getMainLooper()) {}
    private var time: Long = 0
    private var timeZero: Long = 0
    private var counter = Counter()
    private var dataSize = 0
    private var isCheck: Boolean = false
    private var mBaseDataDTOMap = mutableMapOf<String, BaseDataDTO>()
    private var configBean = ConfigBean()
    private var cycleCountMap = mutableMapOf<String, Int>()
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
    private var isTimeOutMap = mutableMapOf<String, Boolean>()
    private var isTimeing = true
    /**处理循环次数- 以及考核 超次 少次 数据统计**/
    //当前是否为按压模式-吹气模式
    private var cyclePrCountMap = mutableMapOf<String, Int>()
    private var cycleQyCountMap = mutableMapOf<String, Int>()
    private var isPrMap = mutableMapOf<String, Boolean>()
    private var isQyMap = mutableMapOf<String, Boolean>()
    private var isCycleMap = mutableMapOf<String, Boolean>()
    private var startTimeMap = mutableMapOf<String, Long>()
    private var endTime: Long = 0
    private var currentShowView: ConstraintLayout? = null
    private var curStudentIndex = 0
    //是否已经结束
    private var isOver = false
    //记录考核完成状态，onResume时恢复仪表盘
    private var hasDoneMap = mutableMapOf<String, Boolean>()
    private var handler:Handler = object :Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val binding = msg.obj as CycleFragmentMultiItemBinding
            when(msg.what) {
                INIT_PRESS -> {
                    initPress(binding)
                }
                INIT_LUNG -> {
                    initLung(binding)
                }
                START_TIME -> {
                    startTime(binding)
                }
                STOP_TIME -> {

                }

            }
        }
    }


    companion object {
        private const val TAG = "MutiActivityNew"
        //初始化按压
        private const val INIT_PRESS = 10001
        //初始化吹气
        private const val INIT_LUNG = 10002
        //开始计时
        private const val START_TIME = 10003
        //结束计时
        private const val STOP_TIME = 10004
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

        binding.oprLayout.ivBack.setOnClickListener {
            EventBus.getDefault().post(MessageEventData(BaseConstant.CLEAR_DEVICE_HISTORY_DATA, "", null))
            finish()
        }
        binding.oprLayout.ivStart.setOnClickListener {
            if (isOver) {
                val notice = "本次${if (isCheck) "考核" else "练习"}已结束"
                ToastUtils.showShort(notice)
                return@setOnClickListener
            }
            isStart = !isStart
            if (isStart) {
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_START, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                binding.tvTime.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvModel.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvCycle.setTextColor(resources.getColor(R.color.color_37B48B))
                counter.let { headTimeHandler.post(it) }
            } else {
                stopAllOutTime()
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.start_play_hight)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
                counter.let { headTimeHandler.removeCallbacks(it) }
                showScore()
                isOver = true
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
            mBaseDataDTOMap[dataDTO.mac] = dataDTO
            //计算循环次数
            cycle(dataDTO)
            //第一次按压或吹气才开始计时
            val startTime = startTimeMap[dataDTO.mac] ?: 0L
            if(startTime <= 0 && (dataDTO.prSum != 0 || dataDTO.qySum != 0)){
                startTimeMap[dataDTO.mac] = System.currentTimeMillis()
            }
            showView(viewBinding, dataDTO)
//            //按压
//            pr(viewBinding, dataDTO)
//            //吹气
//            qy(viewBinding, dataDTO)
            //中断超时
            val isTime = isTimeOutMap[dataDTO.mac] ?: false
            if (!isTime && dataDTO.distance == dataDTO.preDistance && dataDTO.bpValue <= 0 && dataDTO.prSum > 0) {
                isTimeOutMap[dataDTO.mac] = true
                handler.removeMessages(START_TIME, viewBinding)
                sendMsg(START_TIME, viewBinding, 1000)
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

    private fun showView(viewBinding:CycleFragmentMultiItemBinding, data: BaseDataDTO) {
        val preDistance = data.preDistance
        val isPress = abs(preDistance - data.distance) > 10
        val isBlow = data.bpValue > 5

        //没有按压 也没有吹气，显示上一次的视图
        if (!isPress && !isBlow) {
            currentShowView?.visibility = View.VISIBLE
        } else if (isPress) {
            viewBinding.layoutScore.visibility = View.GONE
            viewBinding.layoutLung.visibility = View.GONE
            viewBinding.layoutPress.visibility = View.VISIBLE
            currentShowView = viewBinding.layoutPress
            pr(viewBinding, data)
        } else if(isBlow) {
            viewBinding.layoutPress.visibility = View.GONE
            viewBinding.layoutScore.visibility = View.GONE
            viewBinding.layoutLung.visibility = View.VISIBLE
            viewBinding.ivLung.setImageResource(R.mipmap.icon_lung_border)
            currentShowView = viewBinding.layoutLung
            qy(viewBinding, data)
        }
    }

    private fun cycle(dataDTO: BaseDataDTO) {
//        if ((cyclePrCount >= configBean.prCount && cycleQyCount >= configBean.qyCount) || (isPress && cycleQyCount > 0 && cyclePrCount > 0)) {
        val isPr = isPrMap[dataDTO.mac] ?: false
        val isQy = isQyMap[dataDTO.mac] ?: false
        val isCycle = isCycleMap[dataDTO.mac] ?: false
        if (isQy && !isPr && !isCycle) {
            isCycleMap[dataDTO.mac] = true
            isQyMap[dataDTO.mac] = false
            isPrMap[dataDTO.mac] = false
            if (isCheck) {
                val cyclePrCount = cyclePrCountMap[dataDTO.mac] ?: 0
                val cycleQyCount = cycleQyCountMap[dataDTO.mac] ?: 0
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
                cyclePrCountMap[dataDTO.mac] = 0
                cycleQyCountMap[dataDTO.mac] = 0
            }
            var cycleCount = cycleCountMap[dataDTO.mac] ?: 0
            cycleCount ++
            cycleCountMap[dataDTO.mac] = cycleCount
        }
    }

    private fun pr(viewBinding:CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(viewBinding.chart, dataDTO.pf)
        viewBinding.pressLayoutView.smoothScrollTo(dataDTO.distance, dataDTO)
        //处理是否按压
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
            stopOutTime(viewBinding, dataDTO)
            var prCount = cyclePrCountMap[dataDTO.mac] ?: 0
            prCount ++
            cyclePrCountMap[dataDTO.mac] = prCount
            cycleQyCountMap[dataDTO.mac] = 0
            isPrMap[dataDTO.mac] = true
            isQyMap[dataDTO.mac] = false
            isCycleMap[dataDTO.mac] = false
            //按压位置错误
            if (err_pr_posi != dataDTO.err_pr_posi && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.err_pr_posi
                viewBinding.ivPressAim.visibility = View.VISIBLE
                handler.removeMessages(INIT_PRESS, viewBinding)
                sendMsg(INIT_PRESS, viewBinding)
            } else if (err_qr_unback != dataDTO.err_pr_unback) {
                //按压未回弹
                err_qr_unback = dataDTO.err_pr_unback
                viewBinding.pressLayoutView.setUnBack()
            } else {
                //按压不足
                if (err_pr_low != dataDTO.err_pr_low) {
                    err_pr_low = dataDTO.err_pr_low
                    viewBinding.pressLayoutView.setDown()
                } else if (err_pr_high != dataDTO.err_pr_high) {//按压过大
                    err_pr_high = dataDTO.err_pr_high
                }
            }
        }
        //按压错误数统计
        viewBinding.tvPress.text =
            "${(dataDTO.err_pr_posi + dataDTO.err_pr_low + dataDTO.err_pr_high + dataDTO.err_pr_unback)}"
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
                val qyMax = dataDTO.qyMax()
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
                sendMsg(INIT_LUNG, viewBinding)
                stopOutTime(viewBinding, dataDTO)
            }
        } else {
            if (dataDTO.bpValue > 5) {
                stopOutTime(viewBinding, dataDTO)
                viewBinding.ivAim.visibility = View.VISIBLE
                sendMsg(INIT_LUNG, viewBinding)
            }
        }
        //记录吹气超次少次
        if (qyValue != dataDTO.qySum) {
            var qyCount = cycleQyCountMap[dataDTO.mac] ?: 0
            qyCount++
            cycleQyCountMap[dataDTO.mac] = qyCount
            isQyMap[dataDTO.mac] = true
            isPrMap[dataDTO.mac] = false
        }

        if (dataDTO.bpValue <= 0 && qyRate > 0) {
            //吹气频率清零
            qyRate = 0//用于清空数据
        } else {
            if (qyRate == 0 && dataDTO.bpValue > 0) {
                qyRate = dataDTO.bpValue
            }
        }

        qyValue = dataDTO.qySum
        //吹气频率
        setQyRate(viewBinding.chartQy, dataDTO.cf)
        //吹气错误数统计
        viewBinding.tvLungError.text =
            "${(dataDTO.err_qy_close + dataDTO.err_qy_high + dataDTO.err_qy_low + dataDTO.err_qy_dead)}"
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
            if (isStart)
                setViewDate(view, it)
        })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        when (event.code) {
//            BaseConstant.EVENT_CPR_TIMEING -> {
//                counter.let { mHandler.post(it) }
//            }
        }
    }

    //不让用户返回，只能点下面的返回按钮
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("确认结束测试并退出？")
            .setPositiveButton("确定") { dialog, _ ->
                EventBus.getDefault().post(MessageEventData(BaseConstant.CLEAR_DEVICE_HISTORY_DATA, "", null))
                dialog.cancel()
                finish()
            }
            .setNegativeButton("取消"){ dialog , _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private inner class Counter : Runnable {
        override fun run() {
            headTimeHandler.postDelayed(this, 1000);//一秒钟循环计时一次
            if (time <= 0) {
                headTimeHandler.removeCallbacks(counter)
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
                binding.tvCycle.text = "${cycleCountMap[it.mac] ?: 0}"
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

    private fun setQyRate(view: DialChart07View, value: Int) {
        val max = 16f
        val min = 0
        val p = value % (max - min + 1) + min
        var pf = p / 200f

        when {
            value < 1 -> {
                pf = 0.0f
            }
            value > 16 -> {
                pf = 1.0f
            }
            value > 14 -> {
                pf = 0.95f
            }
            value > 12 -> {
                pf = 0.9f
            }
            value > 10 -> {
                pf = 0.8f
            }
            value > 8 -> {
                pf = 0.7f
            }
            value < 2 -> {
                pf = 0.1f
            }
            value < 3 -> {
                pf = 0.2f
            }
            value < 4 -> {
                pf = 0.3f
            }
            value < 5 -> {
                pf = 0.4f
            }
            value < 6 -> {
                pf = 0.4f
            }
            value in 6..8 -> {
                pf = 0.5f
            }

        }
        Log.e("setQyRate", "Rate: ${pf}")
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    private fun sendMsg(type:Int, binding: CycleFragmentMultiItemBinding, delayTime:Long = 2000) {
        handler.removeMessages(type, binding)
        var msg = Message()
        msg.what = type
        msg.obj = binding
        handler.sendMessageDelayed(msg, delayTime)
    }

    private fun showScore() {
        dataList.forEach { data->
            if (!TextUtils.equals(data.mac, BaseConstant.FAKE_MAC)) {
                val item = getItemViewByMac(data.mac)
                item.layoutLung.visibility = View.GONE
                item.layoutPress.visibility = View.GONE
                item.layoutScore.visibility = View.VISIBLE
                val score = countScore()
                var ratingBar : RatingBar = item.ratingBar
                when(score) {
                    in 0..20 ->{
                        item.ratingBarRed.visibility = View.VISIBLE
                        item.ratingBar.visibility = View.GONE
                        item.ratingBarYellow.visibility = View.GONE
                        ratingBar = item.ratingBarRed
                    }
                    in 21..60 ->{
                        item.ratingBarYellow.visibility = View.VISIBLE
                        item.ratingBar.visibility = View.GONE
                        item.ratingBarRed.visibility = View.GONE
                        ratingBar = item.ratingBarYellow
                    }
                    in 61..100 ->{
                        item.ratingBar.visibility = View.VISIBLE
                        item.ratingBarRed.visibility = View.GONE
                        item.ratingBarYellow.visibility = View.GONE
                        ratingBar = item.ratingBar
                    }
                }
                item.tvScore.text = "$score"
                ratingBar.rating = (5.0 * countScore() / 100).toFloat()

                item.layoutScore.setOnClickListener {



                    val mTrainingDTO = TrainingDTO()
                    mTrainingDTO.isCheck = mTrainingBean!!.isCheck
                    mTrainingDTO.cycleCount = binding.tvCycle.text.toString().trim().toInt()
                    mTrainingDTO.timeTotal = (configBean.operationTime * 1000).toLong()
                    mTrainingDTO.prCount = configBean.prCount
                    mTrainingDTO.qyCount = configBean.qyCount
                    mTrainingDTO.pressScore = configBean.pressScore
                    mTrainingDTO.blowScore = configBean.blowScore
                    mTrainingDTO.processScore = configBean.processScore.toFloat()
                    mTrainingDTO.deduction = configBean.deductionScore


                    mTrainingDTO.check1 = true
                    mTrainingDTO.check2 = true
                    mTrainingDTO.check3 = true
                    mTrainingDTO.check4 = true
                    mTrainingDTO.check5 = true
                    mTrainingDTO.check6 = true
                    mTrainingDTO.check7 = true
                    mTrainingDTO.check8 = true
                    mTrainingDTO.check9 = true
                    mTrainingDTO.check10 = true
                    TrainResultActivity.start(this, mTrainingDTO)
                }
            }
        }
    }


    private fun countScore() : Int{
        return 80
    }

    private fun initPress(binding: CycleFragmentMultiItemBinding) {
        binding.ivPressAim.visibility = View.INVISIBLE
    }

    private fun initLung(binding: CycleFragmentMultiItemBinding) {
        binding.ivLung.setImageResource(R.mipmap.icon_lung_border)
        binding.ivAim.visibility = View.INVISIBLE
        setQyRate(binding.chartQy, 0)
    }

    private fun startTime(binding: CycleFragmentMultiItemBinding) {
        if (isStart) {
            binding.ctTime.visibility = View.VISIBLE
            binding.ctTime.base = SystemClock.elapsedRealtime()
            binding.ctTime.start()
        }
    }

    private fun stopAllOutTime() {
        dataList.forEach {
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC)) {
                val b = getItemViewByMac(it.mac)
                b.ctTime.stop()
            }
        }

    }

    private fun stopOutTime(viewBinding:CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //暂停超时时间
        val isTime = isTimeOutMap[dataDTO.mac] ?: false
        if (isTime) {
            isTimeOutMap[dataDTO.mac] = false
            viewBinding.ctTime.visibility = View.INVISIBLE
            timeOutTotal += SystemClock.elapsedRealtime() - viewBinding.ctTime.base
            handler.removeCallbacks(counter)
            viewBinding.ctTime.base = SystemClock.elapsedRealtime()
            viewBinding.ctTime.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        dataList.forEach {
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC)) {
                val binding = getItemViewByMac(it.mac)
                setRate(binding.chart, 0)
                setQyRate(binding.chartQy, 0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        counter.let { headTimeHandler.removeCallbacks(it) }
        EventBus.getDefault().unregister(this)
    }
}