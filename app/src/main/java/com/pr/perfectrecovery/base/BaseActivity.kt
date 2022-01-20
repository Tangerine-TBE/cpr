package com.pr.perfectrecovery.base

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.pr.perfectrecovery.R

open class BaseActivity : AppCompatActivity() {

    var mActivity: Activity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBar(resources.getColor(R.color.theme_color))
        mActivity = this
    }

    private fun setStatusBar(color: Int) {
        val window: Window = window
        //After LOLLIPOP not translucent status bar
        //After LOLLIPOP not translucent status bar
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //Then call setStatusBarColor.
        //Then call setStatusBarColor.
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.theme_color)
        window.navigationBarColor = resources.getColor(R.color.theme_color)
    }
}