package com.pr.perfectrecovery.activity

import android.app.ProgressDialog
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
import java.math.RoundingMode
import java.text.DecimalFormat
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
    private var prLessCountMap = mutableMapOf<String, Int>()

    //按压多次
    private var prManyCountMap = mutableMapOf<String, Int>()

    //吹气少次
    private var qyLessCountMap = mutableMapOf<String, Int>()

    //吹气多次
    private var qyManyCountMap = mutableMapOf<String, Int>()

    //中断计时累加
    private var timeOutTotalMap = mutableMapOf<String, Long>()
    private var prValueMap = mutableMapOf<String, Int>()
    private var qyValueMap = mutableMapOf<String, Int>()

    //通气频率
    private var qyRateMap = mutableMapOf<String, Int>()
    private var err_pr_low_Map = mutableMapOf<String, Int>()
    private var err_pr_high_Map = mutableMapOf<String, Int>()
    private var err_pr_posi_Map = mutableMapOf<String, Int>()
    private var err_pr_unback_Map = mutableMapOf<String, Int>()
    private var err_qy_low_Map = mutableMapOf<String, Int>()
    private var err_qy_high_Map = mutableMapOf<String, Int>()
    private var err_qy_dead_Map = mutableMapOf<String, Int>()
    private var err_qy_close_Map = mutableMapOf<String, Int>()
    private var isTimeOutMap = mutableMapOf<String, Boolean>()
    private var isTimeingMap = mutableMapOf<String, Boolean>()

    /**处理循环次数- 以及考核 超次 少次 数据统计**/
    //当前是否为按压模式-吹气模式
    private var cyclePrCountMap = mutableMapOf<String, Int>()
    private var cycleQyCountMap = mutableMapOf<String, Int>()
    private var isPrMap = mutableMapOf<String, Boolean>()
    private var isQyMap = mutableMapOf<String, Boolean>()
    private var isQyAimMap = mutableMapOf<String, Boolean>()
    private var isCycleMap = mutableMapOf<String, Boolean>()
    private var startTimeMap = mutableMapOf<String, Long>()
    private var endTimeMap = mutableMapOf<String, Long>()
    private var resultBeanMap = mutableMapOf<String, TrainingDTO>()
    private var currentShowView: ConstraintLayout? = null
    private var curStudentIndex = 0

    private var bindingList = mutableListOf<CycleFragmentMultiItemBinding>()

    //是否已经结束
    private var isOver = false

    private val testMac = arrayOf(
        "001bacd17a75",
        "001bc8c59378",
        "001be814fe78",
        "001bffbd026f"
    )

    //记录考核完成状态，onResume时恢复仪表盘
    private var hasDoneMap = mutableMapOf<String, Boolean>()
    private var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val binding = msg.obj as CycleFragmentMultiItemBinding
            when (msg.what) {
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
        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_DO_BIND, "", null))
        showDialog()
    }

    var progressDialog: ProgressDialog? = null
    private fun showDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this@MutiActivityNew)
            progressDialog?.setTitle("提示")
            progressDialog?.setMessage("页面初始中，请稍后...")
        }
        if (!progressDialog!!.isShowing)
            progressDialog?.show()
    }

    private fun initView() {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        time = (configBean.operationTime * 1000).toLong()

        for (i in 0 until 6) {
            val item = BaseDataDTO()
            if (i < dataSize) {
                val bean = mTrainingBean?.list?.get(i)
//                item.mac = testMac[i]
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
        val timeDrawable =
            if (mTrainingBean?.isCheck == true) resources.getDrawable(R.mipmap.icon_wm_countdown) else resources.getDrawable(
                R.mipmap.icon_wm_time
            )
        binding.tvTime.setCompoundDrawablesWithIntrinsicBounds(
            timeDrawable,
            null,
            null,
            null
        )

        binding.oprLayout.ivBack.setOnClickListener {
            Log.e("hunger_test_clear", "ivBack: post message")
            EventBus.getDefault()
                .post(MessageEventData(BaseConstant.CLEAR_DEVICE_HISTORY_DATA, "", null))
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
                EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_DO_START, "", null))
                binding.oprLayout.ivStart.setBackgroundResource(R.drawable.drawable_chart_bg)
                binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_stop)
                binding.tvTime.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvModel.setTextColor(resources.getColor(R.color.color_37B48B))
                binding.tvCycle.setTextColor(resources.getColor(R.color.color_37B48B))
                counter.let { headTimeHandler.post(it) }
            } else {
                stop()
            }
        }
    }

    private fun initDeviceView() {
        dataList.forEach {
            Log.e("hunger_test", "initDeviceView: ${it.mac}")
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC)) {
                val item = getItemViewByMac(it.mac)
                initDeviceView(item)
                cycleCountMap[it.mac] = 0
            }
        }
    }

    private fun initDeviceView(view: CycleFragmentMultiItemBinding?) {
        view?.let {
            it.layoutPress.ivPress.visibility = View.INVISIBLE
            it.layoutPress.pressLayoutView.visibility = View.VISIBLE
            it.layoutPress.dashBoard.visibility = View.INVISIBLE
            it.layoutLung.dashBoard2.visibility = View.INVISIBLE
            it.layoutPress.chart.visibility = View.VISIBLE
            it.layoutLung.chartQy.visibility = View.VISIBLE
            setRate(it.layoutPress.chart, 0)
            setQyRate(it.layoutLung.chartQy, 0)
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

        bindingList.add(binding.item1)
        bindingList.add(binding.item2)
        bindingList.add(binding.item3)
        bindingList.add(binding.item4)
        bindingList.add(binding.item5)
        bindingList.add(binding.item6)
    }


    private fun setViewData(viewBinding: CycleFragmentMultiItemBinding?, dataDTO: BaseDataDTO?) {
        viewBinding?.let {
            dataDTO?.let {
                if (hasDoneMap[dataDTO.mac] == true) return
                mBaseDataDTOMap[dataDTO.mac] = dataDTO
                //中断超时
                if (isTimeOutMap[dataDTO.mac] != true && dataDTO.distance == dataDTO.preDistance && dataDTO.bpValue <= 0 && dataDTO.prSum > 0) {
                    isTimeOutMap[dataDTO.mac] = true
                    sendMsg(START_TIME, viewBinding, (configBean.interruptTime * 1000).toLong())
                }

                //第一次按压或吹气才开始计时
                if (startTimeMap[dataDTO.mac] ?: 0L <= 0 && (dataDTO.prSum != 0 || dataDTO.qySum != 0)) {
                    startTimeMap[dataDTO.mac] = System.currentTimeMillis()
                }
                showView(viewBinding, dataDTO)

                pr(viewBinding, dataDTO)
                qy(viewBinding, dataDTO)

                //计算循环次数
                cycle(dataDTO.mac)

                //更新循环次数
                if (prValueMap[dataDTO.mac] != dataDTO.prSum && isTimeingMap[dataDTO.mac] == true) {
                    isTimeingMap[dataDTO.mac] = false
                }
            }
        }
    }

    private fun showView(viewBinding: CycleFragmentMultiItemBinding, data: BaseDataDTO) {
        val isPress = abs(data.preDistance - data.distance) > 10
        val isBlow = data.bpValue > 5

        //没有按压 也没有吹气，显示上一次的视图
        if (!isPress && !isBlow) {
            currentShowView?.visibility = View.VISIBLE
        } else if (isPress) {
            viewBinding.layoutScore.root.visibility = View.GONE
            viewBinding.layoutLung.root.visibility = View.GONE
            viewBinding.layoutPress.root.visibility = View.VISIBLE
            currentShowView = viewBinding.layoutPress.root

        } else if (isBlow) {
            viewBinding.layoutPress.root.visibility = View.GONE
            viewBinding.layoutScore.root.visibility = View.GONE
            viewBinding.layoutLung.root.visibility = View.VISIBLE
            viewBinding.layoutLung.ivLung.setImageResource(R.mipmap.icon_lung_border)
            currentShowView = viewBinding.layoutLung.root

        }
    }

    private fun cycle(mac: String) {
        cycleEnd(mac)
        if (isQyMap[mac] == true && isPrMap[mac] != true && isCycleMap[mac] != true) {
            isCycleMap[mac] = true
            isQyMap[mac] = false
            isPrMap[mac] = false
            prMany(mac)
            val cycleCount = cycleCountMap[mac] ?: 0
            cycleCountMap[mac] = cycleCount + 1
        }
    }

    /**
     * 循环结束统计结果
     */
    private fun cycleEnd(mac: String) {
        if (isCheck) {
            if (cycleCountMap[mac] == configBean.cycles && cycleQyCountMap[mac] == configBean.qyCount) {
                /**
                 * 此处避免多次结算循环多次少次 -
                 * isCheck 只在当前页面不影响
                 */
                qyMany(mac)
                hasDoneMap[mac] = true
                endTimeMap[mac] = System.currentTimeMillis()
                showSingleResult(mac)
            }
        }
    }

    private fun prMany(mac: String) {
        if (isCheck) {
            val cyclePrCount = cyclePrCountMap[mac] ?: 0
            if (cyclePrCount > configBean.prCount && cyclePrCount > 0) {
                //按压超次
                val prManyCount = prManyCountMap[mac] ?: 0
                prManyCountMap[mac] = cyclePrCount - configBean.prCount + prManyCount
            } else if (cyclePrCount < configBean.prCount) {
                //按压少次
                val prLessCount = prLessCountMap[mac] ?: 0
                prLessCountMap[mac] = configBean.prCount - cyclePrCount + prLessCount
            }
            cyclePrCountMap[mac] = 0
        }
    }

    private fun pr(viewBinding: CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        setRate(viewBinding.layoutPress.chart, dataDTO.pf)
        viewBinding.layoutPress.pressLayoutView.smoothScrollTo(dataDTO.distance, dataDTO)
        //处理是否按压
        if (prValueMap[dataDTO.mac] ?: 0 != dataDTO.prSum) {
            prValueMap[dataDTO.mac] = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
            stopOutTime(viewBinding, dataDTO.mac)
            qyMany(dataDTO.mac)
            var cout = cyclePrCountMap[dataDTO.mac] ?: 0
            cout++
            cyclePrCountMap[dataDTO.mac] = cout
            cycleQyCountMap[dataDTO.mac] = 0
            isPrMap[dataDTO.mac] = true
            isQyMap[dataDTO.mac] = false
            isCycleMap[dataDTO.mac] = false
            //按压位置错误
            if (err_pr_posi_Map[dataDTO.mac] != dataDTO.err_pr_posi && dataDTO.psrType == 0) {
                err_pr_posi_Map[dataDTO.mac] = dataDTO.err_pr_posi
                viewBinding.layoutPress.ivPressAim.visibility = View.VISIBLE
                sendMsg(INIT_PRESS, viewBinding)
            } else if (err_pr_unback_Map[dataDTO.mac] ?: 0 != dataDTO.err_pr_unback) {
                //按压未回弹
                err_pr_unback_Map[dataDTO.mac] = dataDTO.err_pr_unback
                viewBinding.layoutPress.pressLayoutView.setUnBack()
            } else {
                if (err_pr_low_Map[dataDTO.mac] ?: 0 != dataDTO.err_pr_low) {//按压不足
                    err_pr_low_Map[dataDTO.mac] = dataDTO.err_pr_low
                    viewBinding.layoutPress.pressLayoutView.setDown()
                } else if (err_pr_high_Map[dataDTO.mac] != dataDTO.err_pr_high) {//按压过大
                    err_pr_high_Map[dataDTO.mac] = dataDTO.err_pr_high
                }
            }
        }
        //按压错误数统计
        viewBinding.layoutPress.tvPress.text = "${dataDTO.getPr_err_total()}"
        //按压总数
        viewBinding.layoutPress.tvPressTotal.text = "/${dataDTO.prSum}"
    }

    private var qyManyCycleMap = mutableMapOf<String, Int>()
    private fun qyMany(mac: String) {
        val cycleQyCount = cycleQyCountMap[mac] ?: 0
        if (isCheck && cycleCountMap[mac] != qyManyCycleMap[mac] && cycleQyCount > 0) {
            qyManyCycleMap[mac] = cycleCountMap[mac] ?: 0
            if (cycleQyCount > configBean.qyCount) {
                //吹气超次
                val qyManyCount = qyManyCountMap[mac] ?: 0
                qyManyCountMap[mac] = cycleQyCount - configBean.qyCount + qyManyCount
                Log.e("qyManyCount 吹气超次", "qyManyCount: $qyManyCount")
            } else if (cycleQyCount < configBean.qyCount) {
                //吹气少次
                val qyLessCount = qyLessCountMap[mac] ?: 0
                qyLessCountMap[mac] = configBean.qyCount - cycleQyCount + qyLessCount
                Log.e("qyLessCount 吹气少次", "qyLessCount: $qyLessCount")
            }
            cycleQyCountMap[mac] = 0
        }
    }


    /**
     * 吹气状态
     */
    private fun qy(viewBinding: CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1) {
            viewBinding.layoutLung.ivAim.visibility = View.INVISIBLE
            if (qyValueMap[dataDTO.mac] ?: 0 != dataDTO.qySum) {
                stopOutTime(viewBinding, dataDTO.mac)
                var cout = cycleQyCountMap[dataDTO.mac] ?: 0
                cout++
                cycleQyCountMap[dataDTO.mac] = cout
                isQyMap[dataDTO.mac] = true
                isPrMap[dataDTO.mac] = false

                val qyMax = dataDTO.qyMaxValue
                when {
                    qyMax < configBean.tidalVolume -> {
                        err_qy_low_Map[dataDTO.mac] = dataDTO.err_qy_low
                        viewBinding.layoutLung.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                    }
                    qyMax in configBean.tidalVolumeEnd..1199 -> {
                        err_qy_high_Map[dataDTO.mac] = dataDTO.err_qy_high
                        viewBinding.layoutLung.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                    }
                    qyMax >= 1200 -> {
                        err_qy_dead_Map[dataDTO.mac] = dataDTO.err_qy_dead
                        viewBinding.layoutLung.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                    }
                    else -> {
                        viewBinding.layoutLung.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                }
                //吹气变灰
                sendMsg(INIT_LUNG, viewBinding)
            }
        } else {
            if (dataDTO.err_qy_close != err_qy_close_Map[dataDTO.mac]) {
                err_qy_close_Map[dataDTO.mac] = dataDTO.err_qy_close
                stopOutTime(viewBinding, dataDTO.mac)
                viewBinding.layoutLung.ivAim.visibility = View.VISIBLE
                isQyAimMap[dataDTO.mac] = true
                var cout = cycleQyCountMap[dataDTO.mac] ?: 0
                cout++
                cycleQyCountMap[dataDTO.mac] = cout
                isQyMap[dataDTO.mac] = true
                isPrMap[dataDTO.mac] = false
                sendMsg(INIT_LUNG, viewBinding)
            }
        }
        qyValueMap[dataDTO.mac] = dataDTO.qySum
        //吹气频率
        setQyRate(viewBinding.layoutLung.chartQy, dataDTO.cf)
        //吹气错误数统计
        viewBinding.layoutLung.tvLungError.text = "${(dataDTO.getQy_err_total())}"
        viewBinding.layoutLung.tvLungTotal.text = "/${dataDTO.qySum}"

        //清空吹气图标
        if ((viewBinding.layoutLung.ivAim.isShown && isQyAimMap[dataDTO.mac] == true) || (dataDTO.bpValue <= 0 && qyRateMap[dataDTO.mac] ?: 0 > 0)) {
            qyRateMap[dataDTO.mac] = 0//用于清空数据
            isQyAimMap[dataDTO.mac] = false
            sendMsg(INIT_LUNG, viewBinding)
        } else {
            if (qyRateMap[dataDTO.mac] == 0 && dataDTO.bpValue > 0) {
                qyRateMap[dataDTO.mac] = dataDTO.bpValue
            }
        }
    }


    private fun getItemViewByMac(mac: String): CycleFragmentMultiItemBinding? {
        var index = -1
        for (i in 0 until dataList.size) {
            if (TextUtils.equals(mac, dataList[i].mac))
                index = i
        }
        val view = when (index) {
            0 -> binding.item1
            1 -> binding.item2
            2 -> binding.item3
            3 -> binding.item4
            4 -> binding.item5
            5 -> binding.item6
            else -> {}
        }
        return if (view is CycleFragmentMultiItemBinding) view else null
    }

    private fun getMacByItemView(binding: CycleFragmentMultiItemBinding): String {
        val index = bindingList.indexOf(binding)
        return dataList[index].mac
    }

    private fun observeData() {
        StatusLiveData.dataSingle.observe(this, Observer {
            it?.let {
                Log.e("hunger_test", "printData: mac: ${it.mac}, distance: ${it.distance}")
                val view = getItemViewByMac(it.mac)
                if (isStart && hasDoneMap[it.mac] != true)
                    setViewData(view, it)
            }
        })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: MessageEventData) {
        when (event.code) {
            BaseConstant.DEVICE_DISCONNECTED -> {
                // 断连置灰
                val mac = initMac(event.cycleCount)
                val bind = getItemViewByMac(mac)
                bind?.let {
                    bind.layoutPress.root.visibility = View.VISIBLE
                    bind.layoutLung.root.visibility = View.GONE
                    bind.layoutScore.root.visibility = View.GONE
                    bind.layoutPress.dashBoard.visibility = View.VISIBLE
                    bind.layoutPress.ivPress.visibility = View.VISIBLE
                    bind.layoutPress.chart.visibility = View.INVISIBLE
                    bind.layoutPress.pressLayoutView.visibility = View.INVISIBLE
                }
            }
            BaseConstant.EVENT_CANCEL_DIALOG -> {
                if (progressDialog != null && progressDialog!!.isShowing) {
                    progressDialog?.cancel()
                }
            }
        }
    }

    private fun initMac(mac: String): String {
        var new = mac.replace(":", "")
        return new.lowercase()
    }

    //不让用户返回，只能点下面的返回按钮
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("确认结束测试并退出？")
            .setPositiveButton("确定") { dialog, _ ->
                Log.e("hunger_test_clear", "dialog : post message")
                EventBus.getDefault()
                    .post(MessageEventData(BaseConstant.CLEAR_DEVICE_HISTORY_DATA, "", null))
                dialog.cancel()
                finish()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private inner class Counter : Runnable {
        override fun run() {
            headTimeHandler.postDelayed(this, 1000)//一秒钟循环计时一次
            if (time <= 0) {
                stop()
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

    fun updateCycleData(time: Long) {
        if (time % BaseConstant.INTERVAL_TIME == 0L) {
            curStudentIndex %= dataSize
            mTrainingBean?.list?.get(curStudentIndex)?.let {
                binding.tvModel.text = "${it.count}"
                binding.tvCycle.text = "${cycleCountMap[it.mac] ?: 0}"
                binding.tvBattery.power = mBaseDataDTOMap[it.mac]?.electricity ?: 0
            }
            curStudentIndex++
        }
    }

    private fun setRate(view: DialChart07View, value: Int) {
        var pf = 0f
        if (value > 0) {
            when {
                value < configBean.depthFrequency -> {
                    pf = (0.33f / configBean.depthFrequency) * value
                }
                value in configBean.depthFrequency..configBean.depthFrequencyEnd -> {
                    pf =
                        (0.33f / (configBean.depthFrequencyEnd - configBean.depthFrequency) * (value - configBean.depthFrequency) + 0.33f)
                }
                value > configBean.depthFrequencyEnd -> {
                    pf =
                        (0.33f / (200 - configBean.depthFrequencyEnd) * (value - configBean.depthFrequencyEnd) + 0.66f)
                }
            }
        }
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    private fun setQyRate(view: DialChart07View, value: Int) {
        var pf = 0f
        if (value > 0) {
            when {
                value < configBean.tidalFrequency -> {
                    pf = (0.33f / configBean.tidalFrequency) * value
                }
                value in configBean.tidalFrequency..configBean.tidalFrequencyEnd -> {
                    pf =
                        (0.33f / (configBean.tidalFrequencyEnd - configBean.tidalFrequency) * (value - configBean.tidalFrequency) + 0.33f)
                }
                value > configBean.tidalFrequencyEnd -> {
                    pf =
                        (0.33f / (60 - configBean.tidalFrequencyEnd) * (value - configBean.tidalFrequencyEnd) + 0.66f)
                }
            }
        }
        Log.e("setQyRate", "Rate: $pf")
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    private fun stop() {
        stopAllOutTime()
        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
        binding.oprLayout.ivStart.setBackgroundResource(R.drawable.start_play_hight)
        binding.oprLayout.ivStart.setImageResource(R.mipmap.icon_wm_start_white)
        counter.let { headTimeHandler.removeCallbacks(it) }
        showAllResult()
        isOver = true
    }

    private fun sendMsg(type: Int, binding: CycleFragmentMultiItemBinding, delayTime: Long = 2000) {
        handler.removeMessages(type, binding)
        var msg = Message()
        msg.what = type
        msg.obj = binding
        handler.sendMessageDelayed(msg, delayTime)
    }

    private fun showAllResult() {
        dataList.forEach { data ->
            showSingleResult(data.mac)
        }
    }

    private fun showSingleResult(mac: String) {
        if (!TextUtils.equals(mac, BaseConstant.FAKE_MAC)) {
            if (isCheck) {
                showCheckScore(mac)
            } else {
                showTestResult(mac)
            }
        }
    }

    private fun showCheckScore(mac: String) {
        val item = getItemViewByMac(mac)
        item?.let {
            item.layoutLung.root.visibility = View.GONE
            item.layoutPress.root.visibility = View.GONE
            item.layoutTest.root.visibility = View.GONE
            item.layoutScore.root.visibility = View.VISIBLE
            val score = countScore(mac)
            var ratingBar: RatingBar = item.layoutScore.ratingBar
            when (score) {
                in 0f..40f -> {
                    item.layoutScore.ratingBarRed.visibility = View.VISIBLE
                    item.layoutScore.ratingBar.visibility = View.GONE
                    item.layoutScore.ratingBarYellow.visibility = View.GONE
                    ratingBar = item.layoutScore.ratingBarRed
                }
                in 40f..80f -> {
                    item.layoutScore.ratingBarYellow.visibility = View.VISIBLE
                    item.layoutScore.ratingBar.visibility = View.GONE
                    item.layoutScore.ratingBarRed.visibility = View.GONE
                    ratingBar = item.layoutScore.ratingBarYellow
                }
                in 80f..100f -> {
                    item.layoutScore.ratingBar.visibility = View.VISIBLE
                    item.layoutScore.ratingBarRed.visibility = View.GONE
                    item.layoutScore.ratingBarYellow.visibility = View.GONE
                    ratingBar = item.layoutScore.ratingBar
                }
            }
            item.layoutScore.tvScore.text = "${if (score > 0) getNoMoreThanTwoDigits(score) else 0}"
            ratingBar.rating = (5.0 * score / 100).toFloat()

            item.layoutScore.root.setOnClickListener {
                gotoDetail(mac)
            }
        }
    }

    private fun showTestResult(mac: String) {
        val item = getItemViewByMac(mac)
        item?.let {
            item.layoutLung.root.visibility = View.GONE
            item.layoutPress.root.visibility = View.GONE
            item.layoutScore.root.visibility = View.GONE
            item.layoutTest.root.visibility = View.VISIBLE
            item.layoutTest.testPressError.text =
                "${if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.getPr_err_total() else 0}"
            item.layoutTest.testPresTotal.text =
                "/${if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.prSum else 0}"
            item.layoutTest.testCycleIcon.text = "${cycleCountMap[mac] ?: 0}"
            item.layoutTest.testCycleTotal.text = "${cycleCountMap[mac] ?: 0}"
            item.layoutTest.testLungError.text =
                "${if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.getQy_err_total() else 0}"
            item.layoutTest.testLungTotal.text =
                "/${if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qySum else 0}"

            item.layoutTest.root.setOnClickListener {
                gotoDetail(mac)
            }
        }
    }

    private fun gotoDetail(mac: String) {
        val mTrainingDTO = getResultBean(mac)
        TrainResultActivity.start(this, mTrainingDTO, multi = true)
    }

    private fun getResultBean(mac: String): TrainingDTO {
        if (resultBeanMap[mac] != null) return resultBeanMap[mac]!!
        val mTrainingDTO = TrainingDTO()
        mTrainingDTO.isCheck = mTrainingBean!!.isCheck
        mTrainingBean?.list?.forEach {
            if (TextUtils.equals(it.mac, mac))
                mTrainingDTO.name = it.name
        }

        mTrainingDTO.check1 = false
        mTrainingDTO.check2 = false
        mTrainingDTO.check3 = false
        mTrainingDTO.check4 = false
        mTrainingDTO.check5 = false
        mTrainingDTO.check6 = false
        mTrainingDTO.check7 = false
        mTrainingDTO.check8 = false
        mTrainingDTO.check9 = false
        mTrainingDTO.check10 = false

        mTrainingDTO.startTime = startTimeMap[mac] ?: 0
        mTrainingDTO.endTime = endTimeMap[mac] ?: 0
        mTrainingDTO.operateTime = mTrainingDTO.endTime - mTrainingDTO.startTime
        mTrainingDTO.timeOutTotal = timeOutTotalMap[mac] ?: 0
        mTrainingDTO.err_pr_high = err_pr_high_Map[mac] ?: 0
        mTrainingDTO.err_pr_low = err_pr_low_Map[mac] ?: 0
        mTrainingDTO.err_pr_posi = err_pr_posi_Map[mac] ?: 0
        mTrainingDTO.err_pr_unback = err_pr_unback_Map[mac] ?: 0
        //按压总错误数
        mTrainingDTO.pressErrorCount =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.getPr_err_total() else 0

        mTrainingDTO.err_qy_high = err_qy_high_Map[mac] ?: 0
        mTrainingDTO.err_qy_low = err_qy_low_Map[mac] ?: 0
        mTrainingDTO.err_qy_dead = err_qy_dead_Map[mac] ?: 0
        mTrainingDTO.err_qy_close = err_qy_close_Map[mac] ?: 0

        //吹气总错误数
        mTrainingDTO.blowErrorCount =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.getQy_err_total()
                .toFloat() else 0f

        mTrainingDTO.prSum = if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.prSum else 0
        mTrainingDTO.qySum = if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qySum else 0

        mTrainingDTO.pr_depth_sum =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.pr_depth_sum else 0
        mTrainingDTO.pr_time_sum =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.pr_time_sum else 0
        mTrainingDTO.qy_volume_sum =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qy_volume_sum else 0
        mTrainingDTO.qy_time_sum =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qy_time_sum else 0
        mTrainingDTO.pr_seqright_total =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.pr_seqright_total else 0
        mTrainingDTO.qy_serright_total =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qy_serright_total else 0
        mTrainingDTO.qy_max_volume_sum =
            if (mBaseDataDTOMap[mac] != null) mBaseDataDTOMap[mac]!!.qy_max_volume_sum else 0

        //超次少次
        mTrainingDTO.prManyCount = prManyCountMap[mac] ?: 0
        mTrainingDTO.prLessCount = prLessCountMap[mac] ?: 0
        mTrainingDTO.qyManyCount = qyManyCountMap[mac] ?: 0
        mTrainingDTO.qyLessCount = qyLessCountMap[mac] ?: 0
        mTrainingDTO.cycleCount = cycleCountMap[mac] ?: 0

        mTrainingDTO.timeTotal = (configBean.operationTime * 1000).toLong()
        mTrainingDTO.prCount = configBean.prCount
        mTrainingDTO.qyCount = configBean.qyCount
        mTrainingDTO.cycles = configBean.cycles
        mTrainingDTO.pressScore = configBean.pressScore.toFloat()
        mTrainingDTO.blowScore = configBean.blowScore.toFloat()
        mTrainingDTO.processScore = configBean.processScore.toFloat()
        mTrainingDTO.deduction = configBean.deductionScore
        resultBeanMap[mac] = mTrainingDTO
        return mTrainingDTO
    }

    private fun countScore(mac: String): Float {
        val trainingDTO = getResultBean(mac)
        var scoreTotal: Float =
            trainingDTO.getQyScore() + trainingDTO.getPrScore()
        if (scoreTotal > trainingDTO.getTimeOutScore()) {
            scoreTotal -= trainingDTO.getTimeOutScore()
        } else {
            scoreTotal = 0f
        }
        return scoreTotal
    }

    private fun initPress(binding: CycleFragmentMultiItemBinding) {
        binding.layoutPress.ivPressAim.visibility = View.INVISIBLE
    }

    private fun initLung(binding: CycleFragmentMultiItemBinding) {
        binding.layoutLung.ivLung.setImageResource(R.mipmap.icon_lung_border)
        binding.layoutLung.ivAim.visibility = View.INVISIBLE
        setQyRate(binding.layoutLung.chartQy, 0)
    }

    private fun startTime(binding: CycleFragmentMultiItemBinding) {
        if (isStart) {
            val mac = getMacByItemView(binding)
            var totalTime = timeOutTotalMap[mac] ?: 0
            binding.layoutPress.ctPressTime.setOnChronometerTickListener {
                //SystemClock.elapsedRealtime()系统当前时间
                //chronometer.getBase()记录计时器开始时的时间
                if ((SystemClock.elapsedRealtime() - binding.layoutPress.ctPressTime.base) >= 1000) {
                    totalTime += 1000
                    timeOutTotalMap[mac] = totalTime
                }
            }
            binding.layoutLung.ctLungTime.setOnChronometerTickListener {
                //SystemClock.elapsedRealtime()系统当前时间
                //chronometer.getBase()记录计时器开始时的时间
                if ((SystemClock.elapsedRealtime() - binding.layoutLung.ctLungTime.base) >= 1000) {
                    totalTime += 1000
                    timeOutTotalMap[mac] = totalTime
                }
            }
            if (isPrMap[mac] == true) {
                binding.layoutPress.ctPressTime.visibility = View.VISIBLE
                binding.layoutPress.ctPressTime.base = SystemClock.elapsedRealtime()
                binding.layoutPress.ctPressTime.start()
            } else if (isQyMap[mac] == true) {
                binding.layoutLung.ctLungTime.visibility = View.VISIBLE
                binding.layoutLung.ctLungTime.base = SystemClock.elapsedRealtime()
                binding.layoutLung.ctLungTime.start()
            }
        }
    }

    private fun stopAllOutTime() {
        val endTime = System.currentTimeMillis()
        dataList.forEach {
            //点击停止键，所有为完成都都标记为完成并记录结束时间
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC) && hasDoneMap[it.mac] != true) {
                endTimeMap[it.mac] = endTime
                val b = getItemViewByMac(it.mac)
                if (isPrMap[it.mac] == true) {
                    b?.layoutPress?.ctPressTime?.stop()
                } else if (isQyMap[it.mac] == true) {
                    b?.layoutLung?.ctLungTime?.stop()
                }


                qyMany(it.mac)

                val cycleCount = cycleCountMap[it.mac] ?: 0
                val cyclePrCount = cyclePrCountMap[it.mac] ?: 0
                if (cycleCount < configBean.cycles) {
                    val number = configBean.cycles - cycleCount
                    var prLessCount = prLessCountMap[it.mac] ?: 0
                    if (number > 0 && cyclePrCount > 0) {
                        prLessCount += (number - 1) * configBean.prCount
                        prMany(it.mac)
                    } else {
                        prLessCount += number * configBean.prCount
                    }
                    prLessCountMap[it.mac] = prLessCount
                    val qyLessCount = qyLessCountMap[it.mac] ?: 0
                    qyLessCountMap[it.mac] = number * configBean.qyCount + qyLessCount
                }
            }
        }
    }

    private fun stopOutTime(viewBinding: CycleFragmentMultiItemBinding, mac: String) {
        //暂停超时时间
        if (isTimeOutMap[mac] == true) {
            isTimeOutMap[mac] = false
            handler.removeCallbacks(counter)
            if (isPrMap[mac] == true) {
                viewBinding.layoutPress.ctPressTime.visibility = View.INVISIBLE
                viewBinding.layoutPress.ctPressTime.base = SystemClock.elapsedRealtime()
                viewBinding.layoutPress.ctPressTime.stop()
            } else if (isQyMap[mac] == true) {
                viewBinding.layoutLung.ctLungTime.visibility = View.INVISIBLE
                viewBinding.layoutLung.ctLungTime.base = SystemClock.elapsedRealtime()
                viewBinding.layoutLung.ctLungTime.stop()
            }
        }
    }

    private fun getNoMoreThanTwoDigits(number: Float): String {
        val format = DecimalFormat("0.#")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(number)
    }

    override fun onResume() {
        super.onResume()
        dataList.forEach {
            if (!TextUtils.equals(it.mac, BaseConstant.FAKE_MAC)) {
                val binding = getItemViewByMac(it.mac)
                binding?.let {
                    setRate(binding.layoutPress.chart, 0)
                    setQyRate(binding.layoutLung.chartQy, 0)
                }
            }
        }
    }

    override fun onDestroy() {
        clearMap()
        counter.let { headTimeHandler.removeCallbacks(it) }
        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_BLE_CLOSE, "", null))
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun clearMap() {
        mBaseDataDTOMap.clear()
        cycleCountMap.clear()
        prLessCountMap.clear()
        prManyCountMap.clear()
        qyLessCountMap.clear()
        qyManyCountMap.clear()
        timeOutTotalMap.clear()
        prValueMap.clear()
        qyValueMap.clear()
        qyRateMap.clear()
        err_pr_low_Map.clear()
        err_pr_high_Map.clear()
        err_pr_posi_Map.clear()
        err_pr_unback_Map.clear()
        err_qy_low_Map.clear()
        err_qy_high_Map.clear()
        err_qy_dead_Map.clear()
        err_qy_close_Map.clear()
        isTimeOutMap.clear()
        isTimeingMap.clear()
        cyclePrCountMap.clear()
        cycleQyCountMap.clear()
        isPrMap.clear()
        isQyMap.clear()
        isQyAimMap.clear()
        isCycleMap.clear()
        startTimeMap.clear()
        endTimeMap.clear()
        resultBeanMap.clear()
    }
}