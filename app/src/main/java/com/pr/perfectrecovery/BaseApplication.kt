package com.pr.perfectrecovery

import android.app.Application
import android.hardware.usb.UsbManager
import cn.wch.ch34xuartdriver.CH34xUARTDriver
import com.tencent.mmkv.MMKV

class BaseApplication : Application() {
    // 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
    companion object {
        var driver: CH34xUARTDriver? = null
    }

    private val ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION"

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)

        driver = CH34xUARTDriver(
            getSystemService(USB_SERVICE) as UsbManager, this, ACTION_USB_PERMISSION
        )
    }
}