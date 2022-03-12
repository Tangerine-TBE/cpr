package com.pr.perfectrecovery.fragment

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.CycleFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CycleViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.view.DialChart07View
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

/**
 * CPR按压页
 */
class CycleFragment : Fragment() {
    private lateinit var viewBinding: CycleFragmentBinding
    private var mMediaPlayer: MediaPlayer? = null
    private var isTS: Boolean = false
    private var isYY: Boolean = false
    private var isCheck: Boolean = false
    private var cycleCount = 0
    private var mBaseDataDTO: BaseDataDTO? = null
    private var isStart = false
    private var isTimeing = true
    private var configBean = ConfigBean()

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

    //按压总数统计

    companion object {
        fun newInstance(isTS: Boolean, isYY: Boolean, isCheck: Boolean) = CycleFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_PARAM1, isTS)
                putBoolean(ARG_PARAM2, isYY)
                putBoolean(ARG_PARAM3, isCheck)
            }
        }
    }

    private lateinit var viewModel: CycleViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = CycleFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.let {
            isTS = it.getBoolean(ARG_PARAM1)
            isYY = it.getBoolean(ARG_PARAM2)
            isCheck = it.getBoolean(ARG_PARAM3)
        }
        viewModel = ViewModelProvider(this).get(CycleViewModel::class.java)
        initView()
    }

    private fun initView() {
        alphaAnimation()
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
//        DataVolatile.PR_HIGH_VALUE = configBean.prHigh()
//        DataVolatile.PR_LOW_VALUE = configBean.prLow()
        //按压通气比列
        StatusLiveData.data.observe(requireActivity(), Observer {
            if (it != null) {
                setViewDate(it)
                viewBinding.tvPress3.text = "距离值：${it.distance}"
                viewBinding.tvPress4.text = "气压值：${it.bpValue}"
                viewBinding.tvPress5.text = "按压频率：${it.pf}"
                viewBinding.tvPress6.text = "吹气频率：${it.cf}"
                viewBinding.tvPress7.text = "气道状态：${it.aisleType}"
                viewBinding.tvPress8.text = "按压位置：${it.psrType}"
                viewBinding.tvPress9.text = "初始值：${it.preDistance}"
                viewBinding.tvPress10.text = "按压深度：${abs(it.preDistance - it.distance)}"
            }
        })

        //事件监听器，时间发生变化时可进行操作
        viewBinding.ctTime.setOnChronometerTickListener {
            //SystemClock.elapsedRealtime()系统当前时间
            //chronometer.getBase()记录计时器开始时的时间
            if ((SystemClock.elapsedRealtime() - viewBinding.ctTime.base) >= 1000) {
                timeOutTotal += 1000
                Log.e("elapsedRealtime", "initView: $timeOutTotal")
            }
        }
    }

    private val VOICE_MP3_BGM: Int = 1//节奏音乐
    private val VOICE_MP3_AYBZ: Int = 2//按压不足
    private val VOICE_MP3_AYGD: Int = 3//按压过大
    private val VOICE_MP3_AYWZCW: Int = 4//按压位置错误
    private val VOICE_MP3_CQBZ: Int = 5//吹气不足
    private val VOICE_MP3_CQGD: Int = 6//吹气过大
    private val VOICE_MP3_CQJW: Int = 7//吹气进胃
    private val VOICE_MP3_WDKQD: Int = 8//未打开气道
    private val VOICE_MP3_WHT: Int = 9//未回弹
    private val VOICE_MP3_DIS: Int = 10//蓝牙断开连接

    private var mpType = -1
    private var isPlay = false

    private fun setPlayVoice(type: Int) {
        if (isTS && !isPlay) {
            if (mMediaPlayer != null) {
                mMediaPlayer?.reset()
            }
            isPlay = true
            mpType = -1
            when (type) {
                VOICE_MP3_AYBZ -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_aybz)
                }
                VOICE_MP3_AYGD -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_aygd)
                }
                VOICE_MP3_AYWZCW -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_aywzcw)
                }
                VOICE_MP3_CQBZ -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_cqbz)
                }
                VOICE_MP3_CQGD -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_cqgd)
                }
                VOICE_MP3_CQJW -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_cqjw)
                }
                VOICE_MP3_WDKQD -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_wdkqd)
                }
                VOICE_MP3_WHT -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_wht)
                }
                VOICE_MP3_DIS -> {
                    mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_blueooth)
                }
            }
            //如果是其他MP3播完后播节奏
            mMediaPlayer?.setOnCompletionListener {
                isPlay = false
                if (mpType > 0) {
                    setPlayVoice(mpType)
                } else {
                    startMP3()
                }
                mpType = -1
            }
            mMediaPlayer?.start()
        } else {
            mpType = type
        }
    }

    fun start() {
        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_START, "", null))
        viewBinding.ivPress.setImageResource(R.mipmap.icon_wm_normal)
        viewBinding.ivLung.setImageResource(R.mipmap.icon_lung_border)
        viewBinding.dashBoard.setImageResource(R.mipmap.icon_wm_bp_2)
        viewBinding.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_2)
        viewBinding.ivPress.visibility = View.INVISIBLE
        viewBinding.pressLayoutView.visibility = View.VISIBLE
        viewBinding.dashBoard.visibility = View.INVISIBLE
        viewBinding.dashBoard2.visibility = View.INVISIBLE
        viewBinding.chart.visibility = View.VISIBLE
        viewBinding.chartQy.visibility = View.VISIBLE
        isStart = true
        startMP3()
    }

    fun stop(): TrainingDTO {
        //返回成绩结果类
        endTime = System.currentTimeMillis()
        isStart = false
        val trainingDTO = TrainingDTO()
        mBaseDataDTO?.apply {
            trainingDTO.startTime = startTime
            trainingDTO.endTime = endTime
            trainingDTO.pressOutTime = timeOutTotal
            trainingDTO.pressHigh = err_pr_high
            trainingDTO.pressLow = err_pr_low
            trainingDTO.pressLocation = err_pr_posi
            trainingDTO.pressRebound = err_pr_unback.toFloat()
            //按压总错误数
            trainingDTO.pressErrorCount = mBaseDataDTO!!.getPr_err_total().toFloat()
            trainingDTO.blowHigh = err_qy_high
            trainingDTO.blowLow = err_qy_low
            trainingDTO.blowIntoStomach = err_qy_dead
            trainingDTO.err_qy_close = err_qy_close
            //吹气总错误数
            trainingDTO.blowErrorCount = mBaseDataDTO!!.getQy_err_total().toFloat()
            trainingDTO.prSum = prSum.toFloat()
            trainingDTO.qySum = qySum
            //超次少次
            trainingDTO.prManyCount = prManyCount
            trainingDTO.prLessCount = prLessCount
            trainingDTO.qyManyCount = qyManyCount
            trainingDTO.qyLessCount = qyLessCount

            trainingDTO.pr_depth_sum = mBaseDataDTO!!.pr_depth_sum
            trainingDTO.pr_time_sum = mBaseDataDTO!!.pr_time_sum.toFloat()
            trainingDTO.qy_volume_sum = mBaseDataDTO!!.qy_volume_sum
            trainingDTO.qy_time_sum = mBaseDataDTO!!.qy_time_sum.toFloat()
            trainingDTO.pr_seqright_total = mBaseDataDTO!!.pr_seqright_total
            trainingDTO.qy_serright_total = mBaseDataDTO!!.qy_serright_total
            trainingDTO.qy_max_volume_sum = mBaseDataDTO!!.qy_max_volume_sum
        }
        mHandler.removeCallbacks(counter)
        viewBinding.ctTime.stop()
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
            mMediaPlayer = null
        }
        return trainingDTO
    }

    fun bluetoothDisconnected() {
        setPlayVoice(VOICE_MP3_DIS)
        val dialog: androidx.appcompat.app.AlertDialog =
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("提示")
                .setMessage("当前蓝牙已断开！")
                .setPositiveButton(
                    "确认"
                ) { arg0, arg1 ->
                    activity?.finish()
                }.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    /**
     * 播放节奏音乐
     */
    private fun startMP3() {
        if (isYY && isStart) {//节奏音
            mMediaPlayer = MediaPlayer.create(activity, R.raw.wm_bg)
            mMediaPlayer?.isLooping = true
            mMediaPlayer?.seekTo(0)
//            mMediaPlayer?.prepareAsync()
            mMediaPlayer?.setOnPreparedListener { // 装载完毕回调
                mMediaPlayer?.start()
            }
        }
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) {}
    private val mHandler1 = object : Handler(Looper.getMainLooper()) {}
    private val mHandler2 = object : Handler(Looper.getMainLooper()) {}
    private val mHandler3 = object : Handler(Looper.getMainLooper()) {}
    private val mHandler4 = object : Handler(Looper.getMainLooper()) {}
    private val mHandler5 = object : Handler(Looper.getMainLooper()) {}
    private val mHandler6 = object : Handler(Looper.getMainLooper()) {}

    private val counter = Runnable {
        isTimeOut = true
        viewBinding.ctTime.visibility = View.VISIBLE
        //记录一次按压超时
        viewBinding.ctTime.base = SystemClock.elapsedRealtime()
        viewBinding.ctTime.start()
    }

    private var prValue = 0
    private var qyValue = 0

    //通气频率
    private var qyRate = 0
    private var err_pr_low = 0
    private var err_pr_high = 0
    private var err_pr_posi = 0
    private var err_qr_unback = 0
    private var isTimeOut = false

    /**处理循环次数- 以及考核 超次 少次 数据统计**/
    //当前是否为按压模式-吹气模式
    private var cyclePrCount = 0
    private var cycleQyCount = 0
    private var isPr = false
    private var isQy = false
    private var isQyAim = false

    private var startTime: Long = 0
    private var endTime: Long = 0

    //按压切换到吹气，算一个循环
    private fun setViewDate(dataDTO: BaseDataDTO?) {
        if (dataDTO != null) {
            mBaseDataDTO = dataDTO
            //中断超时
            if (!isTimeOut && dataDTO.distance == dataDTO.preDistance
                && dataDTO.bpValue <= 0 && dataDTO.prSum > 0
            ) {
                isTimeOut = true
                mHandler.removeCallbacksAndMessages(null)
                mHandler.postDelayed(counter, (configBean.interruptTime * 1000).toLong())
            }

            //第一次按压或吹气才开始计时
            if (startTime <= 0 && (dataDTO.prSum != 0 || dataDTO.qySum != 0)) {
                startTime = System.currentTimeMillis()
            }

            if ((dataDTO.preDistance - dataDTO.distance) < 15 && dataDTO.bpValue > 0) {
                //吹气
                qy(dataDTO)
            } else {
                //按压
                pr(dataDTO)
                //吹气
//            qy(dataDTO)
            }
            //清空吹气频率
            if (dataDTO.bpValue <= 0 && qyRate > 0) {
                //吹气频率清零
                qyRate = 0//用于清空数据
                mHandler2.removeCallbacks(runnableCF)
                mHandler2.postDelayed(runnableCF, 10000)
            } else {
                if (qyRate == 0 && dataDTO.bpValue > 0) {
                    qyRate = dataDTO.bpValue
                }
            }
            //清空吹气图标
//            if (viewBinding.ivAim.isShown && isQyAim) {
//                isQyAim = false
//                mHandler4.removeCallbacksAndMessages(null)
//                mHandler4.postDelayed(this::setQyAimVisibility, 2000)
//            }
            //计算循环次数
            cycle(dataDTO)
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

    /**
     * 循环次数计算
    吹气在整个流程的开头或中间：
    检测到有按压，一个循环结束。这时候就能统计出这个循环的吹气次数，然后和设定的吹气次数比较，判断有没有超次少次

    吹气在整个流程的结尾：
    达到设定的次数结束整个流程，没有达到设定的次数，等待继续吹，如果一直没有吹，一直等到流程的时间用完，这个时候算出这个循环少次
     */
    private var isCycle: Boolean = false
    private fun cycle(dataDTO: BaseDataDTO) {
//        if ((cyclePrCount >= configBean.prCount && cycleQyCount >= configBean.qyCount) || (isPress && cycleQyCount > 0 && cyclePrCount > 0)) {
        if (isQy && !isPr && !isCycle) {
            isCycle = true
            isQy = false
            isPr = false
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

    /**
     * 按压处理逻辑
     */
    private fun pr(dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(viewBinding.chart, dataDTO.pf)
        viewBinding.pressLayoutView.smoothScrollTo(dataDTO.distance, dataDTO)
        //处理是否按压
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
            stopOutTime()
            cyclePrCount++
            cycleQyCount = 0
            isPr = true
            isQy = false
            isCycle = false
            //按压位置错误
            if (err_pr_posi != dataDTO.err_pr_posi && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.err_pr_posi
                viewBinding.ivPressAim.visibility = View.VISIBLE
                mHandler3.postDelayed({
                    viewBinding.ivPressAim.visibility = View.INVISIBLE
                }, 2000)
                setPlayVoice(VOICE_MP3_AYWZCW)
            } else if (err_qr_unback != dataDTO.err_pr_unback) {
                //按压未回弹
                err_qr_unback = dataDTO.err_pr_unback
                viewBinding.pressLayoutView.setUnBack()
                setPlayVoice(VOICE_MP3_WHT)
            } else {
                //按压不足
                if (err_pr_low != dataDTO.err_pr_low) {
                    err_pr_low = dataDTO.err_pr_low
                    viewBinding.pressLayoutView.setDown()
                    setPlayVoice(VOICE_MP3_AYBZ)
                } else if (err_pr_high != dataDTO.err_pr_high) {//按压过大
                    err_pr_high = dataDTO.err_pr_high
                    setPlayVoice(VOICE_MP3_AYGD)
                }
            }
        }

        //按压错误数统计
        viewBinding.tvPress.text = "${dataDTO.getPr_err_total()}"
        //按压总数
        viewBinding.tvPressTotal.text = "/${dataDTO.prSum}"
    }

    /**
     * 吹气状态
     */
    private fun qy(dataDTO: BaseDataDTO) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1 && dataDTO.bpValue > 5) {
            viewBinding.ivAim.visibility = View.INVISIBLE
            if (qyValue != dataDTO.qySum) {
                val qyMax = dataDTO.qyMax()
                Log.e("qyMax", "qy:${qyMax}")
                when {
                    qyMax in 35..55 -> {//通气正常
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                    qyMax > 55 -> {//通气过大
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                        setPlayVoice(VOICE_MP3_CQGD)
                    }
                    qyMax in 5..35 -> {//通气不足
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                        setPlayVoice(VOICE_MP3_CQBZ)
                    }
                    qyMax > 75 -> {//吹气进胃
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                        setPlayVoice(VOICE_MP3_CQJW)
                    }
                }
                //吹气变灰
                mHandler4.removeCallbacksAndMessages(null)
                mHandler1.postDelayed(this::setQyAimVisibility, 2000)
                stopOutTime()
            }
        } else {
            if (dataDTO.bpValue > 5) {
                stopOutTime()
                setPlayVoice(VOICE_MP3_WDKQD)
                viewBinding.ivAim.visibility = View.VISIBLE
                isQyAim = true
                mHandler4.removeCallbacksAndMessages(null)
                mHandler4.postDelayed(this::setQyAimVisibility, 2000)
            }
        }
        //记录吹气超次少次
        if (qyValue != dataDTO.qySum) {
            cycleQyCount++
            isQy = true
            isPr = false
        }
        qyValue = dataDTO.qySum
        //吹气频率
        setQyRate(viewBinding.chartQy, dataDTO.cf)
        //吹气错误数统计
        viewBinding.tvLungError.text = "${(dataDTO.getQy_err_total())}"
        viewBinding.tvLungTotal.text = "/${dataDTO.qySum}"
    }

    /**
     * 吹气图恢复状态
     */
    private fun setQyAimVisibility() {
        viewBinding.ivAim.visibility = View.INVISIBLE
        setQyRate(viewBinding.chartQy, 0)
        viewBinding.ivLung.setImageResource(R.mipmap.icon_lung_border)
        //EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_CLEAR, "", null))
    }

    private fun stopOutTime() {
        //暂停超时时间
        if (isTimeOut) {
            isTimeOut = false
            mHandler.removeCallbacks(counter)
            viewBinding.ctTime.visibility = View.INVISIBLE
//            timeOutTotal += SystemClock.elapsedRealtime() - viewBinding.ctTime.base
            viewBinding.ctTime.base = SystemClock.elapsedRealtime()
            viewBinding.ctTime.stop()
        }
    }

    //按压频率
    private fun setRate(view: DialChart07View, value: Int) {
        val max = 200
        val min = 0
        val p = value % (max - min + 1) + min
        var pf = p / 200f
        if (pf > 0) {
            pf += 0.03f
        }
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    //按压频率
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

    //按压中断 - 开启计时器 频率清零
    private val runnableCF = Runnable {
        //DataVolatile.setCF_Value()
    }

    override fun onResume() {
        super.onResume()
        setRate(viewBinding.chart, 0)
        setQyRate(viewBinding.chartQy, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().post(MessageEventData(BaseConstant.EVENT_CPR_STOP, "", null))
        StatusLiveData.data.value = null
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
        mHandler.removeCallbacksAndMessages(null)
        mHandler1.removeCallbacksAndMessages(null)
        mHandler2.removeCallbacksAndMessages(null)
        mHandler3.removeCallbacksAndMessages(null)
        mHandler4.removeCallbacksAndMessages(null)
        mHandler5.removeCallbacksAndMessages(null)
        mHandler6.removeCallbacksAndMessages(null)
        //数据清零
//        DataVolatile.dataClear()
    }

    private var alphaAniShow: AlphaAnimation? = null
    private var alphaAniHide: AlphaAnimation? = null

    //透明度动画
    private fun alphaAnimation() {
        //显示
        alphaAniShow = AlphaAnimation(0f, 1f) //百分比透明度，从0%到100%显示
        alphaAniShow?.duration = 100 //一秒

        //隐藏
        alphaAniHide = AlphaAnimation(1f, 0f)
        alphaAniHide?.duration = 100
    }
}