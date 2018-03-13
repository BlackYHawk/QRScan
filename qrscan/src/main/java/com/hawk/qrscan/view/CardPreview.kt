package com.hawk.qrscan.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.zxing.ResultPoint
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager

/**
 * Created by heyong on 2018/3/6.
 */
class CardPreview constructor(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        private val DefaultAnimationInterval = 10L         //刷新界面的时间
        private val DefaultOpaque = 0xFF                   //背景图片透明度
        private val DefaultBottomTextSize = 12             //下面字体大小
        private val DefaultTextPadding = 20                //字体距离扫描框下面的距离
        private val DefaultCornerWidth = 10                //四个绿色边角对应的宽度
        private val DefaultMiddleLineWidth = 6             //扫描框中的中间线的宽度
        private val DefaultMiddleLinePadding = 5           //扫描框中的中间线的与扫描框左右的间隙
        private val DefaultSpeenDistance = 5               //中间那条线每次刷新移动的距离
    }
    /**
     * 画笔对象的引用
     */
    private var paint: Paint? = null
    /**
     * 手机的屏幕密度
     */
    private var density: Float = 0.toFloat()
    /**
     * 手机的屏幕宽度
     */
    private var widthPixels: Int = 0;
    /**
     * 四个绿色边角对应的长度
     */
    private var ScreenRate: Int = 0
    /**
     * 扫描框下面的字
     */
    private var bottomHint: String? = null
    /**
     * 中间滑动线的最顶端位置
     */
    private var slideTop: Int = 0
    /**
     * 中间滑动线的最底端位置
     */
    private var slideBottom: Int = 0
    /**
     * 将扫描的二维码拍下
     */
    private var maskColor: Int = 0
    private var hintColor: Int = 0

    private var resultPointColor: Int = 0
    private var possibleResultPoints: HashSet<ResultPoint>? = null
    private var lastPossibleResultPoints: HashSet<ResultPoint>? = null

    private var isFirst: Boolean = false
    private var top = true

    init {
        density = context.getResources().getDisplayMetrics().density
        widthPixels = context.resources.displayMetrics.widthPixels
        ScreenRate = (20 * density).toInt()
        bottomHint = context.getString(R.string.scan_card)
        paint = Paint()
        val resources = resources
        maskColor = resources.getColor(R.color.viewfinder_mask)
        hintColor = resources.getColor(R.color.blue)

        resultPointColor = resources.getColor(R.color.possible_result_points)
        possibleResultPoints = HashSet(5)
    }

    override fun onDraw(canvas: Canvas?) {
        //中间的扫描框，你要修改扫描框的大小，去CameraManager里面修改
        val frame = CameraManager.get()?.getCardFramingRect() ?: return

        //初始化中间线滑动的最上边和最下边
        if (!isFirst) {
            isFirst = true
            slideTop = frame.top
            slideBottom = frame.bottom
        }

        drawMask(canvas!!, frame)
        drawRect(canvas!!, frame)
        drawLine(canvas!!, frame)
        drawBottom(canvas!!, frame)
        drawResultPoint(canvas!!, frame)

        //只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(DefaultAnimationInterval, frame.left, frame.top,
                frame.right, frame.bottom)
    }

    private fun drawMask(canvas: Canvas, frame: Rect) {
        //获取屏幕的宽和高
        val width = canvas.getWidth()
        val height = canvas.getHeight()

        paint?.setColor(maskColor)
        //画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        //扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(),
                (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)
    }

    private fun drawRect(canvas: Canvas, frame: Rect) {
        //画扫描框边上的角，总共8个部分
        paint?.setColor(Color.GREEN)
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), (frame.left + ScreenRate).toFloat(),
                (frame.top + DefaultCornerWidth).toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), (frame.left + DefaultCornerWidth).toFloat(),
                (frame.top + ScreenRate).toFloat(), paint)
        canvas.drawRect((frame.right - ScreenRate).toFloat(), frame.top.toFloat(), frame.right.toFloat(),
                (frame.top + DefaultCornerWidth).toFloat(), paint)
        canvas.drawRect((frame.right - DefaultCornerWidth).toFloat(), frame.top.toFloat(), frame.right.toFloat(),
                (frame.top + ScreenRate).toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), (frame.bottom - DefaultCornerWidth).toFloat(), (frame.left + ScreenRate).toFloat(),
                frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), (frame.bottom - ScreenRate).toFloat(),
                (frame.left + DefaultCornerWidth).toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect((frame.right - ScreenRate).toFloat(), (frame.bottom - DefaultCornerWidth).toFloat(),
                frame.right.toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect((frame.right - DefaultCornerWidth).toFloat(), (frame.bottom - ScreenRate).toFloat(),
                frame.right.toFloat(), frame.bottom.toFloat(), paint)
    }

    private fun drawLine(canvas: Canvas, frame: Rect) {
        //绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
        slideTop += DefaultSpeenDistance
        if (slideTop >= frame.bottom) {
            slideTop = frame.top
        }

        canvas.drawRect((frame.left + DefaultMiddleLinePadding).toFloat(), (slideTop - DefaultMiddleLineWidth / 2).toFloat(),
                (frame.right - DefaultMiddleLinePadding).toFloat(), (slideTop + DefaultMiddleLineWidth / 2).toFloat(), paint)
    }

    private fun drawBottom(canvas: Canvas, frame: Rect) {
        //画扫描框下面的字
        paint?.setColor(Color.WHITE)
        paint?.setTextSize(DefaultBottomTextSize * density)
        paint?.setAlpha(0x5a)
        paint?.setTypeface(Typeface.create("System", Typeface.BOLD))
        paint?.setTextAlign(Paint.Align.CENTER)
        canvas.drawText(bottomHint, (widthPixels / 2).toFloat(), frame.bottom + DefaultTextPadding.toFloat() * density, paint)
    }

    private fun drawResultPoint(canvas: Canvas, frame: Rect) {
        val currentPossible = possibleResultPoints
        val currentLast = lastPossibleResultPoints

        if (currentPossible == null || currentPossible.isEmpty()) {
            lastPossibleResultPoints = null
        } else {
            possibleResultPoints = HashSet(5)
            lastPossibleResultPoints = currentPossible
            paint?.setAlpha(DefaultOpaque)
            paint?.setColor(resultPointColor)

            for (point in currentPossible) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint)
            }
        }
        if (currentLast != null) {
            paint?.setAlpha(DefaultOpaque / 2)
            paint?.setColor(resultPointColor)

            for (point in currentLast) {
                canvas.drawCircle(frame.left + point.x, frame.top + point.y, 3.0f, paint)
            }
        }
    }

    fun drawViewfinder() {
        invalidate()
    }

    fun addPossibleResultPoint(point: ResultPoint) {
        possibleResultPoints?.add(point)
    }

}