package pl.mrugacz95.gravityview

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

abstract class Body(val isStatic: Boolean) {
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
    var mass: Double = 1.0
        set(value) {
            if (value < 0) {
                throw Exception("Mass can't be less then zero.")
            }
            if (isStatic) {
                invInertia = 0.0
            } else {
                invMass = 1 / value
            }
            field = value
        }
    var invMass: Double = 0.0
    var invInertia: Double
    var inertia: Double = 1.0
        set(value) {
            if (isStatic) {
                invInertia = 0.0
            } else {
                invInertia = 1 / value
            }
            field = value
        }
    val color: Int
    val restitution = 0.2
    protected var transformUpdateRequired = true
    protected var cachedAABB: AABB? = null
    val AABB: AABB
        get() {
            cachedAABB.let { cache ->
                if (cache == null) {
                    calculateAABB().let {
                        cachedAABB = it
                        return it
                    }
                }
                return cache
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
        this.cachedAABB = null
        this.transformUpdateRequired = true
    }

    abstract fun calculateAABB(): AABB

    fun applyAcceleration(a: Vec2, dt: Double) {
        this.velocity += a * dt
    }

    abstract fun isInside(point: Vec2): Boolean
}


class Rectangle(isStatic: Boolean) : Body(isStatic) {
    var width: Double = 1.0
        set(value) {
            field = value
            updateProperties()
        }
    var height: Double = 1.0
        set(value) {
            field = value
            updateProperties()
        }

    private fun updateProperties() {
        this.inertia = 1.0 / 12.0 * mass * (width * width + height * height)
        transformUpdateRequired = true
        cachedAABB = null
    }

    private fun points(): Array<Vec2> = arrayOf(
        Vec2(-width / 2, height / 2),
        Vec2(width / 2, height / 2),
        Vec2(width / 2, -height / 2),
        Vec2(-width / 2, -height / 2)
    )

    private val cachedTransformedPoints = Array(4) { Vec2.ZERO }

    val transformedPoints: Array<Vec2>
        get() {
            if (transformUpdateRequired) {
                val points = points()
                for (i in points.indices) {
                    this.cachedTransformedPoints[i] = points[i].rotate(rotation) + this.pos
                }
                transformUpdateRequired = false
            }
            return cachedTransformedPoints
        }

    val transformedAxes: Array<Axis>
        get() = Array(transformedPoints.size) {
            val first = this.transformedPoints[it]
            val second = this.transformedPoints[(it + 1) % this.transformedPoints.size]
            Axis(first, second, second - first)
        }

    override fun calculateAABB(): AABB {
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (p in this.transformedPoints) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        return AABB(minX, minY, maxX, maxY)
    }

    override fun isInside(point: Vec2): Boolean {
        for (p in this.transformedAxes) {
            val d = p.axis.cross(p.p2 - point)
            if (d < 0) return false
        }
        return true
    }
}

data class Axis(val p1: Vec2, val p2: Vec2, val axis: Vec2)
