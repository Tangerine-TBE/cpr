package com.pr.perfectrecovery.bean

import com.pr.perfectrecovery.base.BaseConstant
import com.tencent.mmkv.MMKV
import java.io.Serializable

/**
 * 训练、考核  基本配置属性
 */

data class ConfigBean(
    var depth: Int = 0,//深度start
    var depthEnd: Int = 0,//深度end
    var depthFrequency: Int = 0,//深度频率开始
    var depthFrequencyEnd: Int = 0,//深度频率结束
    var tidalVolume: Int = 0,//吹气（潮气）开始
    var tidalVolumeEnd: Int = 0,//吹气（潮气）结束
    var tidalFrequency: Int = 0,//吹气（潮气）频率开始
    var tidalFrequencyEnd: Int = 0,//吹气（潮气）频率结束
    var interruptTime: Int = 0,//中断
    var operationTime: Int = 0,//操作时长
    var cycles: Int = 0,//循环次数
    var qyCount: Int = 0,//吹气次数
    var prCount: Int = 0,//按压次数
    var processScore: Int = 0,//流程分数
    var pressScore: Int = 0,//按压分数
    var blowScore: Int = 0,//吹气分数
    var deductionScore: Float = 0f//中断分数
) : Serializable {
    //按压比列计算
    fun prHigh(): Int {
        val model = MMKV.defaultMMKV().getBoolean(BaseConstant.MMKV_MODEL, false)
        return if (model) {
            (depthEnd * 10).toInt()
        } else {
            (depthEnd * 10 * 1.2).toInt()
        }
    }

    fun prLow(): Int {
        return if (depth > 0) (depth * 10).toInt() else 0
    }

    //吹气配置计算比列
    fun qyHigh(): Int {
        return tidalVolumeEnd / 100 * 8
    }

    fun qyLow(): Int {
        return tidalVolume / 100 * 8
    }

    var qy_max = 100

    fun getTime(value: Int): Long {
        return (value * 1000).toLong()
    }
}