package com.pr.perfectrecovery.bean

import java.io.Serializable

/**
 * 训练、考核  基本配置属性
 */

data class ScoringConfigBean(
    var depth: Int = 0,//深度start
    var depthEnd: Int = 0,//深度end
    var depthFrequency: Int = 0,//深度频率开始
    var depthFrequencyEnd: Int = 0,//深度频率结束
    var tidalVolume: Int = 0,//吹气（潮气）开始
    var tidalVolumeEnd: Int = 0,//吹气（潮气）结束
    var tidalFrequency: Int = 0,//吹气（潮气）频率开始
    var tidalFrequencyEnd: Int = 0,//吹气（潮气）频率结束
    var interrupt: String = "",//中断
    var operationTime: String = "",//操作时长
    var cycles: Int = 0,//循环次数
    var cprRatio: Int = 0,//按压通气比列start
    var cprRatioEnd: Int = 0,//按压通气比列end

    var process: Int = 0,//流程分数
    var compressions: Int = 0,//按压分数
    var ventilation: Int = 0,//吹气分数
    var deduction: Float = 0f//中断分数
) : Serializable {

}