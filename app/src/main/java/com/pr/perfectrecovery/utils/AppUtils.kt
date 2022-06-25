package com.pr.perfectrecovery.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import kotlin.math.pow

object AppUtils {

    /**
     * 是否是平板
     *
     * @param context 上下文
     * @return 是平板则返回true，反之返回false
     */
    fun isPad(context: Context): Boolean {
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.getDefaultDisplay()
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val x = (dm.widthPixels / dm.xdpi).toDouble().pow(2.0)
        val y = (dm.heightPixels / dm.ydpi).toDouble().pow(2.0)
        val screenInches = Math.sqrt(x + y) // 屏幕尺寸
        return screenInches >= 7.0
    }
}