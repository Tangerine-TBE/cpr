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
                && oldItem.err_pr_unback == newItem.err_pr_unback
                && oldItem.err_pr_low == newItem.err_pr_low
                && oldItem.err_pr_high == newItem.err_pr_high
                && oldItem.err_pr_posi == newItem.err_pr_posi
                && oldItem.err_qy_low == newItem.err_qy_low
                && oldItem.err_qy_high == newItem.err_qy_high
                && oldItem.err_qy_close == newItem.err_qy_close
                && oldItem.pr_depth_sum == newItem.pr_depth_sum
                && oldItem.pr_time_sum == newItem.pr_time_sum
                && oldItem.qy_volume_sum == newItem.qy_volume_sum
                && oldItem.qy_time_sum == newItem.qy_time_sum
                && oldItem.pr_seqright_total == newItem.pr_seqright_total
                && oldItem.qy_serright_total == newItem.qy_serright_total
                && oldItem.isStart == newItem.isStart
    }
}