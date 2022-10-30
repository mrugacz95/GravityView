package pl.mrugacz95.gravityview

import android.view.View
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

class Engine(var g: Double = 9.81) : LinkedList<Body>() {
    private val iterations: Int = 20
    private val dt: Double = 1.0 / 60

    private var tickJob: Job? = null

    suspend fun start(view: View) = coroutineScope {
        tickJob?.cancel()
        tickJob = launch(Dispatchers.Default) {
            tickerFlow(dt.seconds)
                .cancellable()
                .collectLatest {
                    update()
                    view.invalidate()
                }
        }
    }

    private fun update() {
        for (it in 0 until iterations) {
            for (body in this) {
                if (!body.isStatic) {
                    body.applyAcceleration(Vec2(.0, this.g), dt / this.iterations)
                }
            }
            step()
            for (body in this) {
                body.update(this.dt / this.iterations)
            }
        }
    }

    fun step() {
        val broadColliding = broadPhrase()
        narrowPhrase(broadColliding)
    }

    private fun broadPhrase(): List<Pair<Int, Int>> {
        val collidingBodies = mutableListOf<Pair<Int, Int>>()
        for (i in this.indices) {
            for (j in i + 1 until this.size) {
                val body1 = this[i]
                val body2 = this[j]
                if (Collision.areAABBColliding(body1, body2)) {
                    collidingBodies.add(i to j)
                }
            }
        }
        return collidingBodies
    }

    private fun narrowPhrase(broadColliding: List<Pair<Int, Int>>) {
        for ((body1idx, body2idx) in broadColliding) {
            val body1 = this[body1idx]
            val body2 = this[body2idx]
            val collisionMTV = Collision.areSATColliding(body1, body2)
            if (collisionMTV != null) {
                this.separateBodies(collisionMTV)
//            const contactPoints = new ContactPoints(body1, body2)
//            const collisionManifold = new CollisionManifold(
//                collisionMTV,
//                contactPoints
//            )
//            this.resolveCollisionWithRotation(collisionManifold)
            }
        }
    }

    private fun separateBodies(mtv: MTV) {
        val body1 = mtv.body1
        val body2 = mtv.body2
        val normal = mtv.normal.normalize()
        if (body2.isStatic) {
            body1.pos += normal * -mtv.depth
        } else if (body1.isStatic) {
            body2.pos += normal * mtv.depth
        } else {
            body2.pos += normal * (mtv.depth / 2.0)
            body1.pos += normal * (-mtv.depth / 2.0)
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

}
