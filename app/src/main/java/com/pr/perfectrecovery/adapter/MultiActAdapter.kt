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
import com.pr.perfectrecovery.activity.MultiActivity
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
    private var isCheck:Boolean = false
    private var cycleCount = 0
    private var cyclePrCount = 0
    private var prManyCount = 0
    private var prLessCount = 0
    private var cycleQyCount = 0
    private var qyManyCount = 0
    private var qyLessCount = 0
    private var mBaseData: BaseDataDTO? = null
    private var prValue = 0
    //按压位置错误
    private var err_pr_posi = 0
    //按压未回弹
    private var err_qr_unback = 0
    // 按压不足
    private var err_pr_low = 0
    // 按压过大
    private var err_pr_high = 0
    // 按压总数
    private var pressCount = 0
    private var isTimeing = false
    private var qyValue = 0
    private var qyRate = 0
    private var currentShowView:ConstraintLayout? = null

    init {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
    }

    override fun convert(holder: BaseViewHolder, item: BaseDataDTO) {
        Log.e("MultiActivity.TAG", "updateData: index: ${holder.adapterPosition}, data is: ${item.mac}}", )
        val viewBinding = CycleFragmentMultiItemBinding.bind(holder.itemView)
        showPosition(viewBinding, holder.adapterPosition)
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

        //获取上一次的视图，如果前面没缓存状态，则是首次进来，初始为按压的视图
        if (currentShowView == null) {
            currentShowView = binding.layoutPress
        }

        val preDistance = data.preDistance
        val isPress = abs(preDistance - data.distance) > 10
        val isBlow = data.bpValue > 5

        Log.e("debugDistance", "mac: ${data.mac}-> preDistance : ${preDistance}, cur:${data.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> new : ${data.distance}, old:${mBaseData?.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> new : ${data.distance}, old:${mBaseData?.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> bpValue: ${data.bpValue} " )
        Log.e("debugDistance", "mac: ${data.mac}-> isPress: ${isPress}" )
        Log.e("debugDistance", "mac: ${data.mac}-> isBlow: ${isBlow} " )

        // 假数据置灰
        if (TextUtils.equals(data.mac, BaseConstant.FAKE_MAC)) {
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
            binding.ivPress.setImageResource(R.mipmap.icon_wm_normal)
            binding.ivLung.setImageResource(R.mipmap.icon_lung_border)
            binding.dashBoard.setImageResource(R.mipmap.icon_wm_bp_2)
            binding.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_2)
                binding.ivPress.visibility = View.INVISIBLE
                binding.pressLayoutView.visibility = View.VISIBLE
                binding.dashBoard.visibility = View.INVISIBLE
                binding.dashBoard2.visibility = View.INVISIBLE
                binding.chart.visibility = View.VISIBLE
                setRate(binding.chart, 0)
                binding.chartQy.visibility = View.VISIBLE
                setRate(binding.chartQy, 0)
        }

        //没有按压 也没有吹气，显示上一次的视图
        if (!isPress && !isBlow) {
            currentShowView?.visibility = View.VISIBLE
        } else if (isPress) {
            binding.layoutScore.visibility = View.GONE
            binding.layoutLung.visibility = View.GONE
            binding.layoutPress.visibility = View.VISIBLE
            currentShowView = binding.layoutPress
            pr(binding, data)
        } else if(isBlow) {
            binding.layoutPress.visibility = View.GONE
            binding.layoutScore.visibility = View.GONE
            binding.layoutLung.visibility = View.VISIBLE
            currentShowView = binding.layoutLung
            qy(binding, data)
        }
    }

    private fun setDataToView(holder: CycleFragmentMultiItemBinding, data: BaseDataDTO) {
        mBaseData = data
        //计算循环次数
        cycle(data)
        //按压
        pr(holder!!, data)
        //吹气
//        qy(data)


    }

    private fun cycle(dataDTO: BaseDataDTO) {
        if (dataDTO.prSum / configBean.prCount > cycleCount && dataDTO.qySum / configBean.qyCount > cycleCount) {
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

    private fun pr(binding: CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(binding.chart, dataDTO.pf)
        binding.pressLayoutView.smoothScrollTo(dataDTO.distance, dataDTO)
        //处理是否按压
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
//            stopOutTime()
            cyclePrCount++
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                binding.ivPressAim.visibility = View.VISIBLE
//                mHandler3.removeCallbacksAndMessages(null)
//                mHandler3.postAtTime(Runnable {
//                    binding.ivPressAim.visibility = View.INVISIBLE
//                }, 2000)
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                binding.pressLayoutView.setUnBack()
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    err_pr_low = dataDTO.ERR_PR_LOW
                    binding.pressLayoutView.setDown()
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                }
            }
        }
        //按压错误数统计
        binding.tvPress.text =
            "${(dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH + dataDTO.ERR_PR_UNBACK)}"
        //按压总数
        binding.tvPressTotal.text = "/${dataDTO.prSum}"
    }

    private fun qy(binding: CycleFragmentMultiItemBinding, dataDTO: BaseDataDTO) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1) {
            binding.ivAim.visibility = View.INVISIBLE
            if (qyValue != dataDTO.qySum) {
                val qyMax = dataDTO.qyMax
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
//                //吹气变灰
//                mHandler1.removeCallbacks(blowRunnable)
//                mHandler1.postDelayed(blowRunnable, 2000)
//                stopOutTime()
            }
        } else {
            if (dataDTO.bpValue > 5) {
//                stopOutTime()
                binding.ivAim.visibility = View.VISIBLE
//                mHandler4.removeCallbacksAndMessages(null)
//                mHandler4.postAtTime(this::setQyAimVisibility, 2000)
            }
        }
        //记录吹气超次少次
        if (qyValue != dataDTO.qySum) {
            cycleQyCount++
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
        setRate(binding.chartQy, dataDTO.cf)
        //吹气错误数统计
        binding.tvLungError.text =
            "${(dataDTO.ERR_QY_CLOSE + dataDTO.ERR_QY_HIGH + dataDTO.ERR_QY_LOW + dataDTO.ERR_QY_DEAD)}"
        binding.tvLungTotal.text = "/${dataDTO.qySum}"
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
        return cycleCount
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