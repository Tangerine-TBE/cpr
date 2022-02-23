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
import com.pr.perfectrecovery.bean.ScoringConfigBean
import com.pr.perfectrecovery.databinding.CprStandardFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CPRStandardViewModel
import com.tencent.mmkv.MMKV

/**
 * CPR评标准配置
 * 2021年11月27
 * jayce
 */
class CPRStandardFragment : Fragment() {
    private lateinit var viewBinding: CprStandardFragmentBinding
    private var dataDTO: ScoringConfigBean? = null

    companion object {
        fun newInstance() = CPRStandardFragment()
    }

    private lateinit var viewModel: CPRStandardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = CprStandardFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CPRStandardViewModel::class.java)
        initView()
    }

    private fun initView() {
        //按压深度
        viewBinding.etDepth.addTextChangedListener(editTextDepth)
        viewBinding.etDepthEnd.addTextChangedListener(editTextDepth)
        //按压频率
        viewBinding.etDepthFrequency.addTextChangedListener(editTextDepthFrequency)
        viewBinding.etDepthFrequencyEnd.addTextChangedListener(editTextDepthFrequency)

        //按压中断
        viewBinding.etInterrupt.addTextChangedListener(editTextInterrupt)

        //潮气量
        viewBinding.etTidalVolume.addTextChangedListener(editTextTidalVolume)
        viewBinding.etTidalVolumeEnd.addTextChangedListener(editTextTidalVolume)

        //潮气频率
        viewBinding.etTidalFrequency.addTextChangedListener(editTextTidalFrequency)
        viewBinding.etTidalFrequencyEnd.addTextChangedListener(editTextTidalFrequency)

        //按压次数
        viewBinding.etPr.addTextChangedListener(editTextCompression)
        //吹气次数
        viewBinding.etQy.addTextChangedListener(editTextCompression)

        setData()
    }

    private fun setData() {
        val decodeString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        dataDTO = GsonUtils.fromJson(decodeString, ScoringConfigBean::class.java)
        dataDTO.let {
            if (it != null) {
                viewBinding.etDepth.setText("${it.depth}")
                viewBinding.etDepthEnd.setText("${it.depthEnd}")
                viewBinding.etDepthFrequency.setText("${it.depthFrequency}")
                viewBinding.etDepthFrequencyEnd.setText("${it.depthFrequencyEnd}")
                viewBinding.etInterrupt.setText("${it.interrupt}")
                viewBinding.etTidalVolume.setText("${it.tidalVolume}")
                viewBinding.etTidalVolumeEnd.setText("${it.tidalVolumeEnd}")
                viewBinding.etTidalFrequency.setText("${it.tidalFrequency}")
                viewBinding.etTidalFrequencyEnd.setText("${it.tidalFrequencyEnd}")
                viewBinding.etTime.setText("${it.operationTime}")
                viewBinding.etCycles.setText("${it.cycles}")
                viewBinding.etPr.setText("${it.prCount}")
                viewBinding.etQy.setText("${it.qyCount}")

            }
        }
    }

    private val editTextDepth = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
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
            dataDTO?.interrupt = viewBinding.etInterrupt.text.toString().trim()
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
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

    private val editTextCompression = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            if (!TextUtils.isEmpty(viewBinding.etPr.text.toString().trim())) {
                dataDTO?.prCount = viewBinding.etPr.text.toString().trim().toInt()
            }
            if (!TextUtils.isEmpty(viewBinding.etQy.text.toString().trim())) {
                dataDTO?.qyCount =
                    viewBinding.etQy.text.toString().trim().toInt()
            }
            dataDTO.let {
                MMKV.defaultMMKV()
                    .encode(BaseConstant.MMKV_WM_CONFIGURATION, GsonUtils.toJson(dataDTO))
            }
        }

    }

}