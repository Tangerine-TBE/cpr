package com.pr.perfectrecovery.activity

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.bean.TrainingDTO
import com.pr.perfectrecovery.databinding.ActivityStatisticalBinding
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
        binding.top.tvRight.setOnClickListener { }
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
        GlobalScope.launch(Dispatchers.IO) {
            mDataList = LitePal.findAll(TrainingDTO::class.java) as ArrayList<TrainingDTO>
            withContext(Dispatchers.Main) {
                mAdapter.setList(mDataList)
            }
        }

        binding.top.tvRight.setOnClickListener {
            isDel = !isDel
            if (isDel) {
                binding.top.tvRight.text = "取消"
                binding.top.tvDel.visibility = View.VISIBLE
            } else {
                selectList.clear()
                binding.top.tvRight.text = "管理"
                binding.top.tvDel.visibility = View.INVISIBLE
            }
            mAdapter.notifyDataSetChanged()
        }

        mAdapter.setOnItemClickListener { adapter, view, position ->
            val item = mAdapter.getItem(position)
            if (isDel) {
                val mCheckBox = view.findViewById<CheckBox>(R.id.cbCheck)
                mCheckBox.isChecked = !mCheckBox.isChecked
                if (mCheckBox.isChecked) {
                    selectList.add(item)
                    item.isCheckBox = true
                } else {
                    item.isCheckBox = false
                    selectList.remove(item)
                }
                mAdapter.data[position] = item
            } else {
                TrainResultActivity.start(this, item, true)
            }
        }

        //删除选中数据
        binding.top.tvDel.setOnClickListener {
            val ids = arrayOf("")
            if (selectList.size > 0) {
                selectList.forEachIndexed { index, item ->
                    ids[index] = "${item.id}"
                    mDataList.remove(item)
                    mAdapter.remove(item)
                }
                LitePal.deleteAll(TrainingDTO::class.java, *ids)
                selectList.clear()
                mAdapter.notifyDataSetChanged()
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

    private val mAdapter = object :
        BaseQuickAdapter<TrainingDTO, BaseViewHolder>(R.layout.item_statistical) {
        override fun convert(holder: BaseViewHolder, item: TrainingDTO) {
            holder.setText(R.id.tvName, if (!TextUtils.isEmpty(item.name)) item.name else "无名")
                .setText(R.id.tvModel, if (item.isCheck) "考核" else "训练")
                .setText(R.id.tvTime, TimeUtils.stampToDate(System.currentTimeMillis()))
                .setText(R.id.tvResult, "${item.score}分")
            val cbCheck = holder.getView<CheckBox>(R.id.cbCheck)

            if (isDel) {
                cbCheck.visibility = View.VISIBLE
            } else {
                cbCheck.visibility = View.INVISIBLE
            }
            cbCheck.isChecked = item.isCheckBox
        }
    }

}

