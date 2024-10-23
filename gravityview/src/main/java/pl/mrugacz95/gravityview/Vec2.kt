package pl.mrugacz95.gravityview

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(val x: Double, val y: Double) {

    companion object {
        val ZERO = Vec2(.0, .0)
    }

    fun rotate(theta: Double): Vec2 {
        val rotatedX = this.x * cos(theta) - this.y * sin(theta)
        val rotatedY = this.x * sin(theta) + this.y * cos(theta)
        return Vec2(rotatedX, rotatedY)
    }

    operator fun minus(s: Double): Vec2 {
        return Vec2(this.x * s, this.y * s)
    }

    fun sqrtMagnitude(): Double {
        return sqrt(magnitude())
    }

    fun magnitude(): Double {
        return this.x * this.x + this.y * this.y
    }

    operator fun minus(other: Vec2): Vec2 {
        return Vec2(this.x - other.x, this.y - other.y)
    }

    fun normalize(): Vec2 {
        val len = this.sqrtMagnitude()
        return Vec2(this.x / len, this.y / len)
    }

    fun dot(other: Vec2): Double {
        return this.x * other.x + this.y * other.y
    }

    fun cross(other: Vec2): Double {
        return this.x * other.y - this.y * other.x
    }

    fun normal(): Vec2 {
        return Vec2(-this.y, this.x)
    }

    operator fun plus(other: Vec2): Vec2 {
        return Vec2(this.x + other.x, this.y + other.y)
    }

    fun distance(other: Vec2): Double {
        return (this - other).sqrtMagnitude()
    }

    fun isCloseTo(other: Vec2): Boolean {
        return (this - other).magnitude() < 0.0005 * 0.0005
    }

    operator fun times(s: Double): Vec2 {
        return Vec2(this.x * s, this.y * s)
    }

    operator fun unaryMinus(): Vec2 {
        return this * (-1.0)
    }

    operator fun div(scale: Double): Vec2 {
        return Vec2(this.x / scale, this.y / scale)
    }
}

operator fun Double.times(normal: Vec2): Vec2 {
    return normal * this
}
