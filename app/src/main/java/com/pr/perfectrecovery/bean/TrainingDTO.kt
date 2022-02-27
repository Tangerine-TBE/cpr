package com.pr.perfectrecovery.bean

import org.litepal.crud.LitePalSupport
import java.io.Serializable

data class TrainingDTO(var name: String = "") : Serializable, LitePalSupport() {
    var id: Int = 0
    var isCheck = false//是否为 false 训练  true 考核
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
    var pressFrequency: Int = 0//按压频率

    var blowErrorCount: Int = 0//吹气错误数
    var blowTotal: Int = 0//吹气总数
    var blowClose: Int = 0//吹气气道错误数
    var blowLow: Int = 0//吹气不足错误数
    var blowHigh: Int = 0//吹气过大错误数
    var blowIntoStomach: Int = 0//吹气进胃错误数
    var blowAverage: Int = 0//吹气平均多少次每分数

    var prCount: Int = 0 //按压次数比例
    var qyCount: Int = 0 //吹气次数比例
    var processScore: Int = 0//流程分数
    var pressScore: Int = 0//按压分数
    var deduction: Float = 0f//扣分
    var blowScore: Int = 0//吹气、通气 分数

    //检查环境
    var check1 = false

    //检查意识
    var check2 = false

    //检查呼吸
    var check3 = false

    //检查脉搏
    var check4 = false

    //检查体位
    var check5 = false

    //清理口腔
    var check6 = false

    //拨打120
    var check7 = false

    //寻求帮助
    var check8 = false

    //体外除颤
    var check9 = false

    //完成评估
    var check10 = false

    /**
     * 按压频率合格率
     */
    fun getPressRate(): Int {
        return if (pressFrequency > 0 && pressTotal > 0) {
            pressFrequency / pressTotal
        } else {
            0
        }
    }

    /**
     * 按压回弹合格率
     */
    fun getReboundRate(): Int {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数
        return if ((pressTotal - pressErrorCount + pressLocation + pressHigh) > 0 && pressTotal > 0) {
            (pressTotal - pressErrorCount + pressLocation + pressHigh) / pressTotal
        } else {
            0
        }
    }

    /**
     * 按压深度合格率
     */
    fun getDepthRate(): Int {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数+深度错误的
        return if ((pressTotal - pressErrorCount + pressLocation + pressHigh + pressRebound) > 0 && pressTotal > 0) {
            (pressTotal - pressErrorCount + pressLocation + pressHigh + pressRebound) / pressTotal
        } else {
            0
        }
    }

    /**
     * 按压时间
     */
    fun getPressTime(): Int {
        return 0
    }

    /**
     * 吹气量合格率
     */
    fun getBlowAmount(): Int {
        return if ((blowTotal - blowErrorCount) > 0 && blowTotal > 0) {
            (blowTotal - blowErrorCount) / blowTotal
        } else {
            0
        }
    }

    /**
     * 吹气频率合格率
     */
    fun getBlowRate(): Int {
        return if ((blowTotal - blowErrorCount) > 0 && blowTotal > 0) {
            (blowTotal - blowErrorCount) / blowTotal
        } else {
            0
        }
    }

}