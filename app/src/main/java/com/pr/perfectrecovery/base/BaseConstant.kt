package com.pr.perfectrecovery.base

object BaseConstant {
    //更新展示学员的时间间隔
    const val INTERVAL_TIME = 5000

    //学员信息
    const val TRAINING_BEAN = "TRAINING_BEAN"
    //连接的蓝牙设备数据
    const val CONNECT_BLE_DEVICES = "CONNECT_BLE_DEVICES"
    //配置
    val MMKV_WM_CONFIGURATION: String = "mmkv_wm_configuration"

    val MMKV_WM_ = 1

    val EVENT_SINGLE_CHART_START = 10000
    val EVENT_SINGLE_CHART_STOP = 10001

    val EVENT_SINGLE_DATA_REFRESH = 10002
    val EVENT_SINGLE_DATA_CYCLE = 10003
    val EVENT_CPR_START = 10004
    val EVENT_CPR_STOP = 10005
    val EVENT_CPR_DISCONNENT = 10006
    val EVENT_CPR_TIMEING = 10007
    val EVENT_CPR_CLEAR = 10008
    val EVENT_SINGLE_END = 10009
    //假的蓝牙地址
    val FAKE_MAC = "FAKE_MAC"
    val CLEAR_DEVICE_HISTORY_DATA = 10008
    val DEVICE_DISCONNECTED = 10009

}