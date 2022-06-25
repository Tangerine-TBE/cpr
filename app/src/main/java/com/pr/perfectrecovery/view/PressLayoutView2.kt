package com.pr.perfectrecovery.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Scroller
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.bean.BaseDataDTO
import kotlin.math.abs

/**
 * 自定义按压组合控件
 * 2022-1-3 10:12:55
 */
class PressLayoutView2 : LinearLayout {
    private var scroller: Scroller? = null

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView(context)
    }

    /**
     * 第三步：重写computeScroll（）方法
     */
    override fun computeScroll() {
        super.computeScroll()
        if (scroller!!.computeScrollOffset()) {
            linearLayout!!.scrollTo(scroller!!.currX, scroller!!.currY)
            invalidate() //重绘，在重绘调用draw（）方法中，内部会调用View的computeScroll()方法
        } else {
            setViewStatus()
        }
    }

    private var viewLayout: View? = null
    private var viewCenter: View? = null
    private var viewPress: View? = null
    private var viewTop: CheckedTextView? = null
    private var viewBottom: CheckedTextView? = null
    private var linearLayout: LinearLayout? = null
    private var ivArrowUp: ImageView? = null
    private var ivArrowDown: ImageView? = null

    //初始化UI，可根据业务需求设置默认值。
    private fun initView(context: Context) {
        scroller = Scroller(context)
        viewLayout = LayoutInflater.from(context).inflate(R.layout.layout_press2, this, true)
        viewTop = findViewById(R.id.viewTop)
        viewBottom = findViewById(R.id.viewBottom)
        viewCenter = findViewById(R.id.viewCenter)
        viewPress = findViewById(R.id.viewPress)
        linearLayout = findViewById(R.id.linearLayout)
        ivArrowUp = findViewById(R.id.ivArrowUp)
        ivArrowDown = findViewById(R.id.ivArrowDown)
    }

    private var newY = 0
    fun smoothScrollTo(destY: Int, dataDTO: BaseDataDTO) {
        Log.e("smoothScrollTo_1", "$destY")
        //距离值：  30-150
        var destY = destY
        destY = getNumber(destY, dataDTO)
        val height = height - (viewPress!!.height + 10)
        newY = abs(height.toFloat() / 10 * destY).toInt()

        viewPress!!.visibility = VISIBLE
        ivArrowUp!!.visibility = INVISIBLE
        ivArrowDown!!.visibility = INVISIBLE
        viewPress!!.visibility = VISIBLE
        viewTop!!.isChecked = false
        viewBottom!!.isChecked = false
        if (destY == 0) {
            viewTop!!.isChecked = true
        }
        val scrollY = linearLayout!!.scrollY
        scroller!!.startScroll(linearLayout!!.scrollX, scrollY, 0, -newY - scrollY)
        if (destY == 9) { //正确的按压
            viewBottom!!.isChecked = true
            Log.e("smoothScrollTo", "按压正确")
            viewPress!!.visibility = INVISIBLE
            ivArrowUp!!.visibility = INVISIBLE
            ivArrowDown!!.visibility = INVISIBLE
        }
        invalidate()
    }

    fun setDown() {
        ivArrowUp!!.visibility = INVISIBLE
        ivArrowDown!!.visibility = VISIBLE
    }

    fun setUnBack() {
        ivArrowUp!!.visibility = VISIBLE
        ivArrowDown!!.visibility = INVISIBLE
    }

    private fun setViewStatus() {
        Log.d("viewY", "" + newY)
        if (newY > viewTop!!.height) {
            viewPress!!.visibility = VISIBLE
        } else {
            viewPress!!.visibility = INVISIBLE
        }
    }

    private fun getNumber(value: Int, dataDTO: BaseDataDTO): Int {
        val number = abs(dataDTO.preDistance - value)
        val depthSegment = dataDTO.PR_LOW_VALUE / 8
        Log.e("depth", "depthSegment: $depthSegment")
        Log.e("depth", "number: $number")
        Log.e("depth", "${dataDTO.PR_LOW_VALUE}/${dataDTO.PR_HIGH_VALUE}")
        if (number < 10) {
            return 0
        }
        return when {
            number < depthSegment -> {
                1
            }
            number < depthSegment.toFloat() * 2.0 -> {
                2
            }
            number < depthSegment.toFloat() * 3.0 -> {
                3
            }
            number < depthSegment.toFloat() * 4.0 -> {
                4
            }
            number < depthSegment.toFloat() * 5.0 -> {
                5
            }
            number < depthSegment.toFloat() * 6.0 -> {
                6
            }
            number < depthSegment.toFloat() * 7.0 -> {
                7
            }
            number < depthSegment.toFloat() * 8.0 -> {
                8
            }
            number in (dataDTO.PR_LOW_VALUE)..(dataDTO.PR_HIGH_VALUE) -> {
                9
            }
            number > dataDTO.PR_HIGH_VALUE -> {
                10
            }
            else -> {
                0
            }
        }
    }

    private var mScrollerCallBack: ScrollerCallBack? = null
    fun setScrollerCallBack(mScrollerCallBack: ScrollerCallBack?) {
        this.mScrollerCallBack = mScrollerCallBack
    }

    interface ScrollerCallBack {
        fun onScrollerState(state: Int)
    }

    companion object {
        const val TYPE_UP = 1 //未回弹
        const val TYPE_MAX = 2 //按压过大
        const val TYPE_MIN = 3 //按压不足
    }
}