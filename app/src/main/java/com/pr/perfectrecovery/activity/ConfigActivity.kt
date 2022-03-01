package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.ActivityCprListBinding
import com.tencent.mmkv.MMKV

/**
 * 基础参数配置界面
1，按压深度输入范围：1-10cm
2，按压频率输入范围：10-200cpm
3，潮气量输入范围：  100-2000ml
4，潮气频率输入范围：1-60vpm
5，操作时长输入范围：00:10-10:00（m:s）
6，循环次数输入范围：1-10次
7，按压通气比例输入范围：1-50
8，流程分数输入范围：0-100
9，按压分数输入范围：0-100
10，通气分数输入范围：0-100
 */
class ConfigActivity : BaseActivity() {

    private lateinit var viewBinding: ActivityCprListBinding
    private var dataDTO: ConfigBean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCprListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {
        setData()
        viewBinding.bottom.tvContinue.visibility = View.INVISIBLE
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        //按压深度
        viewBinding.etDepth.addTextChangedListener(editTextDepth)
        viewBinding.etDepthEnd.addTextChangedListener(editTextDepth)
        //按压频率
        viewBinding.etDepthFrequency.addTextChangedListener(editTextDepthFrequency)
        viewBinding.etDepthFrequencyEnd.addTextChangedListener(editTextDepthFrequency)

        //按压中断
        viewBinding.etInterruptTime.addTextChangedListener(editTextInterrupt)

        //潮气量
        viewBinding.etTidalVolume.addTextChangedListener(editTextTidalVolume)
        viewBinding.etTidalVolumeEnd.addTextChangedListener(editTextTidalVolume)

        //潮气频率
        viewBinding.etTidalFrequency.addTextChangedListener(editTextTidalFrequency)
        viewBinding.etTidalFrequencyEnd.addTextChangedListener(editTextTidalFrequency)

        //按压次数
        viewBinding.etPr.addTextChangedListener(editTextPr)
        //吹气次数
        viewBinding.etQy.addTextChangedListener(editTextQy)
        viewBinding.etDeduction.addTextChangedListener(editTextDeduction)
        viewBinding.etProcess.addTextChangedListener(editTextProcess)
        viewBinding.etCompressions.addTextChangedListener(editTextProcess)
        viewBinding.etVentilation.addTextChangedListener(editTextProcess)

        //循环次数
        viewBinding.etCycles.addTextChangedListener(editTextCycle)
        //操作时长
        viewBinding.etTime.addTextChangedListener(editTextTime)
    }

    private fun setData() {
        val decodeString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        dataDTO = GsonUtils.fromJson(decodeString, ConfigBean::class.java)
        dataDTO.let {
            if (it != null) {
                viewBinding.etDepth.setText("${it.depth}")
                viewBinding.etDepthEnd.setText("${it.depthEnd}")
                viewBinding.etDepthFrequency.setText("${it.depthFrequency}")
                viewBinding.etDepthFrequencyEnd.setText("${it.depthFrequencyEnd}")
                viewBinding.etInterruptTime.setText("${it.interruptTime}")
                viewBinding.etTidalVolume.setText("${it.tidalVolume}")
                viewBinding.etTidalVolumeEnd.setText("${it.tidalVolumeEnd}")
                viewBinding.etTidalFrequency.setText("${it.tidalFrequency}")
                viewBinding.etTidalFrequencyEnd.setText("${it.tidalFrequencyEnd}")
                viewBinding.etTime.setText("${it.operationTime}")
                viewBinding.etCycles.setText("${it.cycles}")
                viewBinding.etPr.setText("${it.prCount}")
                viewBinding.etQy.setText("${it.qyCount}")
                viewBinding.etDeduction.setText("${it.deductionScore}")
                viewBinding.etProcess.setText("${it.processScore}")
                viewBinding.etCompressions.setText("${it.pressScore}")
                viewBinding.etVentilation.setText("${it.blowScore}")

            }
        }
    }

