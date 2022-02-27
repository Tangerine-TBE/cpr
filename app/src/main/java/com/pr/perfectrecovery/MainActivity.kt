package com.pr.perfectrecovery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.activity.CPRActivity
import com.pr.perfectrecovery.activity.ConfigActivity
import com.pr.perfectrecovery.activity.StatisticalActivity
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.ActivityMainBinding
import com.tencent.mmkv.MMKV
import kotlin.system.exitProcess

/**
 * 首页
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val BLUETOOTH_PERMISSIONS =
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        checkPermission()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(BLUETOOTH_PERMISSIONS, 0)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "请在设置中打开应用权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initView() {
        binding.tvVersion.text = getString(R.string.home_version, AppUtils.getAppVersionName())
        binding.tvCPR.setOnClickListener {
            startActivity(Intent(this, CPRActivity::class.java))
        }

        binding.tvStatistical.setOnClickListener {
            startActivity(Intent(this, StatisticalActivity::class.java))
        }

        binding.tvSetting.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }

        binding.tvExit.setOnClickListener {
            showExit()
        }
        initConfigData()
    }

    /**
     * 初始化设置
     */
    private fun initConfigData() {
        val decodeString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        if (TextUtils.isEmpty(decodeString)) {
            val data = ConfigBean()
            data.processScore = 5
            data.pressScore = 50
            data.blowScore = 45
            data.deductionScore = 1
            data.depth = 4
            data.depthEnd = 6
            data.depthFrequency = 100
            data.depthFrequencyEnd = 120
            data.tidalVolume = 400
            data.tidalVolumeEnd = 600
            data.tidalFrequency = 6
            data.tidalFrequencyEnd = 8
            data.operationTime = 120
            data.interruptTime = 30
            data.cycles = 5
            data.prCount = 30
            data.qyCount = 2
            //保存配置信息
            MMKV.defaultMMKV().encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(data))
        }
    }

    private fun showExit() {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("是否退出")
        dialog.setPositiveButton("退出") { dialog, which -> exitProcess(0) }
        dialog.setNegativeButton("取消") { dialog, which -> dialog.dismiss() }
        dialog.create()
        dialog.show()
    }
}

