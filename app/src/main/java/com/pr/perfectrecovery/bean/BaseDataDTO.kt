package com.pr.perfectrecovery.bean

data class BaseDataDTO(
    //mac地址
    var mac: String = "",
    var preDistance: Int = 0,//初始值
    // 吹气总和
    var qyValueSum: Int = 0,
    //电量值：  0-100%
    var electricity: Int = 0,
    //距离值：  30-150
    var distance: Int = 0,
    //气压值：  0-2000ml
    var bpValue: Int = 0,
    //蓝牙连接状态：   0-断开 1-连接
    var blsType: Int = 0,
    //USB连接状态: 0-断开 1-连接
    var usbConnectType: Int = 0,
    //通道打开状态 0-关闭 1-打开
    var aisleType: Int = 0,
    //连接方式  0-蓝牙 1-连接USB
    var connectType: Int = -1,
    //按压位置正确  0-错误  1-正确
    var psrType: Int = -1,
    //工作方式：0——休眠   1——工作    2——待机
    var workType: Int = -1,
    //按压频率：0-200
    var pf: Int = 0,
    //吹气频率：0-200
    var cf: Int = 0,
    //按压次数
    var prSum: Int = 0,
    //吹起次数
    var qySum: Int = 0,
    //按压错误-未回弹
    var err_pr_unback: Int = 0,
    //按压错误-按压不足
    var err_pr_low: Int = 0,
    //按压错误-按压过大
    var err_pr_high: Int = 0,
    //按压错误-按压位置错误
    var err_pr_posi: Int = 0,
    //吹气错误-气压不足
    var err_qy_low: Int = 0,
    //吹气错误-气压过大
    var err_qy_high: Int = 0,
    //吹气错误-气压进胃
    var err_qy_dead: Int = 0,
    //吹气错误-气道未打开错误
    var err_qy_close: Int? = null,
    var QY_TIMES_TOOMORE: Int = 0,
    var ERR_PR_TOOMORE: Int = 0,
    var pr_depth_sum: Int = 0,//按压深度总和(mm)
    var pr_time_sum: Int = 0,   // 按压时间总和（ms）
    var qy_volume_sum: Int = 0,  //吹气量总和
    var qy_time_sum: Int = 0,  //吹气时间总和
    var qy_max_volume_sum: Int = 0,//吹气每次最值大总和
    var pr_seqright_total: Int = 0, //按压频率正常的次数
    var qy_serright_total: Int = 0,//吹气频率正确的次数
    var qyMaxValue: Int = 0,//吹气最大值
    var qy_blow_error_count: Int = 0,//吹气率错误次数
    var model: Boolean = false,//模式
    var PR_CYCLE_TIMES: Int = 0
) {
    var L_d1 = 0
    var L_d2 = 0
    var L_d3 = 0

    //是否开始
    var isStart = false
    override fun toString(): String {
        return "BaseDataDTO(mac='$mac', distance=$distance, bpValue=$bpValue, pf=$pf, cf=$cf, prSum=$prSum, qySum=$qySum, PR_SEQRIGHT_TOTAL=$pr_seqright_total, QY_SERRIGHT_TOTAL=$qy_serright_total)"
    }

    /**
     * 获取按压错误总数
     */
    fun getPr_err_total(): Int {
        return err_pr_unback + err_pr_low + err_pr_high + err_pr_posi + ERR_PR_TOOMORE
    }

    /**
     * 获取吹气总数
     */
    fun getQy_err_total(): Int {
        return err_qy_close ?: 0 + err_qy_low + err_qy_dead + err_qy_high + QY_TIMES_TOOMORE
    }

    var PR_HIGH_VALUE: Int = 0
    var PR_LOW_VALUE: Int = 0
}
