package com.pr.perfectrecovery.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.pr.perfectrecovery.R;

public class LoadingDot extends LinearLayout {
    private ValueAnimator valueAnimator;

    public LoadingDot(Context context) {
        super(context);
        init();
    }

    public LoadingDot(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingDot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    int position = -1;
    private boolean isWhite;

    public void setIsWhite(boolean isWhite) {
        this.isWhite = isWhite;

    }

    private void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        valueAnimator = ValueAnimator.ofInt(0, 7).setDuration(1200);
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int i = (int) animation.getAnimatedValue();
                View view = new View(getContext());
                LayoutParams lp = new LayoutParams(20, 20);
                lp.setMarginStart(10);
                if (position == i) {
                    return;
                }
                Log.i("TAG", i + "");
                position = i;
                switch (i) {
                    case 0:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 1:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_90_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_90_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 2:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_80_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_80_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 3:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_70_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_70_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 4:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_50_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_50_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 5:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_30_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_30_5dp));
                        }
                        addView(view, lp);
                        break;
                    case 6:
                        if (!isWhite) {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_wihte_alpah_10_5dp));
                        } else {
                            view.setBackground(getContext().getResources().getDrawable(R.drawable.dot_grenn_alpah_10_5dp));
                        }
                        addView(view, lp);
                        removeAllViews();
                        invalidate();
                        break;
                }
            }
        });
    }

}
