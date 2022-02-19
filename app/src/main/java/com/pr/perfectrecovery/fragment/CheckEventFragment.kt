package com.pr.perfectrecovery.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.EventFragmentBinding
import com.pr.perfectrecovery.fragment.viewmodel.EventViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CheckEventFragment : Fragment() {
    private lateinit var viewBinding: EventFragmentBinding

    companion object {
        fun newInstance() = CheckEventFragment()
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
        EventBus.getDefault().register(this)
        viewModel = ViewModelProvider(this).get(EventViewModel::class.java)
        initVIew()
    }

    private fun initVIew() {
        viewBinding.root.children.forEach {
            if (it is CheckBox) {
                it.isEnabled = false
                it.setTextColor(Color.GRAY)

            }
        }

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        if (event.code == BaseConstant.EVENT_SINGLE_CHART_START) {
            viewBinding.root.children.forEach {
                if (it is CheckBox) {
                    it.isEnabled = true
                    it.setTextColor(Color.WHITE)

                }
            }
        }
    }

}