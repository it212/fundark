@file:Suppress("unused")

package com.silencefly96.module_common.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.*
import com.silencefly96.module_common.R
import kotlin.math.abs
import kotlin.math.min

/**
 * 滚动选择文字控件
 * 核心思想
 * 1、有两层不同大小及透明度的选项，选中项放在中间
 * 2、接受一个列表的数据，最多显示三个值，三层五个值有点麻烦
 * 3、滑动会造成三个选项滚动，大小透明度发生变化
 * 4、滚动一定距离后，判定是否选中一个项目，并触发动画滚动到选定项
 * 5、尝试做出循环滚动效果
 */
class ScrollSelectView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attributeSet, defStyleAttr){

    //默认字体透明度，大小不应该指定，应该根据view的高度按百分比适应
    companion object{
        const val DEFAULT_BIG_TRANSPARENCY = 255
        const val DEFAULT_SMALL_TRANSPARENCY = (255 * 0.5f).toInt()
    }

    //两层字体大小及透明度
    private var mainSize: Float = 0f
    private var secondSize: Float = 0f

    private val mainAlpha: Int
    private val secondAlpha: Int

    //数据
    var mData: List<String>? = null

    //选择数据index
    var mCurrentIndex: Int = 0

    //单次事件序列累计滑动值
    private var mScrollY: Float = 0f

    //上次事件纵坐标
    private var mLastY: Float = 0f

    //画笔
    private val mPaint: Paint

