package com.pr.perfectrecovery.activity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.databinding.ActivityCprListBinding
import com.pr.perfectrecovery.fragment.CPRScoreFragment
import com.pr.perfectrecovery.fragment.CPRStandardFragment

/**
 * 基础参数配置界面
 */
class ConfigActivity : BaseActivity() {

    private lateinit var viewBinding: ActivityCprListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCprListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {
        viewBinding.bottom.tvContinue.visibility = View.INVISIBLE
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        //分页
        val fragments = mutableListOf<Fragment>()
        fragments.add(CPRStandardFragment.newInstance())
        fragments.add(CPRScoreFragment.newInstance())
        viewBinding.bottom.tvPage.text = "1/${fragments.size}"
        viewBinding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewBinding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }

        }

        viewBinding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                viewBinding.bottom.tvPage.text = "${position + 1}/${fragments.size}"
            }
        })

    }


}