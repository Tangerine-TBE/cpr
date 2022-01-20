package com.pr.perfectrecovery.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.databinding.ActivityAssessmentBinding
import com.pr.perfectrecovery.databinding.ActivityCprListBinding

/**
 * 考核评分
 */
class AssessmentScoreActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityAssessmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAssessmentBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {

    }
}