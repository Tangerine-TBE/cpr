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
val PDF_FLAG = "pdf_flag"

class TrainResultActivity : BaseActivity() {
    private lateinit var viewBinding: ActivityTrainResultBinding

    companion object {
        fun start(context: Context, trainingDTO: TrainingDTO) {
            val intent = Intent(context, TrainResultActivity::class.java)
            intent.putExtra(DATADTO, trainingDTO)
            context.startActivity(intent)
        }

        fun start(context: Context, trainingDTO: TrainingDTO, flagPDF: Boolean) {
            val intent = Intent(context, TrainResultActivity::class.java)
            intent.putExtra(DATADTO, trainingDTO)
            intent.putExtra(PDF_FLAG, flagPDF)
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
        trainingDTO.save()
        //判断模式- false 训练  true 考核
        if (trainingDTO.isCheck) {
            viewBinding.layoutCheck.root.visibility = View.VISIBLE
            viewBinding.groupPr.visibility = View.VISIBLE
            viewBinding.groupQy.visibility = View.VISIBLE
            viewBinding.gruops.visibility = View.GONE
            viewBinding.layoutCheck.tvName.text = "${trainingDTO.name}"
            viewBinding.layoutCheck.tvScale.text = "${trainingDTO.prCount}:${trainingDTO.qyCount}"
            //倒计时
            viewBinding.layoutCheck.tvCountdown.text = TimeUtils.timeParse(trainingDTO.operateTime)
            //流程分数
            viewBinding.layoutCheck.tvProcess.text = "${trainingDTO.processScore}分"
            //按压分数
            viewBinding.layoutCheck.tvPress.text = "${trainingDTO.pressScore}分"
            //扣分
            viewBinding.layoutCheck.tvDeduction.text = "${trainingDTO.deduction}分"
            //吹气分数
            viewBinding.layoutCheck.tvBlowNumber.text = "${trainingDTO.blowScore}分"

            /*-----------------------------总结得分项----------------------------------*/
            //按压得分
            viewBinding.layoutCheck.tvPressScore.text = "${trainingDTO.getPrScore()}"
            //中断扣分
            viewBinding.layoutCheck.tvInterruptScore.text = "${trainingDTO.getTimeOutScore()}"
            //通气得分
            viewBinding.layoutCheck.tvVentilationScore.text = "${trainingDTO.getQyScore()}"
            //流程分数
            viewBinding.layoutCheck.tvProcessScore2.text = "${processCheck(trainingDTO)}"

            //分数星星配置
            viewBinding.layoutCheck.ratingBar.progress = trainingDTO.score.toInt()
            //总得分
            viewBinding.layoutCheck.tvScore.text = "${trainingDTO.score}"

            //按压超次
            viewBinding.tvOutBoutCount.text = "${trainingDTO.prManyCount}次"
            //按压少次
            viewBinding.tvSmallBoutCount.text = "${trainingDTO.prLessCount}次"
            //吹气多次
            viewBinding.tvBlowBoutCount.text = "${trainingDTO.qyManyCount}次"
            //吹气少次
            viewBinding.tvBlowSmallCount.text = "${trainingDTO.qyLessCount}次"
        } else {
            viewBinding.gruops.visibility = View.VISIBLE
            viewBinding.tvTrainName.text = trainingDTO.name
            viewBinding.tvTime.text = TimeUtils.formatDate(trainingDTO.operateTime)
        }

        //循环次数
        viewBinding.tvCycleCount.text = "${trainingDTO.cycleCount}"

        //按压错误数
        viewBinding.tvLungCount.text = "${trainingDTO.pressErrorCount.toInt()}"
        //按压总数
        viewBinding.tvLungTotal.text = "/${trainingDTO.prSum.toInt()}"
        //按压位置错误
        viewBinding.tvLocation.text = "${trainingDTO.pressLocation}"
        //按压不足
        viewBinding.tvInsufficient.text = "${trainingDTO.pressLow}"
        //按压过大
        viewBinding.tvPressBig.text = "${trainingDTO.pressHigh}"
        //按压未回弹
        viewBinding.tvRebound.text = "${trainingDTO.pressRebound.toInt()}"
        //按压超时统计时间
        viewBinding.tvPressTime.text = "${TimeUtils.timeParse(trainingDTO.pressOutTime)}"
        //平均每分钟按压次数
        viewBinding.tvAverageCount.text = "平均：${trainingDTO.getPressAverageTimes()}次/分"
        //按压频率合格率
        viewBinding.tvClock1.text = "${trainingDTO.getPressRate()}%"
        //回弹合格率
        viewBinding.tvPressPercentage.text = "${trainingDTO.getReboundRate()}%"
        //按压深度合格率
        viewBinding.tvPressEnd.text = "${trainingDTO.getDepthRate()}%"
        //按压平均深度
        viewBinding.tvPressBottom.text = "平均：${trainingDTO.getPressAverageDepth()}mm"
        //整体按压百分比
        viewBinding.tvPressCenter.text = "${trainingDTO.getPressTime()}%"
        //吹气错误数
        viewBinding.tvHeartCount.text = "${trainingDTO.blowErrorCount.toInt()}"
        //吹气总数
        viewBinding.tvHeartTotal.text = "/${trainingDTO.qySum}"
        //吹气错误
        viewBinding.tvAirway.text = "${trainingDTO.err_qy_close}"
        //吹气不足
        viewBinding.tvCInsufficient.text = "${trainingDTO.blowLow}"
        //吹气过大
        viewBinding.tvBLowBig.text = "${trainingDTO.blowHigh}"
        //吹气进胃
        viewBinding.tvIntoStomach.text = "${trainingDTO.blowIntoStomach}"
        //平均吹气每分钟次数
        viewBinding.tvBlowAverageCount.text = "平均：${trainingDTO.getBlowAverage()}次/分"
        //吹气频率百分比
        viewBinding.tvClock2.text = "${trainingDTO.getBlowRate()}%"
        //通气合格率
        viewBinding.tvBlow.text = "${trainingDTO.getBlowAmount()}%"
        //吹气平均值
        viewBinding.tvBlowEnd.text = "平均：${trainingDTO.getBlowAverageNumber()}ml"
    }

    /**
     * 流程分数
     */
    private fun processCheck(trainingDTO: TrainingDTO): Float {
        //该页面禁止点击事件
        viewBinding.layoutCheck.check.checkBox1.isClickable = false
        viewBinding.layoutCheck.check.checkBox2.isClickable = false
        viewBinding.layoutCheck.check.checkBox3.isClickable = false
        viewBinding.layoutCheck.check.checkBox4.isClickable = false
        viewBinding.layoutCheck.check.checkBox5.isClickable = false
        viewBinding.layoutCheck.check.checkBox6.isClickable = false
        viewBinding.layoutCheck.check.checkBox7.isClickable = false
        viewBinding.layoutCheck.check.checkBox8.isClickable = false
        viewBinding.layoutCheck.check.checkBox9.isClickable = false
        viewBinding.layoutCheck.check.checkBox10.isClickable = false

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

        if (trainingDTO.processScore > 0) {
            return ((trainingDTO.processScore / 10) * listCheck.size)
        }
        return 0f
    }

    private val listCheck = mutableListOf<Int>()
    private val onCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                listCheck.add(buttonView.id)
            }
        }

}