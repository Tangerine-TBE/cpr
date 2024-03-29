package com.pr.perfectrecovery.activity

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivityStatisticalBinding
import com.pr.perfectrecovery.databinding.ItemStatisticalBinding
import com.pr.perfectrecovery.utils.TimeUtils
import com.yanzhenjie.recyclerview.OnItemMenuClickListener
import com.yanzhenjie.recyclerview.SwipeMenuCreator
import com.yanzhenjie.recyclerview.SwipeMenuItem
import com.yanzhenjie.recyclerview.SwipeRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 统计管理
 */
class StatisticalActivity : BaseActivity() {

    private lateinit var binding: ActivityStatisticalBinding
    private var mDataList = arrayListOf<TrainingDTO>()
    private val selectList = arrayListOf<TrainingDTO>()
    private var isDel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.top.tvRight.text = "管理"
        binding.bottom.root.setBackgroundColor(Color.parseColor("#22231D"))
        binding.bottom.ivBack.setOnClickListener { finish() }
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        // 设置监听器。
        binding.recyclerview.setSwipeMenuCreator(mSwipeMenuCreator)
        // 菜单点击监听。
        binding.recyclerview.setOnItemMenuClickListener(mItemMenuClickListener)
        binding.recyclerview.adapter = mAdapter
        //mAdapter.setEmptyView(R.layout.empty_layout)

        //协程异步加载数据
        lifecycleScope.launch(Dispatchers.IO) {
            mDataList = LitePal.findAll(TrainingDTO::class.java) as ArrayList<TrainingDTO>
            withContext(Dispatchers.Main) {
                mAdapter.setList(mDataList)
            }
        }

        binding.top.tvRight.setOnClickListener {
            isDel = !isDel
            selectList.clear()
            if (isDel) {
                binding.top.tvRight.text = "取消"
                binding.top.tvDel.visibility = View.VISIBLE
                mAdapter.setOnItemClickListener { adapter, view, position ->

                }
            } else {
                binding.top.tvRight.text = "管理"
                binding.top.tvDel.visibility = View.INVISIBLE
                mAdapter.setOnItemClickListener { adapter, view, position ->
                    val item = mAdapter.getItem(position)
                    TrainResultActivity.start(this, item, true)
                }
            }
            mAdapter.notifyDataSetChanged()
        }

        mAdapter.setOnItemClickListener { adapter, view, position ->
            val item = mAdapter.getItem(position)
            TrainResultActivity.start(this, item, true)
        }

        //删除选中数据
        binding.top.tvDel.setOnClickListener {
            if (selectList.size > 0) {
                selectList.forEachIndexed { index, item ->
                    LitePal.delete(TrainingDTO::class.java, item.id)
                    mDataList.remove(item)
                }
                selectList.clear()
                mAdapter.setList(mDataList)
            }
        }
    }

    // 创建菜单：
    private val mSwipeMenuCreator =
        SwipeMenuCreator { leftMenu, rightMenu, position ->
            val deleteItem = SwipeMenuItem(this)
//            deleteItem.text = "删除"
            deleteItem.setImage(R.mipmap.icon_wm_del)
            deleteItem.setTextColor(Color.WHITE)
            deleteItem.width = SizeUtils.dp2px(60f)
            rightMenu.addMenuItem(deleteItem) // 在Item右侧添加一个菜单。
            // 注意：哪边不想要菜单，那么不要添加即可。
        }

    /**
     * RecyclerView的Item的Menu点击监听。
     */
    private val mItemMenuClickListener =
        OnItemMenuClickListener { menuBridge, position ->
            menuBridge.closeMenu()
            val direction = menuBridge.direction // 左侧还是右侧菜单。
            val menuPosition = menuBridge.position // 菜单在RecyclerView的Item中的Position。
            if (direction == SwipeRecyclerView.RIGHT_DIRECTION) {
                // 普通Item。
                val trainingDTO = mAdapter.data[position]
                LitePal.delete(TrainingDTO::class.java, trainingDTO.id)
                mDataList.removeAt(position)
                mAdapter.data.removeAt(position)
                mAdapter.notifyItemRemoved(position)
            } else if (direction == SwipeRecyclerView.LEFT_DIRECTION) {

            }
        }

    /**
     * 对入参保留最多两位小数(舍弃末尾的0)，如:
     * 3.345->3.34
     * 3.40->3.4
     * 3.0->3
     */
    private fun getNoMoreThanTwoDigits(number: Float): String {
        val format = DecimalFormat("0.#")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(number)
    }

    private val mAdapter = object :
        BaseQuickAdapter<TrainingDTO, BaseViewHolder>(R.layout.item_statistical) {
        override fun convert(holder: BaseViewHolder, item: TrainingDTO) {
            holder.setText(R.id.tvName, if (!TextUtils.isEmpty(item.name)) item.name else "无名")
                .setText(R.id.tvModel, if (item.isCheck) "考核" else "训练")
                .setText(R.id.tvTime, TimeUtils.stampToDate(item.endTime))
            val cbCheck = holder.getView<CheckBox>(R.id.cbCheck)
            if (item.isCheck) {
                holder.setText(R.id.tvResult, getNoMoreThanTwoDigits(item.getScoreTotal()) + "分")
            } else {
                holder.setText(R.id.tvResult, "--")
            }
            cbCheck.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isDel) {
                    if (isChecked) {
                        selectList.add(item)
                    } else {
                        selectList.remove(item)
                    }
                }
            }

            if (isDel) {
                cbCheck.visibility = View.VISIBLE
            } else {
                cbCheck.visibility = View.INVISIBLE
            }
            cbCheck.isChecked = item.isCheckBox
        }
    }

}

