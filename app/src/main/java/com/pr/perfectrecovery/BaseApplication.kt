package com.pr.perfectrecovery

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.mmkv.MMKV
import org.litepal.LitePal

class BaseApplication : Application() {

    // 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
    companion object {
        var mInstance: BaseApplication? = null
        fun instance(): BaseApplication? {
            return mInstance
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        MMKV.initialize(this)
        LitePal.initialize(this)
        //Multidex.install(this);
        CrashReport.initCrashReport(applicationContext, "0bd5e51ccc", false);
    }
}