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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.activity.DATADTO
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.bean.ScoringConfigBean
import com.pr.perfectrecovery.databinding.CycleFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CycleViewModel
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.view.PressLayoutView
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.prefs.PreferenceChangeListener

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CycleFragment : Fragment() {
    private lateinit var viewBinding: CycleFragmentBinding
    private var counter: Counter? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var isTS: Boolean = false
    private var isYY: Boolean = false
    private var cycleCount = 0

    //中断计时累加
    private var timeOut: Long = 0

    //错误按压数统计
    private var errorPressCount: Int = 0

    //错误吹起数统计
    private var errorBlowCount: Int = 0

    //按压总数统计
    private var pressCount: Int = 0

    //总吹起数统计
    private var blowCount: Int = 0

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

    private var count = 0
    private fun initView() {
        alphaAnimation()
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        val configBean = GsonUtils.fromJson(jsonString, ScoringConfigBean::class.java)
        //按压通气比列
        StatusLiveData.data.observe(requireActivity(), Observer {
            Log.i("CPRActivity", "${count++}")
            setViewDate(it)
            viewBinding.tvPress3.text = "距离值：${it.distance}"
            viewBinding.tvPress4.text = "气压值：${it.bpValue}"
            viewBinding.tvPress5.text = "按压频率：${it.pf}"
            viewBinding.tvPress6.text = "吹气频率：${it.cf}"
            viewBinding.tvPress7.text = "气道状态：${it.aisleType}"
            viewBinding.tvPress8.text = "按压位置：${it.psrType}"
        })

        //监听按压事件回调-处理结果语音提示
        viewBinding.pressLayoutView.setScrollerCallBack { state ->
            when (state) {
                PressLayoutView.TYPE_UP -> {//未回弹
                    setPlayVoice(VOICE_MP3_WHT)
                }
                PressLayoutView.TYPE_MIN -> {//按压不足
                    setPlayVoice(VOICE_MP3_AYBZ)
                }
                PressLayoutView.TYPE_MAX -> {//按压过大
                    setPlayVoice(VOICE_MP3_AYGD)
                }
            }
        }
        // viewBinding.tvLog.movementMethod = ScrollingMovementMethod.getInstance()
        // viewBinding.tvLog.setMovementMethod(LinkMovementMethod.getInstance())
        viewBinding.chart.setOnClickListener {
            val max = 200
            val min = 0
            val random = Random()
            val p = random.nextInt(max) % (max - min + 1) + min
            val pf = p / 200f
            viewBinding.chart.setCurrentStatus(pf)
            viewBinding.chart.invalidate()
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
                }
            }
            mMediaPlayer?.start()
        } else {
            mpType = type
        }
    }

    fun start() {
        viewBinding.ivPress.setImageResource(R.mipmap.icon_wm_normal)
        viewBinding.ivLung.setImageResource(R.mipmap.icon_lung_border)
        viewBinding.dashBoard.setImageResource(R.mipmap.icon_wm_bp_2)
        viewBinding.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_2)
        viewBinding.ivPress.visibility = View.INVISIBLE
        viewBinding.pressLayoutView.visibility = View.VISIBLE
        viewBinding.dashBoard.visibility = View.INVISIBLE
        viewBinding.chart.visibility = View.VISIBLE
