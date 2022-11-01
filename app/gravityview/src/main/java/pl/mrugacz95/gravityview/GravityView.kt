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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


class GravityView(context: Context?, attrs: AttributeSet?, defStyle: Int) : ViewGroup(context, attrs, defStyle) {

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private val engine = Engine()
    private val coroutineScope = MainScope() + Job()
    private val androidViewToEngineRectangle = HashMap<View, Rectangle>()
    private val scale = 100.0

    init {
        setWillNotDraw(false)

        val a: TypedArray = context!!.obtainStyledAttributes(
            attrs,
            R.styleable.GravityView, 0, 0
        )
        val gravity: Float = a.getFloat(R.styleable.GravityView_gravity, 9.81f)
        engine.g = gravity.toDouble()
        a.recycle()

        val solidGround = Rectangle(true)
        solidGround.pos = Vec2(5.0, 9.0)
        solidGround.width = 9.0
        solidGround.height = 1.5
        solidGround.mass = 1.0
        engine.add(solidGround)


        val leftWall = Rectangle(true)
        leftWall.pos = Vec2(0.0, 5.5)
        leftWall.width = 1.0
        leftWall.height = 10.0
        leftWall.mass = 1.0
        engine.add(leftWall)

        val rightWall = Rectangle(true)
        rightWall.pos = Vec2(10.0, 5.5)
        rightWall.width = 1.0
        rightWall.height = 10.0
        rightWall.mass = 1.0
        engine.add(rightWall)

        val box = Rectangle(false)
        box.width = 1.0
        box.height = 1.0
        box.mass = 1.0
        box.pos = Vec2(5.0, 3.0)
        engine.add(box)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coroutineScope.launch(Dispatchers.Default) {
            engine.start(this@GravityView)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        engine.stop()
    }

    override fun addView(child: View, params: LayoutParams?) {
        super.addView(child, params)
        val rectangle = Rectangle(false)
        androidViewToEngineRectangle[child] = rectangle
        val childLayoutParams = (child.layoutParams as GravityLayoutParams)
        rectangle.pos = Vec2(childLayoutParams.posX / scale, childLayoutParams.posY / scale)
        engine.add(rectangle)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child ?: return
            child.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            val childWidth: Int = child.measuredWidth
            val childHeight: Int = child.measuredHeight
            child.layout(0, 0, childWidth, childHeight)
            val rectangle = androidViewToEngineRectangle[child]
            rectangle ?: continue
            rectangle.width = childWidth / scale
            rectangle.height = childHeight / scale
        }
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
        m.postTranslate(-child.width.toFloat() / 2f, -child.height.toFloat() / 2f)
        m.postRotate(Utils.radToDeg(rectangle.rotation).toFloat())
        m.postTranslate(
            ((rectangle.pos.x) * scale).toFloat(),
            ((rectangle.pos.y) * scale).toFloat()
        )
        child.draw(c)
        canvas?.drawBitmap(b, m, paint)
        return true
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

    class GravityLayoutParams(context: Context?, attrs: AttributeSet?) : LayoutParams(context, attrs) {
        val posX: Int
        val posY: Int

        init {
            val a = context!!.theme.obtainStyledAttributes(
                attrs, R.styleable.GravityView_LayoutParams, 0, 0
            )
            posX = a.getDimensionPixelSize(R.styleable.GravityView_LayoutParams_x_pos, 0)
            posY = a.getDimensionPixelSize(R.styleable.GravityView_LayoutParams_y_pos, 0)
            a.recycle()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val x = event.x
        val y = event.y
        val pos = Vec2(x / scale, y / scale)
        for ((body, rect) in androidViewToEngineRectangle.entries) {
            if (rect.AABB.isInside(pos)) { //todo change to more accurate collision
                body.onTouchEvent(event)
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

}
