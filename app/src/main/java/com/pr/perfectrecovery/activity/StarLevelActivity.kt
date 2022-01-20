package com.pr.perfectrecovery.activity

import android.os.Bundle
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.databinding.ActivityStarLevelBinding
import com.pr.perfectrecovery.fragment.StarLevelFragment

class StarLevelActivity : BaseActivity() {
    private lateinit var binding: ActivityStarLevelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStarLevelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.bottom.ivBack.setOnClickListener { finish() }
        supportFragmentManager.beginTransaction()
            .replace(binding.containerView.id, StarLevelFragment.newInstance()).commit()
    }

}