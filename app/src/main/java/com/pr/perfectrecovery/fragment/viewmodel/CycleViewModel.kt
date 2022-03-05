package com.pr.perfectrecovery.fragment.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.utils.DataVolatile

class CycleViewModel : ViewModel() {
    /*按压结果类*/
    var dataDTO = MutableLiveData<BaseDataDTO>()

    /*反馈类型*/
    var type = MutableLiveData<Int>()

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

    /**
     * 按压处理逻辑
     */
    fun prLogic(dataDTO: BaseDataDTO) {
        //执行三次按压深度
        if (dataDTO.prSum != prValue) {
            prValue = dataDTO.prSum
            //按压位置错误
            if (err_pr_posi != dataDTO.ERR_PR_POSI && dataDTO.psrType == 0) {
                err_pr_posi = dataDTO.ERR_PR_POSI
                type.value = VOICE_MP3_AYWZCW
            } else if (err_qr_unback != dataDTO.ERR_PR_UNBACK) {
                //按压未回弹
                err_qr_unback = dataDTO.ERR_PR_UNBACK
                type.value = VOICE_MP3_WHT
            } else {
                //按压不足
                if (err_pr_low != dataDTO.ERR_PR_LOW) {
                    err_pr_low = dataDTO.ERR_PR_LOW
                    type.value = VOICE_MP3_AYBZ
                } else if (err_pr_high != dataDTO.ERR_PR_HIGH) {//按压过大
                    err_pr_high = dataDTO.ERR_PR_HIGH
                    type.value = VOICE_MP3_AYGD
                }
            }
        }
    }

    /**
     * 吹气状态
     */
    private fun qy(dataDTO: BaseDataDTO, configBean: ConfigBean) {
        //通气道是否打开 0-关闭 1-打开
        if (dataDTO.aisleType == 1) {
//            viewBinding.ivAim.visibility = View.INVISIBLE
            if (qyValue != dataDTO.qySum) {
                val qyMax = DataVolatile.max(DataVolatile.QY_valueSet, false)
                when {
                    qyMax in configBean.qyLow()..configBean.qyHigh() -> {//通气正常
//                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_green)
                    }
                    qyMax in configBean.qyHigh()..100 -> {//通气过大
//                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_red)
//                        setPlayVoice(VOICE_MP3_CQGD)
                        type.value = VOICE_MP3_CQGD
                    }
                    qyMax < configBean.qyLow() -> {//通气不足
//                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_yello)
//                        setPlayVoice(VOICE_MP3_CQBZ)
                        type.value = VOICE_MP3_CQBZ
                    }
                    qyMax > configBean.qy_max -> {//吹气进胃
//                        viewBinding.ivLung.setImageResource(R.mipmap.icon_wm_lung_heart)
//                        setPlayVoice(VOICE_MP3_CQJW)
                        type.value = VOICE_MP3_CQJW
                    }
                }
//                //吹气变灰
//                mHandler1.removeCallbacks(blowRunnable)
//                mHandler1.postDelayed(blowRunnable, 2000)
//                stopOutTime()
            }
        } else {
            if (dataDTO.bpValue > 5) {
                type.value = VOICE_MP3_WDKQD
            }
        }

    }

    /**
     * 数据处理结果回调
     */
    interface DataCallback {

        fun onPrCallback(type: Int)

        fun onQyCallback(type: Int)

        fun onCycleCount(count: Int)

        fun onOutTime(time: Long)
    }

}