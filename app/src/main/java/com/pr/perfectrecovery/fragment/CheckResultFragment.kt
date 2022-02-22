package com.pr.perfectrecovery.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pr.perfectrecovery.databinding.CheckResultFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.CheckResultViewModel

/**
 * 考核结果页
 */
class CheckResultFragment : Fragment() {

    companion object {
        fun newInstance() = CheckResultFragment()
    }

    private lateinit var viewModel: CheckResultViewModel
    private lateinit var viewBinding: CheckResultFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = CheckResultFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CheckResultViewModel::class.java)
        initView()
    }

    private fun initView() {

    }

}