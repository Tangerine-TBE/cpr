package com.pr.perfectrecovery.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.databinding.CheckResultTwoFragmentBinding

/**
 * 训练结果页
 */
class CheckResultTwoFragment : Fragment() {

    companion object {
        fun newInstance() = CheckResultTwoFragment()
    }

    private lateinit var viewModel: CheckResultTwoViewModel
    private lateinit var viewBinding: CheckResultTwoFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = CheckResultTwoFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CheckResultTwoViewModel::class.java)
    }

}