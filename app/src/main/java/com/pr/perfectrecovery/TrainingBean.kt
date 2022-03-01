package com.pr.perfectrecovery

import java.io.Serializable

class TrainingBean() : Serializable {

    //考核人姓名
    var name: String = ""

    //考核人使用的设备编号
    var count:Int = 0

    //是否开启语音提示
    var isVoice: Boolean = false

    //是否开启提示音
    var isBeat: Boolean = false

    //true 考核模式 false 练习模式
    var isCheck: Boolean = false

    //是否单模型
    var isSingle: Boolean = true

    //考核人使用设备的mac地址
    var mac:String = ""

    //多人模式
    var list = mutableListOf<TrainingBean>()

    override fun toString(): String {
        return "TrainingBean(name='$name', count=$count, isVoice=$isVoice, isBeat=$isBeat, isCheck=$isCheck, isSingle=$isSingle, mac='$mac', list=$list)"
    }
}