    private val editTextDepth = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val value = p0.toString().trim()
            if (TextUtils.isEmpty(value) || value.toInt() > 11 || value.toInt() < 0) {
                viewBinding.tvMsg.text = "按压深度输入范围：整数1～10"
            } else {
                if (!TextUtils.isEmpty(viewBinding.etDepth.text.toString().trim())) {
                    dataDTO?.depth = viewBinding.etDepth.text.toString().trim().toInt()
                }
                if (!TextUtils.isEmpty(viewBinding.etDepthEnd.text.toString().trim())) {
                    dataDTO?.depthEnd = viewBinding.etDepthEnd.text.toString().trim().toInt()
                }
                save()
            }
        }

    }

    private val editTextDepthFrequency = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val value = p0.toString().trim()
            val start = viewBinding.etDepthFrequency.text.toString().trim()
            val end = viewBinding.etDepthFrequencyEnd.text.toString().trim()
            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(start) && !TextUtils.isEmpty(end) && value.toInt() in 10..200) {
                dataDTO?.depthFrequency = start.toInt()
                dataDTO?.depthFrequencyEnd = end.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "按压频率输入范围：10-200cpm"
            }

        }

    }

    private val editTextInterrupt = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val str = viewBinding.etInterruptTime.text.toString().trim()
            if (!TextUtils.isEmpty(str) && str.toInt() > 0) {
                dataDTO?.interruptTime = str.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "按压超时时间设置"
            }
        }

    }

    private val editTextTidalFrequency = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //4，潮气频率输入范围：1-60vpm
            val value = p0.toString().trim()
            val start = viewBinding.etTidalFrequency.text.toString().trim()
            val end = viewBinding.etTidalFrequencyEnd.text.toString().trim()
            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(start) && !TextUtils.isEmpty(end) && value.toInt() in 1..60) {
                dataDTO?.tidalFrequency = start.toInt()
                dataDTO?.tidalFrequencyEnd = end.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "潮气频率输入范围：1-60vpm"
            }
        }

    }

    private val editTextTidalVolume = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //潮气量 100-2000ml
            val value = p0.toString().trim()
            val valueStart = viewBinding.etTidalVolume.text.toString().trim()
            val valueEnd = viewBinding.etTidalVolumeEnd.text.toString().trim()
            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(valueStart) && !TextUtils.isEmpty(
                    valueEnd
                ) && value.toInt() in 100..2000
            ) {
                dataDTO?.tidalVolume = valueStart.toInt()
                dataDTO?.tidalVolumeEnd = valueEnd.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "潮气量输入范围：100-2000ml"
            }
        }

    }

    private val editTextPr = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //7，按压通气比例输入范围：1-50
            val value = p0.toString().trim()
            if (!TextUtils.isEmpty(value) && value.toInt() in 1..50) {
                dataDTO?.prCount = value.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "按压通气比例输入范围：1-50"
            }
        }

    }

    private val editTextQy = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //7，按压通气比例输入范围：1-50
            val value = p0.toString().trim()
            if (!TextUtils.isEmpty(value) && value.toInt() in 1..50) {
                dataDTO?.qyCount = value.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "按压通气比例输入范围：1-50"
            }
        }
    }

    private val editTextCycle = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //中断扣分
            val value = p0.toString().trim()
            if (!TextUtils.isEmpty(value) && value.toInt() in 1..10) {
                dataDTO?.cycles = value.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "循环次数输入范围：1-10次"
            }
        }
    }

    private val editTextTime = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //中断扣分
            val value = p0.toString().trim()
            if (!TextUtils.isEmpty(value) && value.toInt() in 10..600) {
                dataDTO?.cycles = value.toInt()
                save()
            } else {
                viewBinding.tvMsg.text = "操作时长输入范围：10-600（s）"
            }
        }
    }

    private val editTextDeduction = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //中断扣分
            val value = p0.toString().trim()
            if (!TextUtils.isEmpty(value) && value.toFloat() > 0) {
                dataDTO?.deductionScore =
                    viewBinding.etDeduction.text.toString().trim().toFloat()
                save()
            } else {
                viewBinding.tvDesc.text = "按压中断时间设定大于10s"
            }
        }
    }

    private val editTextProcess = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            process()
        }
    }

    private fun process() {
        //流程分数
        val process =
            if (!TextUtils.isEmpty(viewBinding.etProcess.text.toString().trim())) {
                viewBinding.etProcess.text.toString().trim().toInt()
            } else {
                0
            }
        //胸外按压
        val compressions =
            if (!TextUtils.isEmpty(viewBinding.etCompressions.text.toString().trim())) {
                viewBinding.etCompressions.text.toString().trim().toInt()
            } else {
                0
            }
        //人工通气
        val ventilation =
            if (!TextUtils.isEmpty(viewBinding.etVentilation.text.toString().trim())) {
                viewBinding.etVentilation.text.toString().trim().toInt()
            } else {
                0
            }
        val number = process + compressions + ventilation
        if (number > 100) {
            viewBinding.tvDesc.text = "三项加起来总分 ＜ 100分"
        } else {
            viewBinding.tvDesc.text = ""
            dataDTO?.processScore = process
            dataDTO?.pressScore = compressions
            dataDTO?.blowScore = ventilation
            save()
        }
    }

    private fun save() {
        viewBinding.tvMsg.text = ""
        viewBinding.tvDesc.text = ""
        dataDTO.let {
            MMKV.defaultMMKV()
                .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
        }
    }

}