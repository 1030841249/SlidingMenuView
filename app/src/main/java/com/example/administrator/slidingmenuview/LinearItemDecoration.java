package com.example.administrator.slidingmenuview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by Admior on 2018/1/6.
 * 下划线
 */

public class LinearItemDecoration extends RecyclerView.ItemDecoration {
    private Paint mPaint;

    private int mColor;

    public LinearItemDecoration(@ColorInt int color) {
        mPaint = new Paint();
        mPaint.setColor(color);
        //启动抗锯齿
        mPaint.setAntiAlias(true);
        //启动防抖动
        mPaint.setDither(true);
    }

    public LinearItemDecoration() {
        this(0);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP
                , 1, parent.getResources().getDisplayMetrics());
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        View childView;
        RecyclerView.LayoutParams params;
        //一共有多少个itemView
        int childCount = layoutManager.getChildCount();
        //矩形
        Rect drawRect = new Rect();
        int top, left, right, bottom;
        //要绘制的数量少1，表示底部不用绘制
        for (int childIndex = 0; childIndex < childCount - 1; childIndex++) {
            //获取指定位置的view
            childView = layoutManager.getChildAt(childIndex);
          params = (RecyclerView.LayoutParams) childView.getLayoutParams();
            top = childView.getBottom() + params.bottomMargin;
            left = childView.getLeft() + params.leftMargin + childView.getPaddingLeft();
            right = childView.getRight() + params.rightMargin + childView.getPaddingRight();
            bottom = top + height;
            drawRect.set(left, top, right, bottom);
            c.drawRect(drawRect, mPaint);

        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, parent.getResources().getDisplayMetrics());
        outRect.set(0, 0, 0, height);
    }
}
