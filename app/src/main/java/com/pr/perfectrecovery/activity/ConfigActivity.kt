package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.ActivityCprListBinding
import com.pr.perfectrecovery.fragment.CPRScoreFragment
import com.pr.perfectrecovery.fragment.CPRStandardFragment
import com.pr.perfectrecovery.utils.TimeUtils
import com.tencent.mmkv.MMKV

/**
 * 基础参数配置界面
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
                viewBinding.etDeduction.setText("${it.deductionTime}")
                viewBinding.etProcess.setText("${it.process}")
                viewBinding.etCompressions.setText("${it.compressions}")
                viewBinding.etVentilation.setText("${it.ventilation}")

            }
        }
    }

    private val editTextDepth = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (p0.toString().toInt() in 11 downTo 0) {
                viewBinding.tvMsg.text = "按压深度输入范围：整数1～10"
            } else {
                if (!TextUtils.isEmpty(viewBinding.etDepth.text.toString().trim())) {
                    dataDTO?.depth = viewBinding.etDepth.text.toString().trim().toInt()
                }
                if (!TextUtils.isEmpty(viewBinding.etDepthEnd.text.toString().trim())) {
                    dataDTO?.depthEnd = viewBinding.etDepthEnd.text.toString().trim().toInt()
                }
                dataDTO.let {
                    MMKV.defaultMMKV()
                        .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                }
            }
        }

    }

    private val editTextDepthFrequency = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (!TextUtils.isEmpty(viewBinding.etDepthFrequency.text.toString().trim())) {
                dataDTO?.depthFrequency =
                    viewBinding.etDepthFrequency.text.toString().trim().toInt()
            }
            if (!TextUtils.isEmpty(viewBinding.etDepthFrequencyEnd.text.toString().trim())) {
                dataDTO?.depthFrequencyEnd =
                    viewBinding.etDepthFrequencyEnd.text.toString().trim().toInt()
            }
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }

    }

    private val editTextInterrupt = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val str = viewBinding.etInterruptTime.text.toString().trim().toInt()

            if (str > 0) {
                dataDTO?.interruptTime = viewBinding.etInterruptTime.text.toString().trim().toInt()
                dataDTO.let {
                    MMKV.defaultMMKV()
                        .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                }
            }
        }

    }

    private val editTextTidalFrequency = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (!TextUtils.isEmpty(viewBinding.etTidalFrequency.text.toString().trim())) {
                dataDTO?.tidalFrequency =
                    viewBinding.etTidalFrequency.text.toString().trim().toInt()
            }
            if (!TextUtils.isEmpty(viewBinding.etTidalFrequencyEnd.text.toString().trim())) {
                dataDTO?.tidalFrequencyEnd =
                    viewBinding.etTidalFrequencyEnd.text.toString().trim().toInt()
            }
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }

    }

    private val editTextTidalVolume = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (!TextUtils.isEmpty(viewBinding.etTidalVolume.text.toString().trim())) {
                dataDTO?.tidalVolume = viewBinding.etTidalVolume.text.toString().trim().toInt()
            }
            if (!TextUtils.isEmpty(viewBinding.etTidalVolumeEnd.text.toString().trim())) {
                dataDTO?.tidalVolumeEnd =
                    viewBinding.etTidalVolumeEnd.text.toString().trim().toInt()
            }
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }
    }

    private val editTextPr = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val value = viewBinding.etPr.text.toString().trim()
            if (TextUtils.isEmpty(value)) {
                viewBinding.tvMsg.text = "请填写按压次数"
            } else {
                dataDTO?.prCount = value.toInt()
                dataDTO.let {
                    MMKV.defaultMMKV()
                        .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                }
            }
        }

    }

    private val editTextQy = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            val value = viewBinding.etQy.text.toString().trim()
            if (TextUtils.isEmpty(value)) {
                viewBinding.tvMsg.text = "请填写吹气次数"
            } else {
                dataDTO?.qyCount = value.toInt()
                dataDTO.let {
                    MMKV.defaultMMKV()
                        .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                }
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
            dataDTO?.deductionTime =
                viewBinding.etDeduction.text.toString().trim().toFloat().toInt()
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
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
            dataDTO?.process = process
            dataDTO?.compressions = compressions
            dataDTO?.ventilation = ventilation
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }
    }

}