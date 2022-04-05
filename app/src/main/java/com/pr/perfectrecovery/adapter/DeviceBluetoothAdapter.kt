package com.pr.perfectrecovery.adapter

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.databinding.ItemBluetoothBinding

/**
 * 蓝牙适配器
 */
class DeviceBluetoothAdapter :
    BaseQuickAdapter<BleDevice, BaseViewHolder>(R.layout.item_bluetooth) {
    override fun convert(holder: BaseViewHolder, item: BleDevice) {
        val viewBinding = ItemBluetoothBinding.bind(holder.itemView)
        viewBinding.tvBluetoothName.text = "完美复苏  " + item.name
        if (item.count > 0) {
            viewBinding.tvBluetoothStatus.text = "${item.count}"
        } else {
            viewBinding.tvBluetoothStatus.text = ""
        }
        //判断蓝牙是否链接
        if (item.isConnected) {//该蓝牙已连接
            viewBinding.tvBluetoothName.setTextColor(context.resources.getColor(R.color.color_37B48B))
            viewBinding.tvBluetoothStatus.setTextColor(context.resources.getColor(R.color.color_37B48B))
            viewBinding.loadingDot2.visibility =
                if (item.isLoading) View.VISIBLE else View.INVISIBLE
            viewBinding.loadingDot.visibility = View.INVISIBLE
            viewBinding.battery.visibility = View.VISIBLE
            viewBinding.battery.power =  item.power
        } else {
            viewBinding.loadingDot2.visibility = View.INVISIBLE
            viewBinding.loadingDot.visibility = if (item.isLoading) View.VISIBLE else View.INVISIBLE
            viewBinding.root.setBackgroundColor(context.resources.getColor(R.color.theme_color))
            viewBinding.tvBluetoothName.setTextColor(context.resources.getColor(R.color.white))
            viewBinding.tvBluetoothStatus.setTextColor(context.resources.getColor(R.color.white))
            viewBinding.battery.visibility = View.GONE
        }

        //处理动画效果不生效问题
//        if (holder.itemView.tag != null) {
//            holder.itemView.removeOnAttachStateChangeListener(holder.itemView.tag as View.OnAttachStateChangeListener) //移除旧的监听器
//        }
//        val listener: View.OnAttachStateChangeListener = object : View.OnAttachStateChangeListener {
//            override fun onViewAttachedToWindow(v: View) {
//                viewBinding.loadingDot.visibility =
//                    if (item.isLoading) View.VISIBLE else View.INVISIBLE
//            }
//
//            override fun onViewDetachedFromWindow(v: View) {}
//        }
//        holder.itemView.addOnAttachStateChangeListener(listener)
//        holder.itemView.tag = listener // 保存监听器对象。
    }
}