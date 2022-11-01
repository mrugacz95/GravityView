package pl.mrugacz95.gravityview

object ContactPoints {
    fun getContactPoints(body1: Body, body2: Body): List<Vec2> {
        if (body1 is Rectangle) {
            if (body2 is Rectangle) {
                return contactPointsPolygonToPolygon(body1, body2)
            }
        }
        throw Exception("Unsupported types")
    }

    private fun contactPointsPolygonToPolygon(body1: Rectangle, body2: Rectangle): List<Vec2> {
        var minDist = Double.MAX_VALUE
        var closestPoint1: Vec2 = Vec2.ZERO
        var closestPoint2: Vec2 = Vec2.ZERO
        var contactCount = 0

        for ((polygon1, polygon2) in listOf(body1 to body2, body2 to body1)) {
            for (point in polygon1.transformedPoints) {
                for (axis in polygon2.transformedAxes) {
                    val newClosest = Utils.closestPointOnSegment(point, axis.p1, axis.p2)
                    val distSq = (newClosest - point).magnitude()

                    if (Utils.isCloseTo(distSq, minDist)) {
                        if (!point.isCloseTo(closestPoint1)) {
                            closestPoint2 = point
                            contactCount = 2
                        }
                    } else if (distSq < minDist) {
                        contactCount = 1
                        closestPoint1 = point
                        minDist = distSq
                    }

                }
            }
        }
        return when (contactCount) {
            1 -> listOf(closestPoint1)
            2 -> listOf(closestPoint1, closestPoint2)
            else -> emptyList()
        }
    }
}
