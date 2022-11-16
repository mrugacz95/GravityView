package pl.mrugacz95.gravityview

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun tickerFlow(period: Duration) = flow {
    while (true) {
        emit(Unit)
        delay(period)
    }
}

class Engine(var g: Double = 9.81) {
    private val iterations: Int = 20
    private val dt: Double = 1.0 / 60
    private val coroutineScope = MainScope() + Job()
    private var tickJob: Job? = null
    val bodies = mutableListOf<Body>()
    private val broadCollidingBodies = mutableListOf<Pair<Int, Int>>()

    fun addBody(body: Body) {
        bodies.add(body)
    }

    fun addAllBodies(bodies: Iterable<Body>) {
        this.bodies.addAll(bodies)
    }

    fun start(view: View) {
        tickJob?.cancel()
        tickJob = coroutineScope.launch(Dispatchers.IO) {
            tickerFlow(dt.seconds)
                .cancellable()
                .collectLatest {
                    update()
                    view.postInvalidate()
                }
        }
    }

    private fun update() {
        for (it in 0 until iterations) {
            for (body in bodies) {
                if (!body.isStatic) {
                    body.applyAcceleration(Vec2(.0, this.g), dt / this.iterations)
                }
            }
            step()
            for (body in bodies) {
                body.update(dt / this.iterations)
            }
        }
    }

    private fun step() {
        val broadColliding = broadPhrase()
        narrowPhrase(broadColliding)
    }

    private fun broadPhrase(): List<Pair<Int, Int>> {
        broadCollidingBodies.clear()
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val body1 = bodies[i]
                val body2 = bodies[j]
                if (body1.isStatic && body2.isStatic) {
                    continue
                }
                if (Collision.areAABBColliding(body1, body2)) {
                    broadCollidingBodies.add(i to j)
                }
            }
        }
        return broadCollidingBodies
    }

    private fun narrowPhrase(broadColliding: List<Pair<Int, Int>>) {
        for ((body1idx, body2idx) in broadColliding) {
            val body1 = bodies[body1idx]
            val body2 = bodies[body2idx]
            val collisionMTV = Collision.areSATColliding(body1, body2)
            if (collisionMTV != null) {
                this.separateBodies(collisionMTV)
                val contactPoints = ContactPoints.getContactPoints(body1, body2)
                val collisionManifold = CollisionManifold(
                    collisionMTV,
                    contactPoints
                )
                this.resolveCollisionWithRotation(collisionManifold)
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

    private fun resolveCollisionWithRotation(collisionManifold: CollisionManifold) {
        val body1: Body = collisionManifold.body1
        val body2: Body = collisionManifold.body2
        val normal = collisionManifold.normal.normalize()

        val impulses = mutableListOf<Triple<Vec2, Vec2, Vec2>>()

        val e = min(body1.restitution, body2.restitution)


        for (contactPoint in collisionManifold.contactPoints) {
            val r1 = contactPoint - body1.pos
            val r2 = contactPoint - body2.pos

            val r1Perp = r1.normal()
            val r2Perp = r2.normal()

            val angularLinearVelocityBody1 = r1Perp * body1.omega
            val angularLinearVelocityBody2 = r2Perp * body2.omega

            val relativeVelocity = (body2.velocity + angularLinearVelocityBody2) -
                (body1.velocity + angularLinearVelocityBody1)


            val contactVelocityMag = relativeVelocity.dot(normal)

            if (contactVelocityMag > 0) {
                continue
            }

            val r1PerpDotN = r1Perp.dot(normal)
            val r2PerpDotN = r2Perp.dot(normal)

            var j: Double = -(1 + e) * contactVelocityMag
            j /= body1.invMass + body2.invMass +
                (r1PerpDotN * r1PerpDotN) * body1.invInertia +
                (r2PerpDotN * r2PerpDotN) * body2.invInertia
            j /= collisionManifold.contactPoints.size

            val impulse = normal * j
            impulses.add(
                Triple(
                    impulse,
                    r1,
                    r2
                )
            )
        }

        for ((impulse, r1, r2) in impulses) {
            body1.velocity -= impulse * body1.invMass
            body1.omega -= r1.cross(impulse) * body1.invInertia
            body2.velocity += impulse * body2.invMass
            body2.omega += r2.cross(impulse) * body2.invInertia
        }
    }


    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

}


data class CollisionManifold(
    val body1: Body,
    val body2: Body,
    val normal: Vec2,
    val depth: Double,
    val contactPoints: List<Vec2>
) {

    constructor(
        mtv: MTV,
        contactPoints: List<Vec2>
    ) : this(
        mtv.body1,
        mtv.body2,
        mtv.normal,
        mtv.depth,
        contactPoints
    )

}
