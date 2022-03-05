package com.pr.perfectrecovery.adapter

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.view.DialChart07View
import com.pr.perfectrecovery.view.PressLayoutView2
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

/**
 * desc   : 多模块界面适配器
 * author : hunger
 * date   : 2022/2/24
 * version: 1.0
 */
class MultiActAdapter(private val mContext: Context) : RecyclerView.Adapter<MultiActAdapter.MyViewHolder>() {
    private val dataDiffl: AsyncListDiffer<BaseDataDTO>
    private val diffCallback: DiffUtil.ItemCallback<BaseDataDTO> = MyItemCallBack()

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
    private var currentShowView:ConstraintLayout? = null

    init {
        dataDiffl = AsyncListDiffer(this, diffCallback)
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
    }

    fun isCheck(check:Boolean) {
        isCheck = check
    }

    fun getCycleCount(mac:String): Int{
        return cycleCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(mContext).inflate(R.layout.cycle_fragment_multi_item, null)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (TextUtils.isEmpty(holder.mac)) {
            holder.mac = item.mac
        }
        if (holder.mac == item.mac){
            showPosition(holder, position)
            showView(holder, item)
            setDataToView(holder, item)
        }
    }

    private fun showPosition(holder: MyViewHolder, position: Int) {
        holder.position1?.visibility = if (position % 2 == 0) View.GONE else View.VISIBLE
        holder.position2?.visibility = if (position % 2 == 0) View.VISIBLE else View.GONE
        holder.position2?.text = (position + 1).toString()
        holder.position1?.text = (position + 1).toString()
        holder.ratingBar?.isEnabled = false
    }

