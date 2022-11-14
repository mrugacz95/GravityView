package pl.mrugacz95.gravityview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginStart
import androidx.core.view.marginTop


class GravityView(context: Context?, attrs: AttributeSet?, defStyle: Int) : ViewGroup(context, attrs, defStyle) {

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    val engine = Engine()
    private val androidViewToRenderingData = HashMap<View, RenderingData>()
    private val scale = 100.0
    private val solidFrame = List(4) { Rectangle(true) }

    init {
        setWillNotDraw(false)

        val a: TypedArray = context!!.obtainStyledAttributes(
            attrs,
            R.styleable.GravityView, 0, 0
        )
        val gravity: Float = a.getFloat(R.styleable.GravityView_gravity, 9.81f)
        engine.g = gravity.toDouble()
        a.recycle()
        engine.addAll(solidFrame)
    }

    fun start() {
        engine.start(this)
    }

    fun stop() {
        engine.stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun addView(child: View, params: LayoutParams?) {
        super.addView(child, params)
        val childLayoutParams = (child.layoutParams as GravityLayoutParams)
        val rectangle = Rectangle(childLayoutParams.isStatic)
        rectangle.mass = childLayoutParams.mass
        rectangle.rotation = childLayoutParams.rotation
        androidViewToRenderingData[child] = RenderingData(rectangle, null, null)
        engine.add(rectangle)
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        androidViewToRenderingData.remove(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val frameThickness = 1.0
        // left
        solidFrame[0].width = frameThickness
        solidFrame[0].height = measuredHeight / scale
        solidFrame[0].pos = Vec2(-frameThickness / 2, measuredHeight / 2.0 / scale)
        // right
        solidFrame[1].width = frameThickness
        solidFrame[1].height = measuredHeight / scale
        solidFrame[1].pos = Vec2(measuredWidth / scale + frameThickness / 2, measuredHeight / 2.0 / scale)
        // top
        solidFrame[2].width = measuredWidth / scale
        solidFrame[2].height = frameThickness
        solidFrame[2].pos = Vec2(measuredWidth / 2.0 / scale - 1, -frameThickness / 2)
        // bottom
        solidFrame[3].width = measuredWidth / scale
        solidFrame[3].height = frameThickness
        solidFrame[3].pos = Vec2(measuredWidth / 2.0 / scale, measuredHeight / scale + frameThickness / 2.0)

        measureChildren(widthMeasureSpec, heightMeasureSpec)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child ?: continue
            val renderingData = androidViewToRenderingData[child]
            renderingData ?: continue
            val childWidth: Int = child.measuredWidth
            val childHeight: Int = child.measuredHeight
            renderingData.rectangle.width = childWidth / scale
            renderingData.rectangle.height = childHeight / scale
            val bitmap = Bitmap.createBitmap(childWidth, childHeight, Bitmap.Config.ARGB_8888)
            renderingData.bitmap = bitmap
            renderingData.canvas = Canvas(bitmap)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child ?: continue
            val childWidth: Int = child.measuredWidth
            val childHeight: Int = child.measuredHeight
            val marginTop = child.marginTop
            val marginStart = child.marginStart
            child.layout(
                marginStart,
                marginTop,
                marginStart + childWidth,
                marginTop + childHeight
            )
            val childLayoutParams = child.layoutParams as GravityLayoutParams
            if (!childLayoutParams.initiallyPositioned) {
                androidViewToRenderingData[child]?.rectangle?.pos = Vec2(
                    x = (childLayoutParams.marginStart + childWidth / 2f) / scale,
                    y = (childLayoutParams.topMargin + childHeight / 2f) / scale
                )
                childLayoutParams.initiallyPositioned = true
            }
        }
    }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        child ?: return false
        if (child.width == 0 || child.height == 0) {
            return false
        }
        val renderingData = androidViewToRenderingData[child] ?: return false
        val paint = Paint()
        val m = Matrix()
        val leftTopCorner = viewLeftTopCornerVec(child)
        m.postTranslate(-leftTopCorner.x.toFloat(), -leftTopCorner.y.toFloat())
        m.postRotate(Utils.radToDeg(renderingData.rectangle.rotation).toFloat())
        m.postTranslate(
            ((renderingData.rectangle.pos.x) * scale).toFloat(),
            ((renderingData.rectangle.pos.y) * scale).toFloat()
        )
        renderingData.canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        child.draw(renderingData.canvas)
        renderingData.bitmap?.let { bitmap ->
            canvas?.drawBitmap(bitmap, m, paint)
        }
        return true
    }

    private fun viewLeftTopCornerVec(view: View): Vec2 {
        return Vec2(view.width.toFloat() / 2.0, view.height.toFloat() / 2.0)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return GravityLayoutParams(context, null)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        if (p == null) return false
        if (p !is GravityLayoutParams) return false
        return true
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return GravityLayoutParams(context, attrs)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    class GravityLayoutParams(context: Context, attrs: AttributeSet?) : MarginLayoutParams(context, attrs) {
        val mass: Double
        val rotation: Double
        val isStatic: Boolean
        var initiallyPositioned = false

        init {
            val a = context.theme.obtainStyledAttributes(
                attrs, R.styleable.GravityView_LayoutParams, 0, 0
            )
            mass = a.getFloat(R.styleable.GravityView_LayoutParams_mass, 1.0f).toDouble()
            rotation = a.getFloat(R.styleable.GravityView_LayoutParams_rotation, 0f).toDouble()
            isStatic = a.getBoolean(R.styleable.GravityView_LayoutParams_isStatic, false)
            a.recycle()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val eventVec2 = event.toVec2()
        val worldPos = eventVec2 / scale
        for ((childView, data) in androidViewToRenderingData.entries) {
            if (data.rectangle.isInside(worldPos)) {
                val ev = event.transformEventToViewLocal(data.rectangle, childView)
                childView.dispatchTouchEvent(ev)
                return true
            }
        }
        return false
    }

    private fun MotionEvent.toVec2() = Vec2(x.toDouble(), y.toDouble())

    private fun MotionEvent.transformEventToViewLocal(body: Body, childView: View): MotionEvent {
        val eventVec2 = this.toVec2()
        val leftTopCornerClientPosition = (body.pos * scale) - viewLeftTopCornerVec(childView).rotate(body.rotation)
        val inChildrenPosition = (eventVec2 - leftTopCornerClientPosition).rotate(-body.rotation)
        return this.replaceTouchPosition(inChildrenPosition.x.toFloat(), inChildrenPosition.y.toFloat())
    }

    private fun MotionEvent.replaceTouchPosition(x: Float, y: Float): MotionEvent {
        return with(this) {
            MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                x,
                y,
                pressure,
                size,
                metaState,
                xPrecision,
                yPrecision,
                deviceId,
                edgeFlags
            )
        }
    }

    fun getBodies(): List<Body> {
        return engine
    }

    data class RenderingData(
        val rectangle: Rectangle,
        var canvas: Canvas?,
        var bitmap: Bitmap?,
    )
}


