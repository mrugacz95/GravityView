package pl.mrugacz95.gravityview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
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
    private val andoridViewToEngineRectangle = HashMap<View, Rectangle>()
    private val scale = 100f

    init {
        setWillNotDraw(false)

        val a: TypedArray = context!!.obtainStyledAttributes(
            attrs,
            R.styleable.GravityView, 0, 0
        )
        val gravity: Float = a.getFloat(R.styleable.GravityView_gravity, 9.81f)
        engine.g = gravity.toDouble()
        a.recycle()
        val solidGround = Rectangle(5.0, 1.0, 1.0, true)
        solidGround.pos = Vec2(5.0, 8.0)
        engine.add(solidGround)

        val box = Rectangle(1.0, 1.0, 1.0, false)
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            val width: Int = child.measuredWidth
            val height: Int = child.measuredHeight
            child.layout(0, 0, width, height)
            val rectangle = Rectangle(width.toDouble() / scale, height.toDouble() / scale, 1.toDouble(), false)
            andoridViewToEngineRectangle[child] = rectangle
            engine.add(rectangle)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.color = Color.parseColor("#B6B6B6")
        canvas?.drawLine(0f, 0f, 1000f, 1000f, paint)
        canvas ?: return
        for (body in engine) {
            body.draw(canvas)
        }
    }

    override fun getChildStaticTransformation(child: View?, t: Transformation?): Boolean {
        return true
    }


    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val b = Bitmap.createBitmap(child?.width ?: 0, child?.height ?: 0, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val paint = Paint()
        val m = Matrix()
        val rectangle = andoridViewToEngineRectangle[child] ?: return true
        m.postRotate(Utils.radToDeg(rectangle.rotation).toFloat())
        m.postTranslate(rectangle.pos.x.toFloat() * scale, rectangle.pos.y.toFloat() * scale)
        child?.draw(c)
        canvas?.drawBitmap(b, m, paint)
        return true
    }

    private fun Body.draw(canvas: Canvas) {
        if (this is Rectangle) {
            val path = Path()
            val axes = transformedAxes
            path.moveTo(
                axes[0].first.x.toFloat() * scale,
                axes[0].first.y.toFloat() * scale
            )
            for (i in axes.indices) {
                path.lineTo(
                    axes[i].second.x.toFloat() * scale,
                    axes[i].second.y.toFloat() * scale
                )
            }
            path.close()
            canvas.drawPath(path, Paint(color))
        }
    }
}
