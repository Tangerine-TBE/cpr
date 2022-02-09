package com.pr.perfectrecovery.bean

import java.io.Serializable

data class TrainingDTO(val name: String = "") : Serializable {
    var trainingTime: String = ""//训练时长
    var cycleCount: Int = 0//循环次数
    var pressErrorCount: Int = 0//按压错误数
    var pressTotal: Int = 0//按压总数
    var pressLocation: Int = 0//按压位置错误数
    var pressLow: Int = 0//按压不足错误数
    var pressHigh: Int = 0//按压过大错误数
    var pressRebound: Int = 0//按压未回弹错误数
    var pressAverage: Int = 0//按压平均多少次每分数
    var pressOutTime: String = ""//按压超时
    var pressMeterPercentage: Int = 0//按压仪表百分比
    var pressTopPercentage: Int = 0//按压顶部百分比
    var pressBottomPercentage: Int = 0//按压底部百分比
    var pressAverageDepth: Int = 0//按压平均深度
    var pressCenterPercentage: Int = 0//按压百分比

    var blowErrorCount: Int = 0//吹气错误数
    var blowTotal: Int = 0//吹气总数
    var blowAirway: Int = 0//吹气气道错误数
    var blowLow: Int = 0//吹气不足错误数
    var blowHigh: Int = 0//吹气过大错误数
    var blowIntoStomach: Int = 0//吹气进胃错误数
    var blowAverage: Int = 0//吹气平均多少次每分数
    var blowMeterPercentage: Int = 0//吹气仪表百分比
    var blowPercentage: Int = 0//吹气百分比
    var blowAverageDepth: Int = 0//吹气平均深度
}