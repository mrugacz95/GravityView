package pl.mrugacz95.gravityview

import kotlin.math.min

object Collision {

    fun areAABBColliding(body1: Body, body2: Body): Boolean {
        return body1.AABB.collides(body2.AABB)
    }

    fun areSATColliding(body1: Body, body2: Body): MTV? {
        if (body1 is Rectangle) {
            if (body2 is Rectangle) {
                return intersectPolygons(body1, body2)
            }
        }
        throw Exception("Unknown types colliding")
    }

    private fun intersectPolygons(body1: Rectangle, body2: Rectangle): MTV? {
        var collisionNormal = Vec2.ZERO
        var collisionDepth = Double.MAX_VALUE
        for (body in listOf(body1, body2)) {
            for (axis in body1.transformedAxes) {
                val normal = (axis.second - axis.first).normal().normalize()

                val b1Proj = projectPolygon(normal, body1)
                val b2Proj = projectPolygon(normal, body2)

                if (b1Proj.max <= b2Proj.min ||
                    b1Proj.min >= b2Proj.max
                ) {
                    return null
                }
                val overlap = min(
                    b1Proj.max - b2Proj.min,
                    b2Proj.max - b1Proj.min
                )

                if (overlap < collisionDepth) {
                    collisionDepth = overlap
                    collisionNormal = normal
                }
            }
        }

        if ((body1.pos - body2.pos).dot(collisionNormal) > 0) {
            collisionNormal = -collisionNormal
        }

        return MTV(body1, body2, collisionNormal, collisionDepth)
    }

    private fun projectPolygon(axis: Vec2, body: Rectangle): Projection {
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for (point in body.transformedPoints) {
            val projection = point.dot(axis)
            if (min > projection) {
                min = projection
            }
            if (max < projection) {
                max = projection
            }
        }
        return Projection(min, max)
    }
}

private data class Projection(val min: Double, val max: Double)

data class MTV(val body1: Body, val body2: Body, val normal: Vec2, val depth: Double)
