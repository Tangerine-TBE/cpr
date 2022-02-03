package com.pr.perfectrecovery.bean

data class BaseDataDTO(
    var electricity: Int = 0,//电量值：  0-100%
    var distance: Int = 0,//距离值：  30-150
    var bpValue: Int = 0,//气压值：  0-2000ml
    var blsType: Int = 0,//蓝牙连接状态：   0-断开 1-连接
    var usbConnectType: Int = 0, //USB连接状态: 0-断开 1-连接
    var aisleType: Int = 0,//通道打开状态 0-关闭 1-打开
    var connectType: Int = -1,//连接方式  0-蓝牙 1-连接USB
    var psrType: Int = -1,//按压位置正确  0-错误  1-正确
    var workType: Int = -1,//工作方式：0——休眠   1——工作    2——待机
    var pf: Int = 0,//按压频率：0-200
    var cf: Int = 0,//吹气频率：0-200
    var prSum: Int = 0,//按压次数
    var qySum: Int = 0,//吹起次数
    var pressInterrupt: Boolean = false//按压中断状态
) {

}