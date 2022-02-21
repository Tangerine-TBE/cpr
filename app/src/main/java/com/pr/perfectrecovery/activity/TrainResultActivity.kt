package com.pr.perfectrecovery.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivityTrainResultBinding
import com.pr.perfectrecovery.utils.TimeUtils

/**
 * 训练结果-操作明细 成绩结果
 * Time 2022年2月8日22:14:36
 * author lrz
 */
val DATADTO = "dataDTO"

class TrainResultActivity : BaseActivity() {
    private lateinit var viewBinding: ActivityTrainResultBinding

    companion object {
        fun start(context: Context, trainingDTO: TrainingDTO) {
            val intent = Intent(context, TrainResultActivity::class.java)
            intent.putExtra(DATADTO, trainingDTO)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityTrainResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
        initData()
    }

    private fun initView() {
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.ivExport.setOnClickListener { }
        viewBinding.bottom.ivExport.visibility = View.INVISIBLE
    }

    private fun initData() {
        val trainingDTO = intent.getSerializableExtra(DATADTO) as TrainingDTO
        viewBinding.tvName.text = trainingDTO.name
        viewBinding.tvTrain.text = "训练"
        viewBinding.tvTime.text = trainingDTO.trainingTime

        //循环次数
        viewBinding.tvCycleCount.text = "${trainingDTO.cycleCount}"

        //按压错误数
        viewBinding.tvLungCount.text = "${trainingDTO.pressErrorCount}"
        //按压总数
        viewBinding.tvLungTotal.text = "/${trainingDTO.pressTotal}"
        //按压位置错误
        viewBinding.tvLocation.text = "${trainingDTO.pressLocation}"
        //按压不足
        viewBinding.tvInsufficient.text = "${trainingDTO.pressLow}"
        //按压过大
        viewBinding.tvPressBig.text = "${trainingDTO.pressHigh}"
        //按压未回弹
        viewBinding.tvRebound.text = "${trainingDTO.pressRebound}"
        //按压超时统计时间
        viewBinding.tvPressTime.text = "${trainingDTO.pressOutTime}"
        //平均每分钟按压次数
        viewBinding.tvAverageCount.text = "${trainingDTO.pressAverage}"
        //按压百仪表分比
        if (trainingDTO.pressTotal > 0) {
            viewBinding.tvClock1.text = "${(trainingDTO.pressFrequency / trainingDTO.pressTotal)}%"
        }
        //按压百分比
        viewBinding.tvPress.text = "${trainingDTO.pressTopPercentage}"
        viewBinding.tvPressEnd.text = "${trainingDTO.pressBottomPercentage}"
        //按压平均深度
        viewBinding.tvPressBottom.text = "${trainingDTO.pressAverageDepth}"
        //整体按压百分比
        viewBinding.tvPressCenter.text = "${trainingDTO.pressCenterPercentage}"

        //吹气错误数
        viewBinding.tvHeartCount.text = "${trainingDTO.blowErrorCount}"
        //吹气总数
        viewBinding.tvHeartTotal.text = "/${trainingDTO.blowTotal}"
        //吹气错误
        viewBinding.tvAirway.text = "${trainingDTO.blowClose}"
        //吹气不足
        viewBinding.tvCInsufficient.text = "${trainingDTO.blowLow}"
        //吹气过大
        viewBinding.tvBLowBig.text = "${trainingDTO.blowHigh}"
        //吹气进胃
        viewBinding.tvIntoStomach.text = "${trainingDTO.blowIntoStomach}"
        //平均吹气每分钟次数
        viewBinding.tvBlowAverageCount.text = "${trainingDTO.blowAverage}"
        //吹气百仪表分比
        viewBinding.tvClock2.text = "${trainingDTO.blowMeterPercentage}"
        //吹气百分比
        viewBinding.tvBlow.text = "${trainingDTO.blowPercentage}"
        //吹气平均值
        viewBinding.tvBlowEnd.text = "${trainingDTO.blowAverageDepth}"
    }
}