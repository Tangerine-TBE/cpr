package com.pr.perfectrecovery

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.JsonUtils
import com.pr.perfectrecovery.activity.CPRActivity
import com.pr.perfectrecovery.activity.ConfigActivity
import com.pr.perfectrecovery.activity.StatisticalActivity
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ScoringConfigBean
import com.pr.perfectrecovery.databinding.ActivityMainBinding
import com.tencent.mmkv.MMKV
import kotlin.system.exitProcess

/**
 * 首页
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
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
        val data = ScoringConfigBean()
        data.process = 5
        data.compressions = 50
        data.ventilation = 45
        data.deduction = 0.5f
        data.depth = -1
        data.depthEnd = 6
        data.depthFrequency = 100
        data.depthFrequencyEnd = 120
        data.tidalVolume = 400
        data.tidalVolumeEnd = 600
        data.tidalFrequency = 6
        data.tidalFrequencyEnd = 98
        data.interrupt = "02:00"
        //保存配置信息
        MMKV.defaultMMKV().encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(data))
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

