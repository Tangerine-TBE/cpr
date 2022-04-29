package com.pr.perfectrecovery.base

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
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

    private var alertDialog: AlertDialog? = null

    open fun showLoadingDialog() {
        alertDialog = AlertDialog.Builder(this).create()
        alertDialog!!.window!!.setBackgroundDrawable(ColorDrawable())
        alertDialog!!.setCancelable(false)
        alertDialog!!.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event -> keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK })
        alertDialog!!.show()
        alertDialog!!.setContentView(R.layout.loading_alert)
        alertDialog!!.setCanceledOnTouchOutside(false)
    }

    open fun hideLoadingDialog() {
        if (null != alertDialog && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }
}