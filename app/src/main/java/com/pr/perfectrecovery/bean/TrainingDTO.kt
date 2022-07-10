package com.pr.perfectrecovery.bean

import org.litepal.crud.LitePalSupport
import java.io.Serializable
import java.math.RoundingMode
import java.nio.channels.FileLock
import java.text.DecimalFormat
import kotlin.math.roundToInt

data class TrainingDTO(var name: String = "") : Serializable, LitePalSupport() {
    var id: Long = 0
    var isCheck = false//是否为 false 训练  true 考核
    var startTime: Long = 0//开始时间
    var endTime: Long = 0//开始时间
    var pr_depth_sum = 0  //按压深度总和(mm)
    var pr_time_sum: Int = 0 // 按压时间总和（ms）
    var qy_volume_sum = 0  //吹气量总和
    var qy_max_volume_sum: Int = 0//吹气每次最值大总和
    var qy_time_sum: Int = 0//吹气时间总和
    var pr_seqright_total = 0 //按压频率正常的次数
    var qy_serright_total = 0 //吹气频率正确的次数
    var timeTotal: Long = 0//总时长
    var operateTime: Long = 0//训练操作时间
    var cycleCount: Int = 0//循环次数
    var pressErrorCount: Int = 0//按压错误数
    var prSum: Int = 0//按压总数
    var err_pr_posi: Int = 0//按压位置错误数
    var err_pr_low: Int = 0//按压不足错误数
    var err_pr_high: Int = 0//按压过大错误数
    var err_pr_unback: Int = 0//按压未回弹错误数
    var timeOutTotal: Long = 0//按压超时
    var blowErrorCount: Float = 0f//吹气错误总数
    var qySum: Int = 0//吹气总数
    var err_qy_close: Int = 0//吹气气道错误数
    var err_qy_low: Int = 0//吹气不足错误数
    var err_qy_high: Int = 0//吹气过大错误数
    var err_qy_dead: Int = 0//吹气进胃错误数

    //设置的数据
    var prCount: Int = 0 //按压次数比例
    var qyCount: Int = 0 //吹气次数比例
    var cycles: Int = 0//设置的循环次数
    var processScore: Float = 0f//流程分数
    var pressScore: Float = 0f//按压分数
    var deduction: Float = 0f//扣分
    var blowScore: Float = 0f//吹气、通气 分数

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
     * 对入参保留最多两位小数(舍弃末尾的0)，如:
     * 3.345->3.34
     * 3.40->3.4
     * 3.0->3
     */
    private fun getNoMoreThanTwoDigits(number: Float): String {
        val format = DecimalFormat("0.#")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(number)
    }

    /**
     * 获取总得分
     */
    fun getScoreTotal(): Float {
        var scoreTotal: Float = getQyScore() + getPrScore() + getCheckSize()
        if (scoreTotal > getTimeOutScore()) {
            scoreTotal -= getTimeOutScore()
        } else {
            scoreTotal = 0.0f
        }
        return scoreTotal
    }

    private fun getCheckSize(): Float {
        val listCheck = mutableListOf<Boolean>()
        if (check1) {
            listCheck.add(check1)
        }
        if (check2) {
            listCheck.add(check2)
        }
        if (check3) {
            listCheck.add(check3)
        }
        if (check4) {
            listCheck.add(check4)
        }
        if (check5) {
            listCheck.add(check5)
        }
        if (check6) {
            listCheck.add(check6)
        }
        if (check7) {
            listCheck.add(check7)
        }
        if (check8) {
            listCheck.add(check8)
        }
        if (check9) {
            listCheck.add(check9)
        }
        if (check10) {
            listCheck.add(check10)
        }
        return if (processScore > 0 && listCheck.size > 0)
            ((processScore / 10) * listCheck.size)
        else
            0.0f
    }

    /**
     * 按压频率合格率
     *  按压频率合格率 = 正确按压频率次数 / 总按压次数
     */
    fun getPressRate(): Int {
        return if (prSum > 0 && pr_seqright_total > 0) ((pr_seqright_total / prSum.toFloat()) * 100).roundToInt() else 0
    }

