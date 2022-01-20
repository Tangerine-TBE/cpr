package com.pr.perfectrecovery.activity

import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.databinding.ActivityStatisticalBinding
import com.pr.perfectrecovery.utils.TimeUtils
import com.yanzhenjie.recyclerview.OnItemMenuClickListener
import com.yanzhenjie.recyclerview.SwipeMenuCreator
import com.yanzhenjie.recyclerview.SwipeMenuItem


class StatisticalActivity : BaseActivity() {

    private lateinit var binding: ActivityStatisticalBinding

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
        binding.recyclerview.adapter = adapter
        val listData = mutableListOf<String>()
        for (item in 1..10) {
            listData.add("完美复苏$item")
        }
        adapter.setList(listData)
    }

    // 创建菜单：
    private val mSwipeMenuCreator =
        SwipeMenuCreator { leftMenu, rightMenu, position ->
//            val deleteItem = SwipeMenuItem(mContext)
//            // 各种文字和图标属性设置。
//            leftMenu.addMenuItem(deleteItem); // 在Item左侧添加一个菜单。
            val deleteItem = SwipeMenuItem(this)
            // 各种文字和图标属性设置。
            rightMenu.addMenuItem(deleteItem) // 在Item右侧添加一个菜单。
            // 注意：哪边不想要菜单，那么不要添加即可。
        }

    private var mItemMenuClickListener =
        OnItemMenuClickListener { menuBridge, position -> // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu()
            // 左侧还是右侧菜单：
            val direction = menuBridge.direction
            // 菜单在Item中的Position：
            val menuPosition = menuBridge.position
            adapter.remove(adapter.getItem(position))
        }

    private val adapter = object :
        BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_statistical) {
        override fun convert(holder: BaseViewHolder, item: String) {
            holder.setText(R.id.tvName, "刘XX" + holder.adapterPosition)
                .setText(R.id.tvModel, "练习")
                .setText(R.id.tvTime, TimeUtils.stampToDate(System.currentTimeMillis()))
                .setText(R.id.tvResult, "优秀")
        }
    }
}

