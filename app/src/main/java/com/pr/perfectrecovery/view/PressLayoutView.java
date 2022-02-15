package com.pr.perfectrecovery.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.pr.perfectrecovery.R;

/**
 * 自定义按压组合控件
 * 2022-1-3 10:12:55
 */
public class PressLayoutView extends LinearLayout {
    private Scroller scroller;

    public PressLayoutView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PressLayoutView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PressLayoutView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public PressLayoutView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    /**
     * 第三步：重写computeScroll（）方法
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            linearLayout.scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();//重绘，在重绘调用draw（）方法中，内部会调用View的computeScroll()方法
        } else {
            setViewStatus();
        }
    }

    private View viewLayout, viewCenter;
    private View viewPress;
    private CheckedTextView viewTop, viewBottom;
    private LinearLayout linearLayout;
    private ImageView ivArrowUp, ivArrowDown;

    //初始化UI，可根据业务需求设置默认值。
    private void initView(Context context) {
        scroller = new Scroller(context);
        viewLayout = LayoutInflater.from(context).inflate(R.layout.layout_press, this, true);
        viewTop = findViewById(R.id.viewTop);
        viewBottom = findViewById(R.id.viewBottom);
        viewCenter = findViewById(R.id.viewCenter);
        viewPress = findViewById(R.id.viewPress);
        linearLayout = findViewById(R.id.linearLayout);
        ivArrowUp = findViewById(R.id.ivArrowUp);
        ivArrowDown = findViewById(R.id.ivArrowDown);
    }

    private int newY;
    private int prCount = 0;
    private int up = 0;
    private int down = 0;
    private boolean isDown = false;
    private boolean isME = false;

    public void smoothScrollTo(int destY, int prSum) {
        //距离值：  30-150
        destY = getNumber(destY);
        int topHeight = viewTop.getHeight();
        int height = getHeight() - (viewPress.getHeight() + 10);
        newY = (int) (((float) height / 10) * destY);
//        Log.d("smoothScrollTo", "destY : " + destY);
//        Log.d("smoothScrollTo", "newY : " + newY);
//        Log.d("smoothScrollTo", "height : " + height);
        viewPress.setVisibility(View.VISIBLE);
        ivArrowUp.setVisibility(View.INVISIBLE);
        ivArrowDown.setVisibility(View.INVISIBLE);
        viewPress.setVisibility(View.VISIBLE);
        viewBottom.setChecked(false);
        if (destY == 0) {
            up = 0;
            down = 0;
            isDown = false;
            viewTop.setChecked(true);
        }

        int scrollY = linearLayout.getScrollY();
//        if (newY - scrollY > 5) {//向上滚动
//            Log.e("smoothScrollTo", "向上滚动" + (scrollY - newY + 30));
//            scroller.startScroll(linearLayout.getScrollX(), scrollY, 0, -newY - scrollY);
//        } else if (newY - scrollY < 0) {//向下滚动
        Log.e("smoothScrollTo", "向下滚动" + (-newY - scrollY));
        scroller.startScroll(linearLayout.getScrollX(), scrollY, 0, -newY - scrollY);
//        }
        if (destY == 9) {//正确的按压
            isDown = true;
            viewBottom.setChecked(true);
            Log.e("smoothScrollTo", "按压正确");
            viewPress.setVisibility(View.INVISIBLE);
            ivArrowUp.setVisibility(View.INVISIBLE);
            ivArrowDown.setVisibility(View.INVISIBLE);
        } else if (prSum != prCount && destY > 9) {//按压过大
            isDown = true;
            Log.e("smoothScrollTo2", prCount + "");
            prCount = prSum;
            Log.e("smoothScrollTo", "按压过大");
            if (mScrollerCallBack != null) {
                mScrollerCallBack.onScrollerState(TYPE_MAX);
            }
        } else if (!isDown) {
            if (-newY - scrollY > 5) {//向上滚动
                if (down != 0 && down < 9) {
                    isDown = true;
                    ivArrowUp.setVisibility(View.INVISIBLE);
                    ivArrowDown.setVisibility(View.VISIBLE);
                    Log.e("smoothScrollTo", "按压不足");
                    if (mScrollerCallBack != null) {
                        mScrollerCallBack.onScrollerState(TYPE_MIN);
                    }
                }
            } else {//向下滚动
                if (destY > 0) {
                    viewTop.setChecked(false);
                    if (up > 0) {//按压未回弹
                        Log.e("smoothScrollTo", "按压未回弹");
                        ivArrowUp.setVisibility(View.VISIBLE);
                        ivArrowDown.setVisibility(View.INVISIBLE);
                        Log.e("smoothScrollTo", "按压未回弹");
                        if (mScrollerCallBack != null) {
                            mScrollerCallBack.onScrollerState(TYPE_UP);
                        }
                    }
                    up = destY;
                }
                isDown = false;
                down = destY;
            }
        }
        invalidate();
    }

    public void setDown() {
        ivArrowUp.setVisibility(View.INVISIBLE);
        ivArrowDown.setVisibility(View.VISIBLE);
    }

    private void setViewStatus() {
        Log.d("viewY", "" + newY);
        if (newY > viewTop.getHeight()) {
            viewPress.setVisibility(View.VISIBLE);
        } else {
            viewPress.setVisibility(View.INVISIBLE);
        }
    }

    private int getNumber(int number) {
        if (number < 120) {
            return 10;
        } else if (number <= 130) {
            return 9;
        } else if (number <= 135) {
            return 8;
        } else if (number <= 140) {
            return 7;
        } else if (number <= 145) {
            return 6;
        } else if (number <= 150) {
            return 5;
        } else if (number <= 155) {
            return 4;
        } else if (number <= 160) {
            return 3;
        } else if (number <= 165) {
            return 2;
        } else if (number <= 170) {
            return 1;
        } else {
            return 0;
        }
    }

    public final static int TYPE_UP = 1;//未回弹
    public final static int TYPE_MAX = 2;//按压过大
    public final static int TYPE_MIN = 3;//按压不足
    private ScrollerCallBack mScrollerCallBack;

    public void setScrollerCallBack(ScrollerCallBack mScrollerCallBack) {
        this.mScrollerCallBack = mScrollerCallBack;
    }

    public interface ScrollerCallBack {
        void onScrollerState(int state);
    }
}