    private fun showView(holder: MyViewHolder, data: BaseDataDTO) {
        holder.layoutScore.visibility = View.GONE
        holder.layoutPress.visibility = View.GONE
        holder.layoutLung.visibility = View.GONE

        //获取上一次的视图，如果前面没缓存状态，则是首次进来，初始为按压的视图
        if (currentShowView == null) {
            currentShowView = holder.layoutPress
        }

        val preDistance = DataVolatile.preDistanceMap[data.mac]?: -1L
        val isPress = abs(preDistance - data.distance) > 10
        val isBlow = data.bpValue > 5

        Log.e("debugDistance", "mac: ${data.mac}-> preDistance : ${preDistance}, cur:${data.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> new : ${data.distance}, old:${mBaseData?.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> new : ${data.distance}, old:${mBaseData?.distance} " )
        Log.e("debugDistance", "mac: ${data.mac}-> bpValue: ${data.bpValue} " )
        Log.e("debugDistance", "mac: ${data.mac}-> isPress: ${isPress}" )
        Log.e("debugDistance", "mac: ${data.mac}-> isBlow: ${isBlow} " )

        //没有按压 也没有吹气，显示上一次的视图
        if (!isPress && !isBlow) {
            currentShowView?.visibility = View.VISIBLE
        } else if (isPress) {
            holder.layoutScore.visibility = View.GONE
            holder.layoutLung.visibility = View.GONE
            holder.layoutPress.visibility = View.VISIBLE
            currentShowView = holder.layoutPress
            pr(holder, data)
        } else if(isBlow) {
            holder.layoutPress.visibility = View.GONE
            holder.layoutScore.visibility = View.GONE
            holder.layoutLung.visibility = View.VISIBLE
            currentShowView = holder.layoutLung
//            qy(binding, data)
        }

        // 未开始  显示灰色的图标
        if (!data.isStart) {
            holder.ivPress.visibility = View.VISIBLE
            holder.ivPress.setImageResource(R.mipmap.icon_wm_press)
            holder.ivLung.visibility = View.VISIBLE
            holder.ivLung.setImageResource(R.mipmap.icon_wm_lung)
            holder.dashBoard.visibility = View.VISIBLE
            holder.dashBoard.setImageResource(R.mipmap.icon_wm_bp_1)
            holder.dashBoard2.visibility = View.VISIBLE
            holder.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_1)

            holder.pressLayoutView.visibility = View.INVISIBLE
            holder.chart.visibility = View.INVISIBLE
            holder.chartQy.visibility = View.INVISIBLE
        } else {
            if(!isPress && !isBlow) {
                //开始，但是暂无数据
                holder.ivPress.setImageResource(R.mipmap.icon_wm_normal)
                holder.ivLung.setImageResource(R.mipmap.icon_lung_border)
                holder.dashBoard.setImageResource(R.mipmap.icon_wm_bp_2)
                holder.dashBoard2.setImageResource(R.mipmap.icon_wm_bp_2)
            } else {
                holder.ivPress.visibility = View.INVISIBLE
                holder.pressLayoutView.visibility = View.VISIBLE
                holder.dashBoard.visibility = View.INVISIBLE
                holder.dashBoard2.visibility = View.INVISIBLE
                holder.chart.visibility = View.VISIBLE
                holder.chartQy.visibility = View.VISIBLE
            }
        }
    }

    private fun setDataToView(holder: MyViewHolder, data: BaseDataDTO) {
        mBaseData = data
        //计算循环次数
        cycle(data)
        //按压
        pr(holder!!, data)
        //吹气
//        qy(data)


    }

    private fun cycle(dataDTO: BaseDataDTO) {
//        if ((cyclePrCount >= configBean.prCount && cycleQyCount >= configBean.qyCount) || (isPress && cycleQyCount > 0 && cyclePrCount > 0)) {
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

    private fun pr(holder: MyViewHolder, dataDTO: BaseDataDTO) {
        //按压位置 0-错误  1-正确
//        if (dataDTO.psrType == 1) {
        //按压频率
        setRate(holder.chart, dataDTO.pf)
        holder.pressLayoutView.smoothScrollTo(dataDTO.distance)
        //处理是否按压
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //暂停超时时间 - 判断是否小于初始值
//            stopOutTime()
            cyclePrCount++
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                holder.ivPressAim.visibility = View.VISIBLE
//                mHandler3.removeCallbacksAndMessages(null)
//                mHandler3.postAtTime(Runnable {
//                    holder.ivPressAim.visibility = View.INVISIBLE
//                }, 2000)
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                holder.pressLayoutView.setUnBack()
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    err_pr_low = dataDTO.ERR_PR_LOW
                    holder.pressLayoutView.setDown()
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                }
            }
        }
        //按压错误数统计
        holder.tvPress.text =
            "${(dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH + dataDTO.ERR_PR_UNBACK)}"
        //按压总数
        holder.tvPressTotal.text = "/${dataDTO.prSum}"
    }

    private fun setRate(view: DialChart07View, value: Int) {
        val max = 200
        val min = 0
        val p = value % (max - min + 1) + min
        val pf = p / 200f
        view.setCurrentStatus(pf)
        view.invalidate()
    }

    override fun getItemCount(): Int {
        return dataDiffl.currentList.size
    }

    fun submitList(data: List<BaseDataDTO>?) {
        dataDiffl.submitList(data)
    }

    fun getItem(position: Int): BaseDataDTO {
        return dataDiffl.currentList[position]
    }

    class MyViewHolder(itemView : View): RecyclerView.ViewHolder(itemView) {
        var mac = ""
        //初始化view
        val layoutScore = itemView.findViewById<ConstraintLayout>(R.id.layout_score)
        val layoutPress = itemView.findViewById<ConstraintLayout>(R.id.layout_press)
        val layoutLung = itemView.findViewById<ConstraintLayout>(R.id.layout_lung)
        val position1 = itemView.findViewById<TextView>(R.id.position1)
        val position2 = itemView.findViewById<TextView>(R.id.position2)
        val ratingBar = itemView.findViewById<RatingBar>(R.id.ratingBar)
        val ivPress = itemView.findViewById<ImageView>(R.id.ivPress)
        val ivLung = itemView.findViewById<ImageView>(R.id.ivLung)
        val dashBoard = itemView.findViewById<ImageView>(R.id.dashBoard)
        val dashBoard2 = itemView.findViewById<ImageView>(R.id.dashBoard2)
        val pressLayoutView = itemView.findViewById<PressLayoutView2>(R.id.pressLayoutView)
        val chart = itemView.findViewById<DialChart07View>(R.id.chart)
        val chartQy = itemView.findViewById<DialChart07View>(R.id.chartQy)
        val ivPressAim = itemView.findViewById<ImageView>(R.id.ivPressAim)
        val tvPress = itemView.findViewById<TextView>(R.id.tvPress)
        val tvPressTotal = itemView.findViewById<TextView>(R.id.tvPressTotal)
    }


}