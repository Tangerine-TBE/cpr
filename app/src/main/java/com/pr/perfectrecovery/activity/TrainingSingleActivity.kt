package com.pr.perfectrecovery.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.clj.fastble.data.BleDevice
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.TrainingBean
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.databinding.ActivityTrianBinding
import org.greenrobot.eventbus.EventBus

/**
 * 训练模式- 单人 - 多人
 *
 */
class TrainingSingleActivity : BaseActivity() {

    private lateinit var binding: ActivityTrianBinding
    private var isSingle = true
    private var blueToothList = arrayListOf<BleDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrianBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        blueToothList =
            intent.getParcelableArrayListExtra<BleDevice>(BaseConstant.CONNECT_BLE_DEVICES) as ArrayList<BleDevice>

        if (blueToothList.size > 1) {
            isSingle = false
            initMany(blueToothList.size)
        } else {
            isSingle = true
            initSingle()
        }
    }

    /**
     * 单人模式
     */
    private fun initSingle() {
        binding.single.root.visibility = View.VISIBLE
        binding.more.root.visibility = View.GONE

        binding.bottom.ivBack.setOnClickListener { finish() }
        val mTrainingBean = TrainingBean()
        mTrainingBean.isSingle = true

        binding.single.oprMod.setOnCheckedChangeListener { radioGroup, _ ->
            when (radioGroup.checkedRadioButtonId) {
                //考核模式
                R.id.cbCheck -> {
                    binding.single.cbCheck.setTextColor(resources.getColor(R.color.color_37B48B))
                    binding.single.cbTraining.setTextColor(resources.getColor(R.color.white))
                    mTrainingBean.isCheck = true
                    val messageEventData =
                        MessageEventData(BaseConstant.EVENT_CPR_CHECK, "", null, true)
                    EventBus.getDefault().post(messageEventData)
                    //考核模式  禁止使用语音和提示音
                    binding.single.switchBeat.isChecked = false
                    binding.single.switchVoice.isChecked = false
                    binding.single.switchBeat.isEnabled = false
                    binding.single.switchVoice.isEnabled = false
                }
                //练习模式
                R.id.cbTraining -> {
                    val messageEventData =
                        MessageEventData(BaseConstant.EVENT_CPR_CHECK, "", null, false)
                    EventBus.getDefault().post(messageEventData)
                    binding.single.cbCheck.setTextColor(resources.getColor(R.color.white))
                    binding.single.cbTraining.setTextColor(resources.getColor(R.color.color_37B48B))
                    mTrainingBean.isCheck = false
                    //考核模式  禁止使用语音和提示音
                    binding.single.switchBeat.isEnabled = true
                    binding.single.switchVoice.isEnabled = true
                }
            }
        }
        val messageEventData =
            MessageEventData(BaseConstant.EVENT_CPR_CHECK, "", null, false)
        EventBus.getDefault().post(messageEventData)
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
                intent.putExtra(BaseConstant.TRAINING_BEAN, mTrainingBean)
                startActivity(intent)
            }
        }
    }

    /**
     * 多人模式
     */
    private fun initMany(count: Int) {
        binding.more.root.visibility = View.VISIBLE
        binding.single.root.visibility = View.GONE

        val nameList = mutableListOf<EditText>()
        binding.bottom.ivBack.setOnClickListener { finish() }
        val mTrainingBean = TrainingBean()
        mTrainingBean.isSingle = false

        binding.more.oprMod.setOnCheckedChangeListener { radioGroup, _ ->
            when (radioGroup.checkedRadioButtonId) {
                //考核模式
                R.id.cbCheck -> {
                    mTrainingBean.isCheck = true
                    binding.more.cbCheck.setTextColor(resources.getColor(R.color.color_37B48B))
                    binding.more.cbTraining.setTextColor(resources.getColor(R.color.white))
                }
                //练习模式
                R.id.cbTraining -> {
                    mTrainingBean.isCheck = false
                    binding.more.cbCheck.setTextColor(resources.getColor(R.color.white))
                    binding.more.cbTraining.setTextColor(resources.getColor(R.color.color_37B48B))
                }
            }
        }
        for (i in 1..count) {
            val view = LayoutInflater.from(this@TrainingSingleActivity)
                .inflate(R.layout.item_student_more, null, false)
            view.findViewById<TextView>(R.id.tvStudent).text = "学员  $i  姓名:"
            binding.more.mMultiStuContainer.addView(view)
            val editText = view.findViewById<EditText>(R.id.etName)
            editText.tag = i
            nameList.add(editText)
        }

        binding.bottom.ivStart.setOnClickListener {
            mTrainingBean.list.clear()
            for (name in nameList) {
                if (name.text.toString().isEmpty()) {
                    ToastUtils.showShort(
                        String.format(
                            resources.getString(R.string.please_input_student_name),
                            nameList.indexOf(name) + 1
                        )
                    )
                    return@setOnClickListener
                }

                val bean = TrainingBean()
                bean.name = name.text.toString()
                bean.isCheck = mTrainingBean.isCheck
                // 根据 BleDevice 的count跟学员顺序对应绑定
                for (device in blueToothList) {
                    // count从0开始， 学员下标从0开始
                    if (device.count == name.tag) {
                        bean.count = device.count
                        bean.mac = initMac(device.mac)
                        //bean里面：设备mac 和 学员姓名 产生映射
                        mTrainingBean.list.add(bean)
                    }
                }
            }

            if (!binding.single.cbTraining.isChecked && !binding.single.cbCheck.isChecked) {
                ToastUtils.showShort(R.string.please_select_model)
            } else {
                val intent = Intent(this, MutiActivityNew::class.java)
                intent.putExtra(BaseConstant.TRAINING_BEAN, mTrainingBean)
                startActivity(intent)
            }
        }
    }

    private fun initMac(mac: String): String {
        val new = mac.replace(":", "")
        return new.lowercase()
    }
}