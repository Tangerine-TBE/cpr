package com.pr.perfectrecovery.fragment.viewmodel

import androidx.lifecycle.ViewModel
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.utils.DataVolatile

class CycleViewModel : ViewModel() {
    private var prValue = 0
    private var err_pr_posi = 0
    private var err_qr_unback = 0
    private var err_pr_low = 0
    private var err_pr_high = 0

    private var qyValue = 0

    val VOICE_MP3_AYBZ: Int = 2//按压不足
    val VOICE_MP3_AYGD: Int = 3//按压过大
    val VOICE_MP3_AYWZCW: Int = 4//按压位置错误
    val VOICE_MP3_CQBZ: Int = 5//吹气不足
    val VOICE_MP3_CQGD: Int = 6//吹气过大
    val VOICE_MP3_CQJW: Int = 7//吹气进胃
    val VOICE_MP3_WDKQD: Int = 8//未打开气道
    val VOICE_MP3_WHT: Int = 9//未回弹

    private lateinit var mDataCallback: DataCallback

    /**
     * 按压，吹气逻辑判断
     */
    fun processLogic(dataDTO: BaseDataDTO, configBean: ConfigBean, mDataCallback: DataCallback) {
        this.mDataCallback = mDataCallback
        //按压
        prLogic(dataDTO)
        //吹气
        qyLogic(dataDTO, configBean)
    }

    /**
     * 按压处理逻辑
     */
    private fun prLogic(dataDTO: BaseDataDTO) {
        val errorTotal =
            (dataDTO.ERR_PR_POSI + dataDTO.ERR_PR_LOW + dataDTO.ERR_PR_HIGH + dataDTO.ERR_PR_UNBACK)
        //执行三次按压深度
        if (dataDTO.prSum != prValue) {
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                mDataCallback.onPrCallback(VOICE_MP3_AYWZCW, errorTotal)
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                mDataCallback.onPrCallback(VOICE_MP3_WHT, errorTotal)
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    err_pr_low = dataDTO.ERR_PR_LOW
                    mDataCallback.onPrCallback(VOICE_MP3_AYBZ, errorTotal)
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                    mDataCallback.onPrCallback(VOICE_MP3_AYGD, errorTotal)
                }
            }
        }
        prValue = dataDTO.prSum
    }

    /**
     * 吹气状态
     */
    private fun qyLogic(dataDTO: BaseDataDTO, configBean: ConfigBean) {
        //通气道是否打开 0-关闭 1-打开
        val errorTotal =
            (dataDTO.ERR_QY_CLOSE + dataDTO.ERR_QY_HIGH + dataDTO.ERR_QY_LOW + dataDTO.ERR_QY_DEAD)
        if (dataDTO.aisleType == 1) {
            if (qyValue != dataDTO.qySum) {
                val qyMax = DataVolatile.max(false)
                when {
                    qyMax in configBean.qyLow()..configBean.qyHigh() -> {//通气正常

                    }
                    qyMax in configBean.qyHigh()..100 -> {//通气过大
                        mDataCallback.onQyCallback(VOICE_MP3_CQGD, errorTotal)
                    }
                    qyMax < configBean.qyLow() -> {//通气不足
                        mDataCallback.onQyCallback(VOICE_MP3_CQBZ, errorTotal)
                    }
                    qyMax > configBean.qy_max -> {//吹气进胃
                        mDataCallback.onQyCallback(VOICE_MP3_CQJW, errorTotal)
                    }
                }
            }
        } else {
            if (dataDTO.bpValue > 5) {
                mDataCallback.onQyCallback(VOICE_MP3_WDKQD, errorTotal)
            }
        }
        qyValue = dataDTO.qySum
    }

    /**
     * 数据处理结果回调
     */
    interface DataCallback {

        fun onPrCallback(type: Int, qrTotal: Int)

        fun onQyCallback(type: Int, qyTotal: Int)

        fun onCycleCount(count: Int)

    }

}