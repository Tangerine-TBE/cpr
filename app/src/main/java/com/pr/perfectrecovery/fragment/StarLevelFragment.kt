package com.pr.perfectrecovery.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.databinding.CycleFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.StarLevelViewModel

class StarLevelFragment : Fragment() {
    private lateinit var viewBinding: CycleFragmentBinding
    companion object {
        fun newInstance() = StarLevelFragment()
    }

    private lateinit var viewModel: StarLevelViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.star_level_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StarLevelViewModel::class.java)
        // TODO: Use the ViewModel
    }

}