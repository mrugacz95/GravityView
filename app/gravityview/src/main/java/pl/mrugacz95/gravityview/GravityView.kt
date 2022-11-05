package pl.mrugacz95.gravityview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Transformation
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


class GravityView(context: Context?, attrs: AttributeSet?, defStyle: Int) : ViewGroup(context, attrs, defStyle) {

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private var initiallyPositioned = false
    val engine = Engine()
    private val coroutineScope = MainScope() + Job()
    private val androidViewToEngineRectangle = HashMap<View, Rectangle>()
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
        val box = Rectangle(false)
        box.width = 1.0
        box.height = 1.0
        box.mass = 1.0
        box.pos = Vec2(5.0, 5.0)
        engine.add(box)
    }

    fun start() {
        coroutineScope.launch(Dispatchers.Default) {
            engine.start(this@GravityView)
        }
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
        val rectangle = Rectangle(false)
        androidViewToEngineRectangle[child] = rectangle
        val childLayoutParams = (child.layoutParams as GravityLayoutParams)
        rectangle.mass = childLayoutParams.mass
        rectangle.rotation = childLayoutParams.rotation
        engine.add(rectangle)
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
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child ?: return
            val heightMeasure = when (child.layoutParams.height) {
                LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
                LayoutParams.WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                else -> MeasureSpec.makeMeasureSpec(child.layoutParams.height, MeasureSpec.EXACTLY)
            }
            val widthMeasure = when (child.layoutParams.width) {
                LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
                LayoutParams.WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                else -> MeasureSpec.makeMeasureSpec(child.layoutParams.width, MeasureSpec.EXACTLY)
            }
            child.measure(widthMeasure, heightMeasure)
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
            if (!initiallyPositioned) {
                androidViewToEngineRectangle[child]?.pos = Vec2(
                    x = (childLayoutParams.marginStart + childWidth / 2f) / scale,
                    y = (childLayoutParams.topMargin + childHeight / 2f) / scale
                )
            }
            val rectangle = androidViewToEngineRectangle[child]
            rectangle ?: continue
            rectangle.width = childWidth / scale
            rectangle.height = childHeight / scale
        }
        initiallyPositioned = true
    }



    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        for (body in engine) {
            if (!androidViewToEngineRectangle.containsValue(body)) {
                body.draw(canvas)
            }
        }
    }

    override fun getChildStaticTransformation(child: View?, t: Transformation?): Boolean {
        return true
    }


    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        child ?: return false
        val b = Bitmap.createBitmap(child.width, child.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val paint = Paint()
        val m = Matrix()
        val rectangle = androidViewToEngineRectangle[child] ?: return true
        val leftTopCorner = viewLeftTopCornerVec(child)
        m.postTranslate(-leftTopCorner.x.toFloat(), -leftTopCorner.y.toFloat())
        m.postRotate(Utils.radToDeg(rectangle.rotation).toFloat())
        m.postTranslate(
            ((rectangle.pos.x) * scale).toFloat(),
            ((rectangle.pos.y) * scale).toFloat()
        )
        child.draw(c)
        canvas?.drawBitmap(b, m, paint)
        return true
    }

    private fun viewLeftTopCornerVec(view: View): Vec2 {
        return Vec2(view.width.toFloat() / 2.0, view.height.toFloat() / 2.0)
    }

    private fun Body.draw(canvas: Canvas) {
        if (this is Rectangle) {
            val path = Path()
            val axes = transformedAxes
            path.moveTo(
                (axes[0].p1.x * scale).toFloat(),
                (axes[0].p1.y * scale).toFloat()
            )
            for (i in axes.indices) {
                path.lineTo(
                    (axes[i].p2.x * scale).toFloat(),
                    (axes[i].p2.y * scale).toFloat()
                )
            }
            path.close()
            canvas.drawPath(path, Paint(color))
        }
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

        init {
            val a = context.theme.obtainStyledAttributes(
                attrs, R.styleable.GravityView_LayoutParams, 0, 0
            )
            mass = a.getFloat(R.styleable.GravityView_LayoutParams_mass, 1f).toDouble()
            rotation = a.getFloat(R.styleable.GravityView_LayoutParams_rotation, 0f).toDouble()
            a.recycle()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val eventVec2 = event.toVec2()
        val worldPos = eventVec2 / scale
        for ((childView, body) in androidViewToEngineRectangle.entries) {
            if (body.isInside(worldPos)) {
                val ev = event.transformEventToViewLocal(body, childView)
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
}


