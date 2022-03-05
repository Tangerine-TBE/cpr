package com.pr.perfectrecovery.adapter

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import com.blankj.utilcode.util.GsonUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.CycleFragmentMultiItemBinding
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.view.DialChart07View
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

/**
 * desc   : 多模块界面适配器
 * author : hunger
 * date   : 2022/2/24
 * version: 1.0
 */
class MultiActAdapter :
    BaseQuickAdapter<BaseDataDTO, BaseViewHolder>(R.layout.cycle_fragment_multi_item) {
    private var configBean = ConfigBean()
    private var cycleCountMap = mutableMapOf<String, Int>()
    private var mBaseDataMap = mutableMapOf<String, BaseDataDTO>()
    private var prValueMap = mutableMapOf<String, Int>()
    //按压位置错误
    private var errPrPosiMap = mutableMapOf<String, Int>()
    //按压未回弹
    private var errPrUnbackMap = mutableMapOf<String, Int>()
    // 按压不足
    private var errPrLowMap = mutableMapOf<String, Int>()
    // 按压过大
    private var errPrHighMap = mutableMapOf<String, Int>()
    // 按压总数
    private var pressCountMap = mutableMapOf<String, Int>()
    private var isTimeingMap = mutableMapOf<String, Boolean>()
    private var qyValueMap = mutableMapOf<String, Int>()
    private var currentShowViewMap = mutableMapOf<String, ConstraintLayout>()

    init {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
    }

    override fun convert(holder: BaseViewHolder, item: BaseDataDTO) {
        // 先把需要用的数据初始化下
        val viewBinding = CycleFragmentMultiItemBinding.bind(holder.itemView)
        showPosition(viewBinding, holder.adapterPosition)
        //先判断是哪个组件显示
        showView(viewBinding, item)
//        setDataToView(viewBinding, item)
    }

    @SuppressLint("SetTextI18n")
    private fun showPosition(binding: CycleFragmentMultiItemBinding, position:Int) {
        binding.position1.visibility = if (position % 2 == 0) View.GONE else View.VISIBLE
        binding.position2.visibility = if (position % 2 == 0) View.VISIBLE else View.GONE
        binding.position2.text = "${position + 1}"
        binding.position1.text = "${position + 1}"
        binding.ratingBar.isEnabled = false
    }

    private fun showView(binding: CycleFragmentMultiItemBinding, data: BaseDataDTO) {
//        binding.layoutScore.visibility = View.GONE
//        binding.layoutPress.visibility = View.GONE
//        binding.layoutLung.visibility = View.GONE

        //获取上一次的视图，如果前面没缓存状态，则是首次进来，初始为按压的视图
        var curShowView = currentShowViewMap[data.mac]
        if (curShowView == null) {
            curShowView = binding.layoutPress
        }
        Log.e("debugDistance", "mac: ${data.mac}; distance new : ${data.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}; distance old : ${mBaseDataMap[data.mac]?.distance} " )

        val isSame = mBaseDataMap[data.mac]?.let { isDataSame(it, data) }
        if (isSame == true) {
            return
        }
        mBaseDataMap[data.mac] = data

        val preDistance = DataVolatile.preDistanceMap[data.mac]?: -1L
        val isPress = abs(preDistance - data.distance) > 10
        val isBlow = data.bpValue != 0

        //没有按压 也没有吹气，显示上一次的视图
        if (!isPress && !isBlow) {
            curShowView.visibility = View.VISIBLE
        } else if (isPress) {
            binding.layoutScore.visibility = View.GONE
            binding.layoutLung.visibility = View.GONE
            binding.layoutPress.visibility = View.VISIBLE
            curShowView = binding.layoutPress
            pr(binding, data)
        } else if(isBlow) {
            binding.layoutPress.visibility = View.GONE
            binding.layoutScore.visibility = View.GONE
            binding.layoutLung.visibility = View.VISIBLE
            curShowView = binding.layoutLung
            qy(binding, data)
        }
        currentShowViewMap[data.mac] = curShowView

        // 未开始  显示灰色的图标
        if (!data.isStart) {
            binding.ivPress.visibility = View.VISIBLE
            binding.ivPress.setImageResource(R.mipmap.icon_wm_press)
            binding.ivLung.visibility = View.VISIBLE
            binding.ivLung.setImageResource(R.mipmap.icon_wm_lung)
            binding.dashBoard.visibility = View.VISIBLE
            binding.dashBoard.setImageResource(R.mipmap.icon_wm_bp_1)
            binding.dashBoard2.visibility = View.VISIBLE
            binding.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_1)

            binding.pressLayoutView.visibility = View.INVISIBLE
            binding.chart.visibility = View.INVISIBLE
            binding.chartQy.visibility = View.INVISIBLE
        } else {
            if(!isPress && !isBlow) {
                //开始，但是暂无数据
                binding.ivPress.setImageResource(R.mipmap.icon_wm_normal)
                binding.ivLung.setImageResource(R.mipmap.icon_lung_border)
                binding.dashBoard.setImageResource(R.mipmap.icon_wm_bp_2)
                binding.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_2)
            } else {
                binding.ivPress.visibility = View.INVISIBLE
                binding.pressLayoutView.visibility = View.VISIBLE
                binding.dashBoard.visibility = View.INVISIBLE
                binding.dashBoard2.visibility = View.INVISIBLE
                binding.chart.visibility = View.VISIBLE
                binding.chartQy.visibility = View.VISIBLE
            }
        }
    }

    private fun setDataToView(binding: CycleFragmentMultiItemBinding, data: BaseDataDTO?) {
        if (data != null) {
            mBaseDataMap[data.mac] = data
            if (configBean.prCount > 0 || configBean.qyCount > 0) {

                //计算循环次数
                var cycleCount = cycleCountMap[data.mac] ?: 0
                if (data.prSum / configBean.prCount > cycleCount && data.qySum / configBean.qyCount > cycleCount) {
                    cycleCount++
                    cycleCountMap[data.mac] = cycleCount
                    //更新循环次数
                    EventBus.getDefault()
                        .post(
                            MessageEventData(
                                BaseConstant.EVENT_SINGLE_DATA_CYCLE,
                                "$cycleCountMap",
                                null
                            )
                        )
                }
            }
            //更新循环次数
            val pressCount = pressCountMap[data.mac] ?: 0
            val isTiming = isTimeingMap[data.mac] ?: false
            if (pressCount != data.prSum && isTiming) {
                isTimeingMap[data.mac] = false
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
     * 按压处理逻辑
     */
    private fun pr(binding: CycleFragmentMultiItemBinding, data: BaseDataDTO) {
        setRate(binding.chart, data.pf)
        val prValue = prValueMap[data.mac] ?: 0
        binding.pressLayoutView.smoothScrollTo(data.distance)
        if (data.prSum != prValue) {
            prValueMap[data.mac] = data.prSum
            //按压位置错误
            val errPrPosi = errPrPosiMap[data.mac] ?: 0
            val errPrUnback = errPrUnbackMap[data.mac] ?: 0
            if (errPrPosi != data.ERR_PR_POSI && data.psrType == 0) {
                errPrPosiMap[data.mac] = data.ERR_PR_POSI
            } else if (errPrUnback != data.ERR_PR_UNBACK) {
                //按压未回弹
                errPrUnbackMap[data.mac] = data.ERR_PR_UNBACK
                binding.pressLayoutView.setUnBack()
            } else {
                //按压不足
                val errPrLow = errPrLowMap[data.mac] ?: 0
                val errPrHigh = errPrHighMap[data.mac] ?: 0
                if (errPrLow != data.ERR_PR_LOW) {
                    Log.e("TAG123", "按压错误：${data.ERR_PR_LOW}")
                    errPrLowMap[data.mac] = data.ERR_PR_LOW
                    binding.pressLayoutView.setDown()
                } else if (errPrHigh != data.ERR_PR_HIGH) {//按压过大
                    errPrHighMap[data.mac] = data.ERR_PR_HIGH
                }
            }
            pressCountMap[data.mac] = data.prSum
        }
        //按压位置错误显示错误图标
        if (data.psrType == 0) {
            binding.ivPressAim.visibility = View.VISIBLE
        } else {
            binding.ivPressAim.visibility = View.INVISIBLE
        }
        //按压错误数统计
        binding.tvPress.text = "${(data.ERR_PR_POSI + data.ERR_PR_LOW + data.ERR_PR_HIGH + data.ERR_PR_UNBACK)}"
        //按压总数
        binding.tvPressTotal.text = "/${data.prSum}"
    }

    /**
     * 吹气状态
     */
    private fun qy(binding: CycleFragmentMultiItemBinding, data: BaseDataDTO) {
        setRate(binding.chartQy, data.cf)
        //通气道是否打开 0-关闭 1-打开
        if (data.aisleType == 1) {
            binding.ivAim.visibility = View.INVISIBLE
            val quValue = qyValueMap[data.mac] ?: 0
            if (quValue != data.qySum) {
                val qyMax = DataVolatile.max(DataVolatile.QY_valueSet, false)
                when {
                    qyMax in configBean.qyLow()..configBean.qyHigh() -> {//通气正常
                        binding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                    qyMax in configBean.qyHigh()..100 -> {//通气过大
                        binding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
                    }
                    qyMax < configBean.qyLow() -> {//通气不足
                        binding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
                    }
                    qyMax > configBean.qy_max -> {//吹气进胃
                        binding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
                    }
                }
                //吹气变灰

            }
        } else {
            binding.ivAim.visibility = View.VISIBLE
        }

        qyValueMap[data.mac] = data.qySum
        //吹气错误数统计
        binding.tvLungError.text = "${(data.ERR_QY_CLOSE + data.ERR_QY_HIGH + data.ERR_QY_LOW + data.ERR_QY_DEAD)}"
    }

    //设置仪表数据
    private fun setRate(view: DialChart07View, value: Int) {
        val max = 200
        val min = 0
        val p = value % (max - min + 1) + min
        val pf = p / 200f
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    fun getCycleCount(mac:String): Int{
        return cycleCountMap[mac] ?: 0
    }

    fun isDataSame(@NonNull oldItem: BaseDataDTO, @NonNull newItem: BaseDataDTO):Boolean{
        return TextUtils.equals(oldItem.mac, newItem.mac)
                && oldItem.electricity == newItem.electricity
                && oldItem.distance == newItem.distance
                && oldItem.bpValue == newItem.bpValue
                && oldItem.blsType == newItem.blsType
                && oldItem.usbConnectType == newItem.usbConnectType
                && oldItem.aisleType == newItem.aisleType
                && oldItem.connectType == newItem.connectType
                && oldItem.psrType == newItem.psrType
                && oldItem.workType == newItem.workType
                && oldItem.pf == newItem.pf
                && oldItem.cf == newItem.cf
                && oldItem.prSum == newItem.prSum
                && oldItem.qySum == newItem.qySum
                && oldItem.ERR_PR_UNBACK == newItem.ERR_PR_UNBACK
                && oldItem.ERR_PR_LOW == newItem.ERR_PR_LOW
                && oldItem.ERR_PR_HIGH == newItem.ERR_PR_HIGH
                && oldItem.ERR_PR_POSI == newItem.ERR_PR_POSI
                && oldItem.ERR_QY_LOW == newItem.ERR_QY_LOW
                && oldItem.ERR_QY_HIGH == newItem.ERR_QY_HIGH
                && oldItem.ERR_QY_CLOSE == newItem.ERR_QY_CLOSE
                && oldItem.PR_DEPTH_SUM == newItem.PR_DEPTH_SUM
                && oldItem.PR_TIME_SUM == newItem.PR_TIME_SUM
                && oldItem.QY_VOLUME_SUM == newItem.QY_VOLUME_SUM
                && oldItem.QY_TIME_SUM == newItem.QY_TIME_SUM
                && oldItem.PR_SEQRIGHT_TOTAL == newItem.PR_SEQRIGHT_TOTAL
                && oldItem.QY_SERRIGHT_TOTAL == newItem.QY_SERRIGHT_TOTAL
                && oldItem.isStart == newItem.isStart
    }
}