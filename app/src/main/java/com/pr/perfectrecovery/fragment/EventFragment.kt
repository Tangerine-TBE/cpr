package com.pr.perfectrecovery.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pr.perfectrecovery.databinding.EventFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.EventViewModel

class EventFragment : Fragment() {
    private lateinit var viewBinding: EventFragmentBinding

    companion object {
        fun newInstance() = EventFragment()
    }

    private lateinit var viewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = EventFragmentBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(EventViewModel::class.java)
        initVIew()
    }

    private fun initVIew() {
        viewBinding.checkBox.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox2.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox3.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox4.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox5.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox6.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox7.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox8.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox9.setOnCheckedChangeListener { compoundButton, b ->

        }
        viewBinding.checkBox10.setOnCheckedChangeListener { compoundButton, b ->

        }

    }

}