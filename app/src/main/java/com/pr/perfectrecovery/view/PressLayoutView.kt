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
import com.pr.perfectrecovery.utils.DataVolatile
import kotlin.math.abs

/**
 * 自定义按压组合控件
 * 2022-1-3 10:12:55
 */
class PressLayoutView : LinearLayout {
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
        viewLayout = LayoutInflater.from(context).inflate(R.layout.layout_press, this, true)
        viewTop = findViewById(R.id.viewTop)
        viewBottom = findViewById(R.id.viewBottom)
        viewCenter = findViewById(R.id.viewCenter)
        viewPress = findViewById(R.id.viewPress)
        linearLayout = findViewById(R.id.linearLayout)
        ivArrowUp = findViewById(R.id.ivArrowUp)
        ivArrowDown = findViewById(R.id.ivArrowDown)
    }

    private var newY = 0
    private var prCount = 0
    private var down = 0
    private var isDown = false
    private var isRate = false
    fun smoothScrollTo(destY: Int, prSum: Int) {
        //距离值：  30-150
        var destY = destY
        destY = getNumber(destY)
        val topHeight = viewTop!!.height
        val height = height - (viewPress!!.height + 10)
        newY = abs(height.toFloat() / 10 * destY).toInt()
        //        Log.d("smoothScrollTo", "destY : " + destY);
//        Log.d("smoothScrollTo", "newY : " + newY);
//        Log.d("smoothScrollTo", "height : " + height);
        viewPress!!.visibility = VISIBLE
        ivArrowUp!!.visibility = INVISIBLE
        ivArrowDown!!.visibility = INVISIBLE
        viewPress!!.visibility = VISIBLE
        viewTop!!.isChecked = false
        viewBottom!!.isChecked = false
        if (destY == 0) {
            down = 0
            isDown = false
            isRate = false
            viewTop!!.isChecked = true
        }
        val scrollY = linearLayout!!.scrollY
        scroller!!.startScroll(linearLayout!!.scrollX, scrollY, 0, -newY - scrollY)
        if (destY == 9) { //正确的按压
            isDown = true
            viewBottom!!.isChecked = true
            Log.e("smoothScrollTo", "按压正确")
            viewPress!!.visibility = INVISIBLE
            ivArrowUp!!.visibility = INVISIBLE
            ivArrowDown!!.visibility = INVISIBLE
        } else if (prSum != prCount && destY > 2) { //按压过大
            isDown = true
            prCount = prSum
            Log.e("smoothScrollTo", "按压过大")
            if (mScrollerCallBack != null) {
                mScrollerCallBack!!.onScrollerState(TYPE_MAX)
            }
        } else if (!isDown) {
            if (-newY - scrollY > 20) { //向上滚动
//                if (down != 0 && down < 9) {
//                    isDown = true;
//                    ivArrowUp.setVisibility(View.INVISIBLE);
//                    ivArrowDown.setVisibility(View.VISIBLE);
//                    Log.e("smoothScrollTo", "按压不足");
//                    if (mScrollerCallBack != null) {
//                        mScrollerCallBack.onScrollerState(TYPE_MIN);
//                    }
//                }
                //为滚动到顶部时向下按压提示 未回弹
                Log.e("smoothScrollTo", "向上滚动：" + (-newY - scrollY))
            } else { //向下滚动
                if (destY > 0) {
                    viewTop!!.isChecked = false
                    Log.e("smoothScrollTo", "向下滚动：" + (-newY - scrollY))
                    if (down > 0 && down > destY) {
                        isRate = false
                        Log.e("smoothScrollTo", "按压未回弹")
                        ivArrowUp!!.visibility = VISIBLE
                        ivArrowDown!!.visibility = INVISIBLE
                        if (mScrollerCallBack != null) {
                            mScrollerCallBack!!.onScrollerState(TYPE_UP)
                        }
                    }
                }
                isDown = false
                down = destY
            }
        }
        invalidate()
    }

    fun setDown() {
        ivArrowUp!!.visibility = INVISIBLE
        ivArrowDown!!.visibility = VISIBLE
    }

    private fun setViewStatus() {
        Log.d("viewY", "" + newY)
        if (newY > viewTop!!.height) {
            viewPress!!.visibility = VISIBLE
        } else {
            viewPress!!.visibility = INVISIBLE
        }
    }

    private fun getNumber(value: Int): Int {
        val number = DataVolatile.preDistance.toInt() - value
        //Log.e("getNumber", "$value   计算后的按压距离值：$number")
        if (number < 10) {
            return 0
        }
        return when {
            number < 10 -> {
                1
            }
            number < 15 -> {
                2
            }
            number < 20 -> {
                3
            }
            number < 25 -> {
                4
            }
            number < 30 -> {
                5
            }
            number < 35 -> {
                6
            }
            number < 40 -> {
                7
            }
            number < 45 -> {
                8
            }
            number <= 65 -> {
                9
            }
            number > 65 -> {
                10
            }
            else -> {
                0
            }
        }
    }
//    private fun getNumber(number: Int): Int {
//        return if (number < 45) {
//            10
//        } else if (number <= 60) {
//            9
//        } else if (number < 70) {
//            8
//        } else if (number < 80) {
//            7
//        } else if (number < 90) {
//            6
//        } else if (number < 100) {
//            5
//        } else if (number < 110) {
//            4
//        } else if (number < 130) {
//            3
//        } else if (number < 150) {
//            2
//        } else if (number < 170) {
//            1
//        } else {
//            0
//        }
//    }

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