package com.pr.perfectrecovery

import java.io.Serializable

class TrainingBean() : Serializable {

    //考核人姓名
    var name: String = ""

    //是否开启语音提示
    var isVoice: Boolean = false

    //是否开启提示音
    var isBeat: Boolean = false

    //true 考核模式 false 练习模式
    var isCheck: Boolean = false

    //多人模式
    var list = mutableListOf<TrainingBean>()
}
