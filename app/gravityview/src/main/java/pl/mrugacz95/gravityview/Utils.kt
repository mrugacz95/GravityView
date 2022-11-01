package pl.mrugacz95.gravityview

import kotlin.math.abs

object Utils {
    fun radToDeg(deg: Double) = deg * 180.0 / Math.PI

    fun closestPointOnSegment(point: Vec2, a: Vec2, b: Vec2): Vec2 {
        val ab = b - a
        val ap = point - a
        val proj = ab.dot(ap)
        val abLenSq = ab.magnitude()
        val d = proj / abLenSq

        return if (d <= 0) {
            a
        } else if (d >= 1) {
            b
        } else {
            a + (ab * d)
        }
    }

    fun isCloseTo(a: Double, b: Double): Boolean {
        return abs(b - a) < 0.0005
    }

}
