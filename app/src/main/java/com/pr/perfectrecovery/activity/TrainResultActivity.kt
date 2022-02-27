package com.pr.perfectrecovery.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
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
        //判断模式- false 训练  true 考核
        if (trainingDTO.isCheck) {
            viewBinding.layoutCheck.root.visibility = View.VISIBLE
            viewBinding.groupPr.visibility = View.VISIBLE
            viewBinding.groupQy.visibility = View.VISIBLE
            viewBinding.gruops.visibility = View.GONE
            viewBinding.layoutCheck.tvName.text = "${trainingDTO.name}"
            viewBinding.layoutCheck.tvScale.text = "${trainingDTO.prScale}:${trainingDTO.qyScale}"
            viewBinding.layoutCheck.tvCountdown.text = "${trainingDTO.trainingTime}"//倒计时
            viewBinding.layoutCheck.tvProcess.text = "${trainingDTO.pressScore}"
            viewBinding.layoutCheck.tvDeduction.text = "${trainingDTO.deduction}"
            viewBinding.layoutCheck.tvBlowScale.text = "${trainingDTO.blowScore}"

            //按压得分
            viewBinding.layoutCheck.tvPressScore.text = ""
            //中断扣分
            viewBinding.layoutCheck.tvInterruptScore.text = ""
            //通气得分
            viewBinding.layoutCheck.tvVentilationScore.text = ""
            //流程分数
            processCheck(trainingDTO)
            viewBinding.layoutCheck.tvProcessScore2.text = ""

            //总得分
            //分数星星配置
            viewBinding.layoutCheck.ratingBar.progress = 0
            viewBinding.layoutCheck.tvScore.text = ""
        } else {
            viewBinding.gruops.visibility = View.VISIBLE
            viewBinding.tvTrain.text = "训练"
            viewBinding.tvName.text = trainingDTO.name
            viewBinding.tvTime.text = trainingDTO.trainingTime
        }

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
        viewBinding.tvClock1.text = "${trainingDTO.getPressRate()}%"
        //按压百分比
        viewBinding.tvPress.text = "${trainingDTO.getReboundRate()}%"
        viewBinding.tvPressEnd.text = "${trainingDTO.getDepthRate()}%"
        //按压平均深度
        viewBinding.tvPressBottom.text = ""
        //整体按压百分比
        viewBinding.tvPressCenter.text = ""

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
        //吹气频率百分比
        viewBinding.tvClock2.text = "${trainingDTO.getBlowRate()}"
        //吹气百分比
        viewBinding.tvBlow.text = "${trainingDTO.getBlowAmount()}"
        //吹气平均值
        viewBinding.tvBlowEnd.text = ""
    }

    /**
     * 流程分数
     */
    private fun processCheck(trainingDTO: TrainingDTO): Int {
        viewBinding.layoutCheck.check.checkBox1.isChecked = trainingDTO.check1
        viewBinding.layoutCheck.check.checkBox2.isChecked = trainingDTO.check2
        viewBinding.layoutCheck.check.checkBox3.isChecked = trainingDTO.check3
        viewBinding.layoutCheck.check.checkBox4.isChecked = trainingDTO.check4
        viewBinding.layoutCheck.check.checkBox5.isChecked = trainingDTO.check5
        viewBinding.layoutCheck.check.checkBox6.isChecked = trainingDTO.check6
        viewBinding.layoutCheck.check.checkBox7.isChecked = trainingDTO.check7
        viewBinding.layoutCheck.check.checkBox8.isChecked = trainingDTO.check8
        viewBinding.layoutCheck.check.checkBox9.isChecked = trainingDTO.check9
        viewBinding.layoutCheck.check.checkBox10.isChecked = trainingDTO.check10

        viewBinding.layoutCheck.check.checkBox1.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox2.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox3.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox4.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox5.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox6.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox7.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox8.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox9.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        viewBinding.layoutCheck.check.checkBox10.setOnCheckedChangeListener(
            onCheckedChangeListener
        )
        if (trainingDTO.pressScore > 0) {
            return trainingDTO.pressScore / 10 * listCheck.size
        }
        return 0
    }

    private val listCheck = mutableListOf<Int>()
    private val onCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                listCheck.add(buttonView.id)
            }
        }

}