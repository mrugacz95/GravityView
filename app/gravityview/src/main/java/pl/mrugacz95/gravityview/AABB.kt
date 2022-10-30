package pl.mrugacz95.gravityview

data class AABB(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
    fun collides(other: AABB): Boolean {
        return this.minX < other.maxX &&
            this.maxX > other.minX &&
            this.minY < other.maxY &&
            this.maxY > other.minY
    }
}
