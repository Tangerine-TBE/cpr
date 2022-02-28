package com.pr.perfectrecovery.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.databinding.CprScoreFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CPRScoreSettingViewModel
import com.tencent.mmkv.MMKV

/**
 * CPR评分配置
 * 2021年11月27
 * jayce
 */
class CPRScoreFragment : Fragment() {

    private var isInit = false
    private lateinit var viewBinding: CprScoreFragmentBinding
    private var dataDTO: ConfigBean? = null

    companion object {
        fun newInstance() = CPRScoreFragment()
    }

    private lateinit var viewModel: CPRScoreSettingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = CprScoreFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CPRScoreSettingViewModel::class.java)
        initView()
    }

    private fun initView() {
        viewBinding.etDeduction.addTextChangedListener(editTextDeduction)
        viewBinding.etProcess.addTextChangedListener(editText)
        viewBinding.etCompressions.addTextChangedListener(editText)
        viewBinding.etVentilation.addTextChangedListener(editText)
        setData()
        isInit = true
    }

    private fun setData() {
        val decodeString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        dataDTO = GsonUtils.fromJson(decodeString, ConfigBean::class.java)
        dataDTO.let {
            if (it != null) {
                viewBinding.etDeduction.setText("${it.deductionScore}")
                viewBinding.etProcess.setText("${it.processScore}")
                viewBinding.etCompressions.setText("${it.pressScore}")
                viewBinding.etVentilation.setText("${it.blowScore}")
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
            dataDTO?.deductionScore = viewBinding.etDeduction.text.toString().trim().toFloat()
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }
    }

    private val editText = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (isInit) {
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
                    dataDTO?.processScore = process
                    dataDTO?.pressScore = compressions
                    dataDTO?.blowScore = ventilation
                    dataDTO.let {
                        MMKV.defaultMMKV()
                            .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                    }
                }
            }
        }
    }
}