package com.pr.perfectrecovery.bean

import java.io.Serializable

/**
 * 训练、考核  基本配置属性
 */

data class ScoringConfigBean(
    var depth: Float = 0f,//深度start
    var depthEnd: Float = 0f,//深度end
    var depthFrequency: Int = 0,//深度频率开始
    var depthFrequencyEnd: Int = 0,//深度频率结束
    var tidalVolume: Int = 0,//吹气（潮气）开始
    var tidalVolumeEnd: Int = 0,//吹气（潮气）结束
    var tidalFrequency: Int = 0,//吹气（潮气）频率开始
    var tidalFrequencyEnd: Int = 0,//吹气（潮气）频率结束
    var interrupt: String = "",//中断
    var operationTime: String = "",//操作时长
    var cycles: Int = 0,//循环次数
    var qyCount: Int = 0,//吹气次数
    var prCount: Int = 0,//按压次数
    var process: Int = 0,//流程分数
    var compressions: Int = 0,//按压分数
    var ventilation: Int = 0,//吹气分数
    var deduction: Float = 0f//中断分数
) : Serializable {
    //按压比列计算
    var pr_High: Int = (depthEnd * 10).toInt()
    var pr_Low: Int = if (depth > 0) (depth * 10).toInt() else 0

    //吹气配置计算比列
    var qy_high = tidalVolumeEnd / 100 * 8
    var qy_low = tidalVolume / 100 * 8
    var qy_max = 100
}