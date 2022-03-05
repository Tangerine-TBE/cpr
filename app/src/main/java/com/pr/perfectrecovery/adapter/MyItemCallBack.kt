package com.pr.perfectrecovery.adapter

import android.text.TextUtils
import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import com.pr.perfectrecovery.bean.BaseDataDTO

/**
 * desc   :
 * author : hunger
 * date   : 2022/3/4
 * version: 1.0
 */
class MyItemCallBack : DiffUtil.ItemCallback<BaseDataDTO>() {

    override fun areItemsTheSame(@NonNull oldItem: BaseDataDTO, @NonNull newItem: BaseDataDTO): Boolean {
        return TextUtils.equals(oldItem.mac, newItem.mac)
    }

    override fun areContentsTheSame(@NonNull oldItem: BaseDataDTO, @NonNull newItem: BaseDataDTO): Boolean {
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