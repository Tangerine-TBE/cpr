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
import android.widget.Chronometer
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.CycleFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CycleViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.utils.TimeUtils
import com.pr.perfectrecovery.view.DialChart07View
import com.pr.perfectrecovery.view.PressLayoutView
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * CPR按压页
 */
class CycleFragment : Fragment() {
    private lateinit var viewBinding: CycleFragmentBinding
    private var counter = Counter()
    private var mMediaPlayer: MediaPlayer? = null
    private var isTS: Boolean = false
    private var isYY: Boolean = false
    private var cycleCount = 0
    private var mBaseDataDTO: BaseDataDTO? = null
    private var isStart = false
    private var isTimeing = true
    private var configBean = ConfigBean()

    //中断计时累加
    private var timeOut: Long = 0

    //按压总数统计
    private var pressCount: Int = 0

    companion object {
        fun newInstance(isTS: Boolean, isYY: Boolean) = CycleFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_PARAM1, isTS)
                putBoolean(ARG_PARAM2, isYY)
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
        }
        viewModel = ViewModelProvider(this).get(CycleViewModel::class.java)
        initView()
    }

    private fun initView() {
        alphaAnimation()
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        DataVolatile.PR_HIGH_VALUE = configBean.prHigh()
        DataVolatile.PR_LOW_VALUE = configBean.prLow()
        //按压通气比列
        StatusLiveData.data.observe(requireActivity(), Observer {
            if (it.isStart) {
                setViewDate(it)
                viewBinding.tvPress3.text = "距离值：${it.distance}"
                viewBinding.tvPress4.text = "气压值：${it.bpValue}"
                viewBinding.tvPress5.text = "按压频率：${it.pf}"
                viewBinding.tvPress6.text = "吹气频率：${it.cf}"
                viewBinding.tvPress7.text = "气道状态：${it.aisleType}"
                viewBinding.tvPress8.text = "按压位置：${it.psrType}"
                viewBinding.tvPress9.text = "初始值：${DataVolatile.preDistance}"
                viewBinding.tvPress10.text = "按压深度：${abs(DataVolatile.preDistance - it.distance)}"
            }
        })
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
        isStart = false
        val trainingDTO = TrainingDTO()
        mBaseDataDTO?.apply {
            trainingDTO.pressOutTime = TimeUtils.timeParse(timeOut).toString()
            trainingDTO.pressHigh = ERR_PR_HIGH
            trainingDTO.pressLow = ERR_PR_LOW
            trainingDTO.pressLocation = ERR_PR_POSI
            trainingDTO.pressRebound = ERR_PR_UNBACK
            //按压总错误数
            trainingDTO.pressErrorCount = ERR_PR_HIGH + ERR_PR_LOW + ERR_PR_POSI + ERR_PR_UNBACK
            trainingDTO.blowHigh = ERR_QY_HIGH
            trainingDTO.blowLow = ERR_QY_LOW
            trainingDTO.blowIntoStomach = ERR_QY_DEAD
            trainingDTO.blowClose = ERR_QY_CLOSE
            //吹气总错误数
            trainingDTO.blowErrorCount = ERR_QY_HIGH + ERR_QY_LOW + ERR_QY_DEAD + ERR_QY_CLOSE
            trainingDTO.pressTotal = prSum
            trainingDTO.blowTotal = qySum
        }
        counter.let { mHandler.removeCallbacks(it) }
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

    private inner class Counter : Runnable {
        override fun run() {
            viewBinding.ctTime.visibility = View.VISIBLE
            //记录一次按压超时
            viewBinding.ctTime.base = SystemClock.elapsedRealtime()
            viewBinding.ctTime.start()
        }
    }

    private var prValue = 0
    private var qyValue = 0
    private var err_pr_low = 0
    private var err_pr_high = 0
    private var err_pr_posi = 0
    private var err_qr_unback = 0
    private var isTimeOut = false

    private fun setViewDate(dataDTO: BaseDataDTO?) {
        if (dataDTO != null) {
            mBaseDataDTO = dataDTO
            if (configBean.prCount > 0 || configBean.qyCount > 0) {
                //计算循环次数
                if (dataDTO.prSum / configBean.prCount > cycleCount && dataDTO.qySum / configBean.qyCount > cycleCount) {
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

            //吹气频率
            setRate(viewBinding.chartQy, dataDTO.cf)
            //按压
            pr(dataDTO)
            //吹气
            qy(dataDTO)
            if (!isTimeOut && dataDTO.distance == DataVolatile.preDistance.toInt() && dataDTO.bpValue <= 0 && dataDTO.prSum > 0) {
                isTimeOut = true
                mHandler.removeCallbacks(counter)
                mHandler.postDelayed(counter, 5000)
            }
            //更新循环次数
            if (pressCount != dataDTO.prSum && isTimeing) {
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
     * 吹气状态
     */
    private fun qy(dataDTO: BaseDataDTO) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1) {
            viewBinding.ivAim.visibility = View.INVISIBLE
            if (qyValue != dataDTO.qySum) {
                val qyMax = DataVolatile.max(DataVolatile.QY_valueSet, false)
                when {
                    qyMax in configBean.qyLow()..configBean.qyHigh() -> {//通气正常
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                    qyMax in configBean.qyHigh()..100 -> {//通气过大
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                        setPlayVoice(VOICE_MP3_CQGD)
                    }
                    qyMax < configBean.qyLow() -> {//通气不足
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                        setPlayVoice(VOICE_MP3_CQBZ)
                    }
                    qyMax > configBean.qy_max -> {//吹气进胃
                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                        setPlayVoice(VOICE_MP3_CQJW)
                    }
                }
                //吹气变灰
                mHandler1.removeCallbacks(blowRunnable)
                mHandler1.postDelayed(blowRunnable, 2000)
                stopOutTime()
            }
        } else {
            if (dataDTO.bpValue > 5) {
                stopOutTime()
                setPlayVoice(VOICE_MP3_WDKQD)
            }
            viewBinding.ivAim.visibility = View.VISIBLE
            mHandler4.removeCallbacksAndMessages(null)
            mHandler4.postAtTime(this::setQyAimVisibility, 2000)
        }
        if (dataDTO.bpValue <= 0) {
            //吹气频率清零
            mHandler2.removeCallbacks(runnableCF)
            mHandler2.postDelayed(runnableCF, 10000)
        }

        qyValue = dataDTO.qySum
        //吹气错误数统计
        viewBinding.tvLungError.text =
            "${(dataDTO.ERR_QY_CLOSE + dataDTO.ERR_QY_HIGH + dataDTO.ERR_QY_LOW + dataDTO.ERR_QY_DEAD)}"
    }

    private fun setQyAimVisibility() {
        viewBinding.ivAim.visibility = View.INVISIBLE
    }

    /**
     * 按压处理逻辑
     */
    private fun pr(dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(viewBinding.chart, dataDTO.pf)
//        viewBinding.pressLayoutView.smoothScrollTo(dataDTO.distance)
        //执行三次按压深度
        viewBinding.pressLayoutView.smoothScrollTo(dataDTO.L_D1)
//        mHandler5.removeCallbacksAndMessages(null)
//        mHandler5.postAtTime(Runnable {
//            viewBinding.pressLayoutView.smoothScrollTo(dataDTO.L_D2)
//            mHandler6.removeCallbacksAndMessages(null)
//            mHandler6.postAtTime(Runnable {
//                viewBinding.pressLayoutView.smoothScrollTo(dataDTO.L_D3)
//            }, 33)
//        }, 33)

        if (dataDTO.prSum != prValue) {

            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
            stopOutTime()
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                setPlayVoice(VOICE_MP3_AYWZCW)
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                viewBinding.pressLayoutView.setUnBack()
                setPlayVoice(VOICE_MP3_WHT)
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    Log.e("TAG123", "按压错误：${dataDTO.ERR_PR_LOW}")
                    err_pr_low = dataDTO.ERR_PR_LOW
                    viewBinding.pressLayoutView.setDown()
                    setPlayVoice(VOICE_MP3_AYBZ)
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                    setPlayVoice(VOICE_MP3_AYGD)
                }
            }
            pressCount = dataDTO.prSum
        }
//        }
        //按压位置错误显示错误图标
        if (dataDTO.psrType == 0) {
            viewBinding.ivPressAim.visibility = View.VISIBLE
            mHandler3.removeCallbacksAndMessages(null)
            mHandler3.postAtTime(Runnable {
                viewBinding.ivPressAim.visibility = View.INVISIBLE
            }, 2000)
            stopOutTime()
        } else {
            viewBinding.ivPressAim.visibility = View.INVISIBLE
        }

        //按压错误数统计
        viewBinding.tvPress.text =
            "${(dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH + dataDTO.ERR_PR_UNBACK)}"
        //按压总数
        viewBinding.tvPressTotal.text = "/${dataDTO.prSum}"
        viewBinding.tvLungTotal.text = "/${dataDTO.qySum}"
    }

    private fun stopOutTime() {
        //暂停超时时间
        isTimeOut = false
        viewBinding.ctTime.visibility = View.INVISIBLE
        mHandler.removeCallbacks(counter)
        viewBinding.ctTime.stop()
    }

    //按压吹气频率
    private fun setRate(view: DialChart07View, value: Int) {
        val max = 200
        val min = 0
        val p = value % (max - min + 1) + min
        val pf = p / 200f
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    /**
     * 吹气图恢复状态
     */
    private val blowRunnable = Runnable {
        viewBinding.ivLung.setImageResource(R.mipmap.icon_lung_border)
        setRate(viewBinding.chartQy, 0)
    }

    //按压中断 - 开启计时器 频率清零
    private val runnableCF = Runnable {
        DataVolatile.setCF_Value()
    }

    override fun onResume() {
        super.onResume()
        setRate(viewBinding.chart, 0)
        setRate(viewBinding.chartQy, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
        //数据清零
        DataVolatile.dataClear()
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