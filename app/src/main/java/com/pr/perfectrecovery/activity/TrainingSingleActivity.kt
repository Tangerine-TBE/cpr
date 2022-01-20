package com.pr.perfectrecovery.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.clj.fastble.data.BleDevice
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.databinding.ActivityTrianBinding

/**
 * 训练模式- 单人 - 多人
 *
 */
class TrainingSingleActivity : BaseActivity() {

    private lateinit var binding: ActivityTrianBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrianBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        val blueToothList = intent.getParcelableArrayListExtra<BleDevice>("blueTooth")
        if (blueToothList != null && blueToothList.size > 1) {
            initMany()
        } else {
            initSingle()
        }
    }

    /**
     * 单人模式
     */
    private fun initSingle() {
        binding.bottom.ivBack.setOnClickListener { finish() }
        val mTrainingBean = TrainingBean()
        //考核模式
        binding.single.cbCheck.setOnCheckedChangeListener { compoundButton, b ->
            //MMKV.defaultMMKV().encode("", );
            if (b) {
                binding.single.cbCheck.setTextColor(resources.getColor(R.color.color_37B48B))
                mTrainingBean.isCheck = true
                binding.single.cbTraining.isChecked = false
            } else {
                binding.single.cbCheck.setTextColor(resources.getColor(R.color.white))
            }
            //考核模式  禁止使用语音和提示音
            binding.single.switchBeat.isChecked = false
            binding.single.switchVoice.isChecked = false
        }

        //练习模式
        binding.single.cbTraining.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                mTrainingBean.isCheck = false
                binding.single.cbCheck.isChecked = false
                binding.single.cbTraining.setTextColor(resources.getColor(R.color.color_37B48B))
            } else {
                binding.single.cbTraining.setTextColor(resources.getColor(R.color.white))
            }
        }

        binding.bottom.ivStart.setOnClickListener {
            val name = binding.single.etName.text.toString()
            if (TextUtils.isEmpty(name)) {
                ToastUtils.showShort(R.string.please_input_name)
            } else if (!binding.single.cbTraining.isChecked && !binding.single.cbCheck.isChecked) {
                ToastUtils.showShort(R.string.please_select_model)
            } else {
                mTrainingBean.name = name
                mTrainingBean.isBeat = binding.single.switchBeat.isChecked
                mTrainingBean.isVoice = binding.single.switchVoice.isChecked
                val intent = Intent(this, SingleActivity::class.java)
                intent.putExtra("single", mTrainingBean)
                startActivity(intent)
            }
        }
    }

    /**
     * 多人模式
     */
    private fun initMany() {
        binding.more.cbCheck.setOnCheckedChangeListener { compoundButton, b ->
            if (b)
                binding.more.cbCheck.setTextColor(resources.getColor(R.color.color_37B48B))
            else
                binding.more.cbCheck.setTextColor(resources.getColor(R.color.white))
        }

        binding.more.cbTraining.setOnCheckedChangeListener { compoundButton, b ->
            if (b)
                binding.more.cbTraining.setTextColor(resources.getColor(R.color.color_37B48B))
            else
                binding.more.cbTraining.setTextColor(resources.getColor(R.color.white))
        }

        binding.more.mRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.more.mRecyclerView.adapter = null

        binding.bottom.ivStart.setOnClickListener {
            val name = binding.single.etName.text.toString()
            if (TextUtils.isEmpty(name)) {
                ToastUtils.showShort(R.string.please_input_name)
            } else if (!binding.single.cbTraining.isChecked && !binding.single.cbCheck.isChecked) {
                ToastUtils.showShort(R.string.please_select_model)
            } else {
                val intent = Intent(this, SingleActivity::class.java)
//                intent.putExtra("single", mTrainingBean)
                startActivity(intent)
            }
        }
    }
}