package com.pr.perfectrecovery.activity

import android.os.Bundle
import com.blankj.utilcode.util.TimeUtils
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.databinding.ActivityTrainResultBinding

/**
 * 训练结果-操作明细
 */
class TrainResultActivity : BaseActivity() {
    private lateinit var viewBinding: ActivityTrainResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityTrainResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.ivExport.setOnClickListener { }
    }

    private fun setData() {
        viewBinding.tvName.text = ""
        viewBinding.tvTrain.text = ""
        viewBinding.tvTime.text = ""
    }
}