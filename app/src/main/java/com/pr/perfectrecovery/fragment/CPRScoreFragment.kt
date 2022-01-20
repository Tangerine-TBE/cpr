package com.pr.perfectrecovery.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.GsonUtils
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.ScoringConfigBean
import com.pr.perfectrecovery.databinding.CprScoreFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CPRScoreSettingViewModel
import com.tencent.mmkv.MMKV

/**
 * CPR评分配置
 * 2021年11月27
 * jayce
 */
class CPRScoreFragment : Fragment() {

    private lateinit var viewBinding: CprScoreFragmentBinding
    private var dataDTO: ScoringConfigBean? = null

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
    }

    private fun setData() {
        val decodeString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        dataDTO = GsonUtils.fromJson(decodeString, ScoringConfigBean::class.java)
        dataDTO.let {
            if (it != null) {
                viewBinding.etDeduction.setText("${it.deduction}")
                viewBinding.etProcess.setText("${it.process}")
                viewBinding.etCompressions.setText("${it.compressions}")
                viewBinding.etVentilation.setText("${it.ventilation}")
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
            dataDTO?.deduction = viewBinding.etDeduction.text.toString().trim().toFloat()
            dataDTO.let {
                MMKV.defaultMMKV().encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }
    }

    private val editText = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            //流程分数
            val process = viewBinding.etProcess.text.toString().trim().toInt()
            //胸外按压
            val compressions = viewBinding.etCompressions.text.toString().trim().toInt()
            //人工通气
            val ventilation = viewBinding.etVentilation.text.toString().trim().toInt()
            val number = process + compressions + ventilation
            if (number > 100) {
                viewBinding.tvDesc.text = "三项加起来总分 ＜ 100分"
            } else {
                dataDTO?.process = process
                dataDTO?.compressions = compressions
                dataDTO?.ventilation = ventilation
                dataDTO.let {
                    MMKV.defaultMMKV().encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
                }
            }
        }
    }
}