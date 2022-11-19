package com.pr.perfectrecovery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import cn.wch.ch34xuartdriver.CH34xUARTDriver
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.mmkv.MMKV
import me.jessyan.autosize.AutoSize
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.onAdaptListener
import me.jessyan.autosize.utils.AutoSizeLog
import org.litepal.LitePal
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class BaseApplication : Application() {

    // 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
    companion object {
        var mInstance: BaseApplication? = null
        var driver: CH34xUARTDriver? = null
        fun instance(): BaseApplication? {
            return mInstance
        }
    }


    private val ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION"
    private fun isPad(): Boolean {

        val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay;
        val dm = DisplayMetrics();
        display.getMetrics(dm)
        val x = (dm.widthPixels / dm.xdpi).toDouble().pow(2.0)
        val y = (dm.heightPixels / dm.ydpi).toDouble().pow(2.0)
        val screenInches = sqrt(x + y)
        return screenInches >= 7.0

    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        MMKV.initialize(this)
        LitePal.initialize(this)
        //Multidex.install(this);
        CrashReport.initCrashReport(applicationContext, "0bd5e51ccc", false);
        driver = CH34xUARTDriver(
            getSystemService(USB_SERVICE) as UsbManager, this, ACTION_USB_PERMISSION
        )
        /*初始化动态加载方案*/
        /*Manifest 的 手机适配width为 375dp  height为780dp*/
        /*平板适配width为500dp height为900dp*/
        /*默认在AndroidManifest文件当中适配为平板*/
        var height = 750
        var width = 375
        if (isPad()) {
            height = 900
            width = 500
        }

        AutoSize.initCompatMultiProcess(this)
        AutoSizeConfig.getInstance().setDesignHeightInDp(height)
            .setDesignWidthInDp(width).onAdaptListener = object : onAdaptListener {
            override fun onAdaptBefore(target: Any?, activity: Activity?) {
            }

            override fun onAdaptAfter(target: Any?, activity: Activity?) {
            }
        }

    }
}