    /**
     * 按压回弹合格率
     * 回弹合格率=（总按压次数-未回弹次数）/总按压次数
     */
    fun getReboundRate(): Int {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数
        return if (prSum - err_pr_unback > 0 && prSum > 0) (((prSum - err_pr_unback) / prSum.toFloat()) * 100).roundToInt() else 0
    }

    /**
     * 按压深度合格率
     * 按压深度合格率 = 正确按压次数 / 总按压次数
     */
    fun getDepthRate(): Int {
        //总数=正确按压次数+位置错误按压次数+未回弹按压次数+深度错误的
        return if ((prSum - pressErrorCount) > 0 && prSum > 0) (((prSum - pressErrorCount) / prSum.toFloat()) * 100).roundToInt() else 0
    }

    /**
     * 按压平均次数/分
     * 按压平均次数/分 = 按压总次数/按压总时间ms * 60
     */
    fun getPressAverageTimes(): Int {
        return if (prSum > 0 && pr_time_sum > 0) (prSum.toFloat() / (pr_time_sum.toFloat() / 1000) * 60).roundToInt() else 0
    }

    /**
     * 按压平均深度
     * 按压平均次数/分 = 按压总深度/按压总次数
     */
    fun getPressAverageDepth(): Int {
        return if (prSum > 0 && pr_depth_sum > 0) ((pr_depth_sum.toFloat() / prSum.toFloat()) / 1.1).roundToInt() else 0
    }

    /**
     * 通气量合格率
     * 通气合格率 = 正确通气量次数 / 通气总次数
     */
    fun getBlowAmount(): Int {
        return if ((qySum - blowErrorCount) > 0 && qySum > 0) ((qySum - blowErrorCount) / qySum * 100).roundToInt() else 0
    }

    /**
     * 吹气频率合格率
     */
    fun getBlowRate(): Int {
        return if ((qySum - blowErrorCount) > 0 && qySum > 0) ((qySum - blowErrorCount) / qySum * 100).roundToInt() else 0
    }

    /**
     *  平均通气平均值
     *  平均通气平均值 = 通气总量 / 通气总次数
     */
    fun getBlowAverageNumber(): Int {
        return if (qySum > 0 && qy_max_volume_sum > 0) qy_max_volume_sum / qySum else 0
    }

    /**
     *  平均通气平均值
     *  总时间/总次数 = 单次时间
     *  平均通气平均值 = 通气总次数 / 通气总时间
     */
    fun getBlowAverage(): Int {
        return if (qySum > 0 && qy_time_sum > 0) (60000 / (qy_time_sum / qySum.toFloat())).roundToInt() else 0
    }

    /**
     * 按压分数
     * 正确按压次数 * 按压设定分数 / 按压设定总数 - 超次扣分
     */
    fun getPrScore(): Float {
        var value = 0f
        if (prSum > 0 && (prSum - pressErrorCount) > 0) {
            value =
                (prSum - pressErrorCount) * pressScore / (prCount * cycles).toFloat() - getPrManyScore()
        }
        return if (value > 0) value else 0.0f
    }

    /**
     * 通气分数
     * 正确吹气次数 * 吹气设定分数 / 吹气设定总数
     */
    fun getQyScore(): Float {
        var value = 0f
        if (qySum > 0 && (qySum - blowErrorCount) > 0) {
            value = ((qySum - blowErrorCount) * (blowScore / (qyCount * cycles))) - getQyManyScore()
        }
        return if (value > 0) value else 0f
    }

    /**
     * 中断扣分 超时扣分
     */
    fun getTimeOutScore(): Float {
        return if (timeOutTotal > 0 && deduction > 0) (timeOutTotal / 1000 * deduction) else 0f
    }

    /**
     * 按压时间百分比
     * 按压时间/（结束操作的时间-开始操作模型的时间）
     */
    fun getPressTime(): Int {
        return if (pr_time_sum > 0) (pr_time_sum.toFloat() / (endTime - startTime) * 100).roundToInt() else 0
    }

    /**
     * 超次少次扣分
     */
    fun getPrManyScore(): Float {
        return (pressScore / (prCount * cycles.toFloat())) * (prManyCount + prLessCount)
    }

    fun getQyManyScore(): Float {
        return (blowScore.toFloat() / (qyCount * cycles.toFloat())) * (qyManyCount + qyLessCount)
    }

}