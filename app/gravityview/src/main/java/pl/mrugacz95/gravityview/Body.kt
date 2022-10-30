package pl.mrugacz95.gravityview

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

abstract class Body(mass: Double, inertia: Double, val isStatic: Boolean) {
    var pos = Vec2.ZERO
        set(value) {
            field = value
            transformUpdateRequired = true
        }
    var rotation: Double = .0
        set(value) {
            field = value
            transformUpdateRequired = true
        }
    var omega = .0
    var velocity = Vec2.ZERO
    private val invMass: Double
    private val invInertia: Double
    val color: Int
    private val restitution = 0.2
    protected var transformUpdateRequired = true
    private var cachedAABB: AABB? = null
    val AABB: AABB
        get() {
            cachedAABB.let { cache ->
                if (cache == null) {
                    calculateAABB().let {
                        cachedAABB = it
                        return it
                    }
                } else {
                    return cache
                }
            }

        }

    init {
        if (isStatic) {
            invMass = .0
            invInertia = .0
        } else {
            invMass = 1 / mass
            invInertia = 1 / inertia
        }
        this.color = Color.BLACK
    }


    fun update(dt: Double) {
        if (this.isStatic) {
            return
        }
        this.pos += this.velocity * dt
        this.rotation += this.omega * dt
        this.transformUpdateRequired = true
        this.cachedAABB = null
    }

    abstract fun calculateAABB(): AABB

    fun applyAcceleration(a: Vec2, dt: Double) {
        this.velocity += a * dt
    }
}


class Rectangle(val width: Double, val height: Double, mass: Double, isStatic: Boolean) :
    Body(
        mass,
        1 / 12 * mass * (width * width + height * height),
        isStatic
    ) {
    private val points: Array<Vec2> = arrayOf(
        Vec2(-width / 2, height / 2),
        Vec2(width / 2, height / 2),
        Vec2(width / 2, -height / 2),
        Vec2(-width / 2, -height / 2)
    )
    val cachedTransformedPoints = Array<Vec2>(4) { Vec2.ZERO }

    val transformedPoints: Array<Vec2>
        get() {
            if (transformUpdateRequired) {
                for (i in points.indices) {
                    this.cachedTransformedPoints[i] = this.points[i].rotate(rotation) + this.pos
                }
            }
            transformUpdateRequired = false
            return cachedTransformedPoints
        }

    val transformedAxes: Array<Pair<Vec2, Vec2>>
        get() = Array(transformedPoints.size) {
            val first = this.transformedPoints[it]
            val second = this.transformedPoints[(it + 1) % this.transformedPoints.size]
            first to second
        }

    override fun calculateAABB(): AABB {
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        for (p in this.transformedPoints) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        return AABB(minX, minY, maxX, maxY)
    }

}
