package com.pr.perfectrecovery.bean

data class BaseDataDTO(
    //mac地址
    var mac: String = "",
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
    var ERR_PR_UNBACK: Int = 0,
    //按压错误-按压不足
    var ERR_PR_LOW: Int = 0,
    //按压错误-按压过大
    var ERR_PR_HIGH: Int = 0,
    //按压错误-按压位置错误
    var ERR_PR_POSI: Int = 0,
    //吹气错误-气压不足
    var ERR_QY_LOW: Int = 0,
    //吹气错误-气压过大
    var ERR_QY_HIGH: Int = 0,
    //吹气错误-气压进胃
    var ERR_QY_DEAD: Int = 0,
    //吹气错误-气道未打开错误
    var ERR_QY_CLOSE: Int = 0,
    var PR_DEPTH_SUM: Int = 0,//按压深度总和(mm)
    var PR_TIME_SUM: Int = 0,   // 按压时间总和（ms）
    var QY_VOLUME_SUM: Int = 0,  //吹气量总和
    var QY_TIME_SUM: Int = 0,  //吹气时间总和
    var PR_SEQRIGHT_TOTAL: Int = 0, //按压频率正常的次数
    var QY_SERRIGHT_TOTAL: Int = 0 //吹气频率正确的次数
) {
    //是否开始
    var isStart = false

}