//        viewBinding.ivAim.visibility = View.VISIBLE
        //viewBinding.ctTime.visibility = View.VISIBLE
        startMP3()
        counter = Counter()
        mHandler.post(counter!!)
    }

    fun stop() {
        counter?.let { mHandler.removeCallbacks(it) }
        viewBinding.ctTime.stop()
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
            mMediaPlayer = null
        }
    }

    fun bluetoothDisconnected() {
        setPlayVoice(VOICE_MP3_DIS)
    }

    /**
     * 播放节奏音乐
     */
    private fun startMP3() {
        if (isYY) {//节奏音
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

    private var time: Long = 5000
    private var isTime = false

    private inner class Counter : Runnable {
        override fun run() {
            mHandler.postDelayed(this, 1000)//一秒钟循环计时一次
//            initRandom()
            if (time <= 0) {
                //记录一次按压超时
                timeOut += 1000
                if (!isTime) {
                    isTime = true
//                    viewBinding.ctTime.visibility = View.VISIBLE
                    viewBinding.ctTime.base = SystemClock.elapsedRealtime()
                    viewBinding.ctTime.start()
                }
            } else {
                isTime = false
                viewBinding.ctTime.visibility = View.INVISIBLE
            }
            time -= 1000
        }
    }

    private var pfValue = 0
    private var bfValue = 0
    private var qyValue = 0

    private fun setViewDate(dataDTO: BaseDataDTO?) {
        if (dataDTO != null) {
            bfValue = dataDTO.pf
            //计算循环次数
            if (dataDTO.prSum / 30 > cycleCount && dataDTO.qySum / 2 > cycleCount) {
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
            //按压位置 0-错误  1-正确
            viewBinding.ivPressAim.visibility =
                if (dataDTO.psrType == 0) View.VISIBLE else View.INVISIBLE
            //按压频率
            if (dataDTO.prSum != pfValue) {
                val max = 200
                val min = 0
                val p = dataDTO.pf % (max - min + 1) + min
                val pf = p / 200f
                viewBinding.chart.setCurrentStatus(pf)
                viewBinding.chart.invalidate()
            }
            pfValue = dataDTO.prSum
            //通气道是否打开 0-关闭 1-打开
            if (dataDTO.aisleType == 1) {
                viewBinding.ivAim.visibility = View.INVISIBLE
                blowCount++
                viewBinding.tvPressTotal.text = "/${dataDTO.prSum}"
                viewBinding.tvLungTotal.text = "/${dataDTO.qySum}"
                if (qyValue != dataDTO.qySum) {
                    dataDTO.bpValue = DataVolatile.max(DataVolatile.QY_valueSet)
                    qyValue = dataDTO.qySum
                    if (qyValue > 1) {
                        //吹气频率清零
                        mHandler.removeCallbacks(runnableCF)
                        mHandler.postDelayed(runnableCF, 10000)
                    }
                    when {
                        dataDTO.bpValue in 40..80 -> {//通气正常
                            viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                        }
                        dataDTO.bpValue in 80..100 -> {//通气过大
                            viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                            setPlayVoice(VOICE_MP3_CQGD)
                        }
                        dataDTO.bpValue < 40 -> {//通气不足
                            viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                            setPlayVoice(VOICE_MP3_CQBZ)
                        }
                        dataDTO.bpValue > 100 -> {//吹气进胃
                            viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                            setPlayVoice(VOICE_MP3_CQJW)
                        }
                    }
                }
            } else {
                setPlayVoice(VOICE_MP3_WDKQD)
            }
            //吹气错误数统计
            viewBinding.tvLungError.text =
                "${(dataDTO.ERR_QY_CLOSE + dataDTO.ERR_QY_HIGH + dataDTO.ERR_QY_LOW + dataDTO.ERR_QY_DEAD)}"
            //按压错误数统计
            viewBinding.tvPress.text =
                "${(dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH)}"
            //按压总数
            viewBinding.tvPressTotal.text = "/${dataDTO.prSum}"
//            } else {
//            viewBinding.ivAim.visibility = View.VISIBLE
            if (dataDTO.distance > 0) {
                pressCount = dataDTO.prSum
                viewBinding.pressLayoutView.smoothScrollTo(dataDTO.distance, dataDTO.prSum)
            }

        }
    }

    private var isPT = false

    //按压中断 - 开启计时器 频率清零
    private val runnable = Runnable {
        isPT = false
        mHandler.removeCallbacksAndMessages(null)
        time = 5000
        mHandler.post(counter!!)
    }

    //按压中断 - 开启计时器 频率清零
    private val runnableCF = Runnable {
        DataVolatile.setCF_Value()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
        //数据清零
        DataVolatile.QY_SUM = 0
        DataVolatile.PR_SUM = 0
        DataVolatile.CF_Value = 0
        DataVolatile.PF_Value = 0
        DataVolatile.ERR_PR_HIGH = 0
        DataVolatile.ERR_PR_LOW = 0
        DataVolatile.ERR_PR_POSI = 0
        DataVolatile.ERR_QY_CLOSE = 0
        DataVolatile.ERR_QY_DEAD = 0
        DataVolatile.ERR_QY_HIGH = 0
        DataVolatile.ERR_QY_LOW = 0
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