    init {
        //读取XML参数，设置相关属性
        val attrArr = context.obtainStyledAttributes(attributeSet, R.styleable.ScrollSelectView)
        //三层字体透明度设置，未设置使用默认值
        mainAlpha = attrArr.getInteger(R.styleable.ScrollSelectView_mainAlpha,
            DEFAULT_BIG_TRANSPARENCY)
        secondAlpha = attrArr.getInteger(R.styleable.ScrollSelectView_secondAlpha,
            DEFAULT_SMALL_TRANSPARENCY)
        //回收
        attrArr.recycle()

        //设置画笔，在构造中初始化，不要写在onDraw里面，onDraw会不断触发
        mPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            style = Paint.Style.FILL
            //该方法即为设置基线上那个点究竟是left,center,还是right
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }
    }

    //设置控件的默认大小，实际viewgroup不需要
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        //根据父容器给定大小，设置自身宽高，wrap_content需要用到默认值
        val width = getSizeFromMeasureSpec(300, widthMeasureSpec)
        val height = getSizeFromMeasureSpec(200, heightMeasureSpec)
        //设置测量宽高，一定要设置，不然错给你看
        setMeasuredDimension(width, height)

        //得到自身高度后，就可以按比例设置字体大小了
        setFontSize(height)
    }

    private fun setFontSize(totalHeight: Int) {
        //按6：3：1的比例设置字体大小
        mainSize = totalHeight * 6 / 10f
        secondSize = totalHeight / 10f
    }

    //根据MeasureSpec确定默认宽高，MeasureSpec限定了该view可用的大小
    private fun getSizeFromMeasureSpec(defaultSize: Int, measureSpec: Int): Int {
        //获取MeasureSpec内模式和尺寸
        val mod = getMode(measureSpec)
        val size = getSize(measureSpec)

        return when (mod) {
            EXACTLY -> size
            AT_MOST -> min(defaultSize, size)
            else -> defaultSize //MeasureSpec.UNSPECIFIED
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //绘制中间项
        drawMainItem(mPaint, canvas)
        //绘制其他两项
        drawSecondItem(mPaint, canvas)
        //绘制由于滑动新创建的item
        drawNewItem(mPaint, canvas)
    }

    private fun drawMainItem(paint: Paint, canvas: Canvas?) {
        //变化值为主次两个控件中线内的高度，不考虑delta>1，大于时在move已切换了MainItem
        val delta = mScrollY / (measuredHeight / 2f + measuredHeight / 4f) / 2f
        val dSize = (mainSize - secondSize) * delta
        val dAlpha = (mainAlpha - secondAlpha) * delta
        val dy = mScrollY
        paint.textSize = mainSize - dSize
        paint.alpha = (mainAlpha - dAlpha).toInt()

        //中心点，但是绘制是从基线开始的，在中心点下方
        //受滑动影响，修改y即可
        val x = measuredWidth / 2f
        val y = measuredHeight / 2f - dy
        //绘制字体的参数，受字体大小样式影响
        val fmi = paint.fontMetricsInt
        //top为基线到字体上边框的距离（负数），bottom为基线到字体下边框的距离（正数）
        //基线中间点的y轴计算公式，即中心点加上字体高度的一半，基线中间点x就是中心点x
        val baseline = y - (fmi.top + fmi.bottom) / 2f
        val mainStr = if (mCurrentIndex - 1 < 0 || mCurrentIndex + 1 >= mData!!.size) ""
            else mData!![mCurrentIndex - 1]
        canvas?.drawText(mainStr, x, baseline, mPaint)
    }

    private fun drawSecondItem(paint: Paint, canvas: Canvas?) {
        //变化的比例只和mainItem有关，即之和切换和不切换直接的状态有关
        val delta = mScrollY / (measuredHeight / 2f + measuredHeight / 4f) / 2f

        //绘制上面项目
        var dSize: Float
        var dAlpha: Float
        var dy: Float
        if (delta > 0) {
            //上面项目变为消失
            dSize = secondSize * delta
            dAlpha = secondAlpha * delta
            //消失的高度为次item的高度一半
            dy = measuredHeight / 4f / 2f * delta
        }else {
            //上面项目变为选中
            dSize = (mainSize - secondSize) * delta
            dAlpha = (mainAlpha - secondAlpha) * delta
            //选中的高度为主次item高度和的一半
            dy = (measuredHeight / 2f + measuredHeight / 4f) / 2f * delta
        }

        paint.textSize = secondSize - dSize
        paint.alpha = (secondAlpha - dAlpha).toInt()
        //中心点，上面项目的高度占1/4，再求中心点y即是1/8
        var x = measuredWidth / 2f
        var y = measuredHeight / 8f - dy
        val fmi = paint.fontMetricsInt
        var baseline = y - (fmi.top + fmi.bottom) / 2f
        val topStr = if (mCurrentIndex - 1 < 0) "" else mData!![mCurrentIndex - 1]
        canvas?.drawText(topStr, x, baseline, mPaint)

        //绘制下面项目
        if (delta > 0) {
            //下面项目变为选中
            dSize = (mainSize - secondSize) * delta
            dAlpha = (mainAlpha - secondAlpha) * delta
            //选中的高度为主次item高度和的一半
            dy = (measuredHeight / 2f + measuredHeight / 4f) / 2f * delta
        }else {
            //下面项目变为消失
            dSize = secondSize * delta
            dAlpha = secondAlpha * delta
            //消失的高度为次item的高度一半
            dy = measuredHeight / 4f / 2f * delta
        }

        paint.textSize = secondSize + dSize
        paint.alpha = (secondAlpha + dAlpha).toInt()
        x = measuredWidth / 2f
        y = measuredHeight  * 7 / 8f + dy
        baseline = y - (fmi.top + fmi.bottom) / 2f
        val bottomStr = if (mCurrentIndex + 1 >= mData!!.size) "" else mData!![mCurrentIndex + 1]
        canvas?.drawText(bottomStr, x, baseline, mPaint)
    }

    private fun drawNewItem(paint: Paint, canvas: Canvas?) {
        //变化的比例只和mainItem有关，即之和切换和不切换直接的状态有关
        //新项目从无到有，delta只为正数
        val delta = abs(mScrollY / (measuredHeight / 2f + measuredHeight / 4f) / 2f)
        //新项目终态就是次item
        val dSize = secondSize * delta
        val dAlpha = secondAlpha * delta
        val dy = measuredHeight / 4f / 2f * delta

        paint.textSize = 0f + dSize
        paint.alpha = 0 + dAlpha.toInt()

        val x = measuredWidth / 2f
        val y = if (mScrollY > 0) {
            //从下面出现
            measuredHeight - dy
        } else {
            //从上面出现
            0 + dy
        }

        val fmi = paint.fontMetricsInt
        val baseline = y - (fmi.top + fmi.bottom) / 2f
        //新的item和选中的index相差为2
        val newItemIndex = if (mScrollY > 0) mCurrentIndex + 2 else mCurrentIndex - 2
        val topStr = if (newItemIndex < 0 || newItemIndex >= mData!!.size) ""
            else mData!![newItemIndex - 1]
        canvas?.drawText(topStr, x, baseline, mPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    //按下开始计算滑动距离
                    mScrollY = 0f
                    mLastY = it.y
                }
                MotionEvent.ACTION_MOVE -> move(event)
                MotionEvent.ACTION_UP -> stopMove()
            }
        }
        //view是最末端了，应该拦截touch事件，不然事件序列将舍弃
        return true
    }

    private fun move(e: MotionEvent) {
        //累加滑动距离
        mScrollY += mLastY - e.y
        //如果滑动距离切换了选中值，重绘前修改选中值
        if (mScrollY > (measuredHeight / 2f + measuredHeight / 4f) / 2f) {
            mCurrentIndex++
            //等于已经切换了，用完的mScrollY不需要了
            mScrollY = 0f
        }else if (mScrollY < -(measuredHeight / 2f + measuredHeight / 4f) / 2f){
            mCurrentIndex--
            mScrollY = 0f
        }
        //滑动后触发重绘，绘制时处理滑动效果
        invalidate()
    }

    private fun stopMove() {
        //结束滑动后判定，滑动距离超过四分之一（即mainItem的一半）就切换了选中项
        val leftScrollY: Float = when {
            mScrollY > measuredHeight / 4f -> measuredHeight / 2f - mScrollY
            mScrollY < -measuredHeight /4f -> -measuredHeight / 2f - mScrollY
            //滑动没有达到切换选中项效果，应该恢复到原先状态
            else -> -mScrollY
        }

        //这里使用ValueAnimator处理剩余的距离，模拟滑动到需要的位置
        val animator = if (leftScrollY > 0) ValueAnimator.ofFloat(0f, leftScrollY)
            else ValueAnimator.ofFloat(leftScrollY, 0f)
        animator.addUpdateListener { animation ->
            mScrollY = animation.animatedValue as Float
            invalidate()
        }
        animator.duration = 300
        animator.start()
    }
}