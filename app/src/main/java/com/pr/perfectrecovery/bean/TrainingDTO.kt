package com.pr.perfectrecovery.bean

import org.litepal.crud.LitePalSupport
import java.io.Serializable

data class TrainingDTO(var name: String = "") : Serializable, LitePalSupport() {
    var id: Long = 0
    var isCheck = false//是否为 false 训练  true 考核
    var startTime: Long = 0//开始时间
    var endTime: Long = 0//开始时间

    var pr_depth_sum = 0  //按压深度总和(mm)
    var pr_time_sum = 0    // 按压时间总和（ms）
    var qy_volume_sum = 0  //吹气量总和
    var qy_time_sum = 0     //吹气时间总和
    var pr_seqright_total = 0 //按压频率正常的次数
    var qy_serright_total = 0 //吹气频率正确的次数

    var timeTotal: Long = 0//总时长
    var operateTime: Long = 0//训练操作时间
    var cycleCount: Int = 0//循环次数
    var pressErrorCount: Int = 0//按压错误数
    var pressTotal: Int = 0//按压总数
    var pressLocation: Int = 0//按压位置错误数
    var pressLow: Int = 0//按压不足错误数
    var pressHigh: Int = 0//按压过大错误数
    var pressRebound: Int = 0//按压未回弹错误数
    var pressOutTime: Long = 0//按压超时

    var blowErrorCount: Int = 0//吹气错误总数
    var blowTotal: Int = 0//吹气总数
    var blowClose: Int = 0//吹气气道错误数
    var blowLow: Int = 0//吹气不足错误数
    var blowHigh: Int = 0//吹气过大错误数
    var blowIntoStomach: Int = 0//吹气进胃错误数

    var prCount: Int = 0 //按压次数比例
    var qyCount: Int = 0 //吹气次数比例
    var processScore: Int = 0//流程分数
    var pressScore: Int = 0//按压分数
    var deduction: Float = 0f//扣分
    var blowScore: Int = 0//吹气、通气 分数

    var prManyCount: Int = 0//按压多次
    var prLessCount: Int = 0//按压少次
    var qyManyCount: Int = 0//吹气多次
    var qyLessCount: Int = 0//吹气少次

    var score: Float = 0f//成绩分数

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

    var isCheckBox = false

    /**
     * 按压频率合格率
     *  按压频率合格率 = 正确按压频率次数 / 总按压次数
     */
    fun getPressRate(): Int {
        return if (pressTotal > 0 && pr_seqright_total > 0) {
            (pr_seqright_total / pressTotal * 100)
        } else {
            0
        }
    }

    /**
     * 按压回弹合格率
     * 回弹合格率=（总按压次数-未回弹次数）/总按压次数
     */
    fun getReboundRate(): Float {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数
        return if ((pressTotal - pressRebound) > 0 && pressTotal > 0) {
            (((pressTotal - pressRebound) / pressTotal).toFloat() * 100)
        } else {
            0f
        }
    }

    /**
     * 按压深度合格率
     * 按压深度合格率 = 正确按压次数 / 总按压次数
     */
    fun getDepthRate(): Int {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数+深度错误的
        return if ((pressTotal - pressErrorCount) > 0 && pressTotal > 0) {
            ((pressTotal - pressErrorCount) / pressTotal * 100)
        } else {
            0
        }
    }

    /**
     * 按压平均次数/分
     * 按压平均次数/分 = 按压总次数/按压总时间
     */
    fun getPressAverageTimes(): Int {
        return if (pressTotal > 0 && pr_time_sum > 0) {
            pressTotal / pr_time_sum
        } else {
            0
        }
    }

    /**
     * 按压平均深度
     * 按压平均次数/分 = 按压总深度/按压总次数
     */
    fun getPressAverageDepth(): Int {
        return if (pressTotal > 0 && pr_depth_sum > 0) {
            pr_depth_sum / pressTotal
        } else {
            0
        }
    }

    /**
     * 通气量合格率
     * 通气合格率 = 正确通气量次数 / 通气总次数
     */
    fun getBlowAmount(): Int {
        return if ((blowTotal - blowErrorCount) > 0 && blowTotal > 0) {
            ((blowTotal - blowErrorCount) / blowTotal * 100)
        } else {
            0
        }
    }

    /**
     * 吹气频率合格率
     */
    fun getBlowRate(): Int {
        return if ((blowTotal - blowErrorCount) > 0 && blowTotal > 0) {
            ((blowTotal - blowErrorCount) / blowTotal * 100)
        } else {
            0
        }
    }

    /**
     *  平均通气每分钟次数
     *  平均通气每分钟次数 = 通气总次数 / 通气总时间
     */
    fun getBlowAverageNumber(): Int {
        return if (blowTotal > 0 && qy_volume_sum > 0) {
            qy_volume_sum / blowTotal
        } else {
            0
        }
    }

    /**
     *  平均通气平均值
     *  平均通气平均值 = 通气总量 / 通气总次数
     */
    fun getBlowAverage(): Int {
        return if (blowTotal > 0 && qy_serright_total > 0) {
            blowTotal / qy_time_sum
        } else {
            0
        }
    }

    /**
     * 按压分数
     */
    fun getPrScore(): Float {
        return if (pressTotal > 0 && prCount > 0) {
            (pressTotal / prCount).toFloat()
        } else {
            0f
        }
    }

    /**
     * 通气分数
     */
    fun getQyScore(): Float {
        return if (blowTotal > 0 && qyCount > 0) {
            (blowTotal / qyCount).toFloat()
        } else {
            0f
        }
    }

    /**
     * 中断扣分 超时扣分
     */
    fun getTimeOutScore(): Float {
        return if (pressOutTime > 0 && deduction > 0) {
            (pressOutTime / 1000 * deduction)
        } else {
            0f
        }
    }

    /**
     * 按压时间百分比
     * 按压时间/（结束操作的时间-开始操作模型的时间）
     */
    fun getPressTime(): Int {
        return if (pr_time_sum > 0)
            ((pr_time_sum / (endTime - startTime)).toInt() * 100)
        else
            0
    }

}