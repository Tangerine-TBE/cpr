/**
 * Copyright 2014  XCL-Charts
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @Project XCL-Charts
 * @Description Android图表基类库演示
 * @author XiongChuanLiang<br />(xcl_168@aliyun.com)
 * @license http://www.apache.org/licenses/  Apache v2 License
 * @version 1.3
 */
package com.pr.perfectrecovery.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.Log;

import org.xclcharts.chart.DialChart;
import org.xclcharts.common.DensityUtil;
import org.xclcharts.common.MathHelper;
import org.xclcharts.renderer.XEnum;
import org.xclcharts.view.GraphicalView;

import java.util.ArrayList;
import java.util.List;

public class DialChart07View extends GraphicalView {

    private String TAG = "DialChart07View";

    private DialChart chart180 = new DialChart();
    private float mPercentage = 0.9f;

    public DialChart07View(Context context) {
        super(context);
        initView();
    }

    public DialChart07View(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DialChart07View(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }


    private void initView() {
        chartRender180();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //super.onSizeChanged(w, h, oldw, oldh);
        chart180.setChartRange(w, (float) h);
    }

    public void chartRender180() {
        try {
            chart180.setTotalAngle(120f);
            chart180.setStartAngle(210f);

            //设置当前百分比
            chart180.getPointer().setPercentage(0);

            //设置指针长度
            chart180.getPointer().setPointerStyle(XEnum.PointerStyle.TRIANGLE);
            chart180.getPointer().setLength(0.7f, 0f);
            getBaseCirclePaint();
            getPointerPaint();

            List<Float> ringPercentage = new ArrayList<Float>();
            float rper = MathHelper.getInstance().div(1, 3); //相当于40%	//270, 4
            ringPercentage.add(rper);
            ringPercentage.add(rper);
            ringPercentage.add(rper);

            List<Integer> rcolor = new ArrayList<Integer>();
            rcolor.add(Color.rgb(144, 144, 144));
            rcolor.add(Color.rgb(61, 179, 142));
            rcolor.add(Color.rgb(144, 144, 144));
            chart180.addStrokeRingAxis(0.75f, 0.6f, ringPercentage, rcolor);
//            chart180.getPlotAxis().get(0).getFillAxisPaint().setColor(Color.TRANSPARENT);
            chart180.getPlotAxis().get(0).getFillAxisPaint().setColor(Color.rgb(6, 7, 9));

            Paint paintTB = new Paint();
            paintTB.setColor(Color.WHITE);
            paintTB.setTextAlign(Align.CENTER);
            paintTB.setTextSize(22);
            paintTB.setAntiAlias(true);
            //chart180.getPlotAttrInfo().addAttributeInfo(XEnum.Location.BOTTOM, "180度仪表盘", 0.5f, paintTB);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, e.toString());
        }

    }

    public void getPointerPaint() {
        mPaintPoint = chart180.getPointer().getPointerPaint();
        mPaintPoint.setColor(Color.rgb(6, 7, 9));
        mPaintPoint.setStrokeWidth(10);
        mPaintPoint.setStrokeCap(Paint.Cap.SQUARE);
//        mPaintPoint.setStrokeMiter(2);
        mPaintPoint.setStrokeJoin(Paint.Join.MITER);
        mPaintPoint.setStyle(Style.FILL_AND_STROKE);
        mPaintPoint.setAntiAlias(true);
    }

    Paint mPaintBaseCircle;
    Paint mPaintPoint;

    public void getBaseCirclePaint() {
        mPaintBaseCircle = chart180.getPointer().getBaseCirclePaint();
        mPaintBaseCircle.setStyle(Style.FILL);
        mPaintBaseCircle.setAntiAlias(true);
        mPaintBaseCircle.setColor(Color.rgb(6, 7, 9));
        mPaintBaseCircle.setStrokeWidth(4);
    }


    public void addAxis() {
        List<String> rlabels2 = new ArrayList<String>();
        for (int i = 0; i < 7; i++) {
            rlabels2.add(Integer.toString(i * 10));
        }

        List<String> rlabels3 = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            if (0 == i) {
                rlabels3.add("");
            } else
                rlabels3.add(Integer.toString(i * 10));
        }
    }

    //增加指针
    public void addPointer() {

    }

    private void addAttrInfo() {
        Paint paintTB = new Paint();
        paintTB.setColor(Color.WHITE);
        paintTB.setTextAlign(Align.CENTER);
        paintTB.setTextSize(22);
        paintTB.setAntiAlias(true);
    }

    public void setCurrentStatus(float percentage) {
        mPercentage = percentage;
        chart180.clearAll();
        //设置当前百分比
        addAxis();
        //增加指针
        addPointer();
        addAttrInfo();
        chartRender180();
        chart180.getPointer().setPercentage(mPercentage);
    }

    @Override
    public void render(Canvas canvas) {
        // TODO Auto-generated method stub
        try {
            chart180.render(canvas);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

}

