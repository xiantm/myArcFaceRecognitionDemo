package com.yryp.facehelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class SquareScanView : View {

    val mPaint = Paint()
    //方形占view 0-1
    var n = 1f
    private var mLeft = 0
    private var mRight = 0
    private var mTop = 0
    private var mBottom = 0
    private lateinit var mTopRect: Rect
    private lateinit var mBottomRect: Rect
    private lateinit var mLeftRect: Rect
    private lateinit var mRightRect: Rect
    private lateinit var lineRect: Rect

    @JvmOverloads
    constructor(context: Context, n: Float, attrs: AttributeSet? = null, def: Int = 0) : super(context, attrs, def) {
        this.n = n

        mPaint.style = Paint.Style.FILL_AND_STROKE
    }


    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            mPaint.color = Color.parseColor("#66000000")
            it.drawRect(mLeftRect, mPaint)
            it.drawRect(mRightRect, mPaint)
            it.drawRect(mTopRect, mPaint)
            it.drawRect(mBottomRect, mPaint)
            mPaint.color = Color.GREEN
            it.drawRect(lineRect, mPaint)
            if (lineRect.bottom >= mBottom) {
                lineRect.top = mTop
                lineRect.bottom = mTop + 2
            } else {
                lineRect.top += 2
                lineRect.bottom += 2
            }
            postInvalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = getMySize(100, widthMeasureSpec)
        var height = getMySize(100, heightMeasureSpec)

        setMeasuredDimension(width, height)
        var edgeWidth = (height * n).toInt()
        if (width <= height) {
            edgeWidth = (width * n).toInt()
        }
        mLeft = (width - edgeWidth) / 2
        mTop = (height - edgeWidth) / 2
        mRight = width - mLeft
        mBottom = height - mTop
        mLeftRect = Rect(0, mTop, mLeft, mBottom)
        mTopRect = Rect(0, 0, width, mTop)
        mRightRect = Rect(mRight, mTop, width, mBottom)
        mBottomRect = Rect(0, mBottom, width, height)
        lineRect = Rect(mLeft, mTop, mRight, mTop + 2)
    }

    private fun getMySize(defaultSize: Int, measureSpec: Int): Int {
        var mySize = defaultSize

        val mode = View.MeasureSpec.getMode(measureSpec)
        val size = View.MeasureSpec.getSize(measureSpec)

        when (mode) {
            View.MeasureSpec.UNSPECIFIED -> {//如果没有指定大小,就设置为默认大小
                mySize = defaultSize
            }
            View.MeasureSpec.AT_MOST -> {//如果测量模式是最大取值为size
                //我们将大小取最大值,你也可以取其他值
                mySize = size
            }
            View.MeasureSpec.EXACTLY -> {//如果是固定的大小,那就不要去改变它
                mySize = size
            }
        }
        return mySize
    }
}