package com.pr.perfectrecovery.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object TimeUtils {
    /**
     * Android 音乐播放器应用里，读出的音乐时长为 long 类型以毫秒数为单位，例如：将 234736 转化为分钟和秒应为 03:55 （包含四舍五入）
     * @param duration 音乐时长
     * @return
     */
    fun timeParse(duration: Long): String? {
        var time: String? = ""
        val minute = duration / 60000
        val seconds = duration % 60000
        val second = (seconds.toFloat() / 1000).roundToInt().toLong()
        if (minute < 10) {
            time += "0"
        }
        time += "$minute:"
        if (second < 10) {
            time += "0"
        }
        time += second
        return time
    }

    /**
     * 将时间戳转换为时间
     *
     * s就是时间戳
     */
    fun stampToDate(milli: Long): String {
        try {
            val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
            //如果它本来就是long类型的,则不用写这一步
            val date = Date(milli)
            return simpleDateFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 将时间戳转换为时间
     *
     * s就是时间戳
     */
    fun formatDate(milli: Long): String {
        try {
            val simpleDateFormat = SimpleDateFormat("mm分ss秒")
            //如果它本来就是long类型的,则不用写这一步
            val date = Date(milli)
            return simpleDateFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 将时分秒转为毫秒数
     */
    fun formatTurnSecond(time: String): Long {
        val index1 = time.indexOf(":")
        val mi = time.substring(0, 2).toInt()
        val ss = time.substring(3, 5).toInt()
        //Log.e(TAG, "formatTurnSecond: 时间== " + hh * 60 * 60 + mi * 60 + ss)
        return ((mi * 60 + ss) * 1000).toLong()
    }


}