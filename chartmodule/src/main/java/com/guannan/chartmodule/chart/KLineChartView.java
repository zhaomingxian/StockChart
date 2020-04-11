package com.guannan.chartmodule.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.guannan.chartmodule.R;
import com.guannan.chartmodule.data.KLineToDrawItem;
import com.guannan.chartmodule.gesture.ChartGestureListener;
import com.guannan.chartmodule.gesture.ChartTouchListener;
import com.guannan.chartmodule.helper.ChartDataSourceHelper;
import com.guannan.chartmodule.helper.OnChartDataCountListener;
import com.guannan.chartmodule.utils.DisplayUtils;
import com.guannan.chartmodule.utils.LogUtils;
import com.guannan.chartmodule.utils.NumberFormatUtils;
import com.guannan.chartmodule.utils.PaintUtils;
import com.guannan.simulateddata.entity.KLineItem;
import java.util.ArrayList;
import java.util.List;

/**
 * @author guannan
 * @date on 2020-03-07 15:56
 * @des K线的主要实现类
 */
public class KLineChartView extends BaseChartView
    implements ChartGestureListener, OnChartDataCountListener<List<KLineToDrawItem>> {

  private ChartTouchListener mChartLineTouchListener;
  private Paint mPaintRed;
  private ChartDataSourceHelper mHelper;
  private Paint mPaintGreen;
  private List<KLineToDrawItem> mToDrawList;
  private float maxPrice;
  private float minPrice;

  private int TEXT_PADDING;
  private int CAL_PADDING;

  public KLineChartView(Context context) {
    this(context, null);
  }

  public KLineChartView(Context context,
      @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public KLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mChartLineTouchListener = new ChartTouchListener(this);
    mChartLineTouchListener.setChartGestureListener(this);

    mPaintRed = new Paint();
    mPaintRed.setColor(ContextCompat.getColor(context, R.color.color_fd4331));

    mPaintGreen = new Paint();
    mPaintGreen.setColor(ContextCompat.getColor(context, R.color.color_05aa3b));

    TEXT_PADDING = DisplayUtils.dip2px(context, 5);
    CAL_PADDING = DisplayUtils.dip2px(context, 3);
  }

  @Override
  protected void drawFrame(Canvas canvas) {
    super.drawFrame(canvas);
    drawOutLine(canvas);
    if (mToDrawList == null) {
      return;
    }
    for (int i = 0; i < mToDrawList.size(); i++) {
      KLineToDrawItem drawItem = mToDrawList.get(i);
      if (drawItem != null) {
        // 绘制蜡烛线日期（只绘制每月第一个交易日）
        if (!TextUtils.isEmpty(drawItem.date)) {
          RectF contentRect = mViewPortHandler.mContentRect;
          Rect rect = new Rect();
          PaintUtils.TEXT_PAINT.getTextBounds(drawItem.date, 0, drawItem.date.length(), rect);
          canvas.drawText(drawItem.date, drawItem.rect.centerX() - rect.width() / 2,
              contentRect.bottom + rect.height() + TEXT_PADDING,
              PaintUtils.TEXT_PAINT);
          canvas.drawLine(drawItem.rect.centerX(), contentRect.top, drawItem.rect.centerX(),
              contentRect.bottom, PaintUtils.GRID_INNER_DIVIDER);
        }
        // 绘制蜡烛线
        if (drawItem.isFall) {
          canvas.drawRect(drawItem.rect, mPaintGreen);
          canvas.drawRect(drawItem.shadowRect, mPaintGreen);
        } else {
          canvas.drawRect(drawItem.rect, mPaintRed);
          canvas.drawRect(drawItem.shadowRect, mPaintRed);
        }
      }
    }
  }

  /**
   * 绘制行情图边框
   */
  private void drawOutLine(Canvas canvas) {
    RectF contentRect = mViewPortHandler.mContentRect;
    if (contentRect != null) {

      Path path = new Path();
      path.moveTo(contentRect.left, contentRect.top);
      path.lineTo(contentRect.right, contentRect.top);
      path.lineTo(contentRect.right, contentRect.bottom);
      path.lineTo(contentRect.left, contentRect.bottom);
      path.close();
      canvas.drawPath(path, PaintUtils.GRID_DIVIDER);
    }

    // 绘制价格刻度和价格分隔线 TODO
    maxPrice = NumberFormatUtils.format(maxPrice, 2);
    minPrice = NumberFormatUtils.format(minPrice, 2);
    Rect rect = new Rect();
    PaintUtils.TEXT_PAINT.getTextBounds(maxPrice + "", 0, String.valueOf(maxPrice).length(), rect);
    canvas.drawText(maxPrice + "", contentRect.left + CAL_PADDING,
        contentRect.top + rect.height() + CAL_PADDING,
        PaintUtils.TEXT_PAINT);
    float perHeight = contentRect.height() / 4;
    float perPrice = NumberFormatUtils.format((maxPrice - minPrice) / 4, 2);
    for (int i = 1; i <= 3; i++) {
      canvas.drawLine(contentRect.left, contentRect.top + perHeight * i, contentRect.right,
          contentRect.top + perHeight * i, PaintUtils.GRID_INNER_DIVIDER);
      float value = NumberFormatUtils.format(maxPrice - perPrice * i, 2);
      canvas.drawText(value + "", contentRect.left + CAL_PADDING,
          contentRect.top + perHeight * i - CAL_PADDING, PaintUtils.TEXT_PAINT);
    }
    canvas.drawText(minPrice + "", contentRect.left + CAL_PADDING, contentRect.bottom - CAL_PADDING,
        PaintUtils.TEXT_PAINT);
  }

  /**
   * 初始化数据
   */
  public void initData(ArrayList<KLineItem> klineList) {
    if (mViewPortHandler != null) {
      int dip10 = DisplayUtils.dip2px(getContext(), 10);
      int dip20 = DisplayUtils.dip2px(getContext(), 20);
      mViewPortHandler.restrainViewPort(dip10, dip20, dip10, dip20);
    }
    if (mHelper == null) {
      mHelper = new ChartDataSourceHelper(this);
    }
    mHelper.initKDrawData(klineList, mViewPortHandler);
  }

  @Override
  public void onChartGestureStart(MotionEvent me,
      ChartTouchListener.ChartGesture lastPerformedGesture) {
    LogUtils.d("触摸开始");
  }

  @Override
  public void onChartGestureEnd(MotionEvent me,
      ChartTouchListener.ChartGesture lastPerformedGesture) {
    LogUtils.d("触摸结束");
  }

  @Override
  public void onChartLongPressed(MotionEvent me) {

  }

  @Override
  public void onChartDoubleTapped(MotionEvent me) {

  }

  @Override
  public void onChartSingleTapped(MotionEvent me) {

  }

  @Override
  public void onChartFling(float distanceX) {
    if (mHelper != null) {
      mHelper.initKMoveDrawData(distanceX);
    }
    invalidateView();
  }

  @Override
  public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

  }

  @Override
  public void onChartTranslate(MotionEvent me, float dX) {
    if (mHelper != null) {
      mHelper.initKMoveDrawData(dX);
    }
    invalidateView();
  }

  @Override
  public void onReady(List<KLineToDrawItem> data, float maxValue, float minValue) {
    this.mToDrawList = data;
    this.maxPrice = maxValue;
    this.minPrice = minValue;
    invalidateView();
  }
}