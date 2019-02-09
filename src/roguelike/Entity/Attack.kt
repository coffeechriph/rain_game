package roguelike.Entity

import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

/*
    TODO: Two things that would be nice to support in the engine to make things like these a bit easier.
    1. onCollision method which takes in two entities that collided.
    2. parent entity to allow this entities transform the be linked to the parent
 */
class Attack(private val parentTransform: Transform) : Entity() {
    private var active = false
    private var direction = Direction.DOWN
    private var activeTime = 0
    lateinit var attacker: Entity

    fun attack(direction: Direction) {
        active = true
        this.direction = direction
    }

    fun isReady(): Boolean {
        return !active;
    }

    fun isActive(): Boolean {
        return active
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = getTransform()
        transform.setPosition(1200.0f,600.0f, 9.0f)
        transform.setScale(72.0f,72.0f)
        getRenderComponents()[0].addCustomUniformData(0, 1.0f)
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val animator = getAnimatorComponent()[0]

        if (active) {
            if (!getRenderComponents()[0].visible) {
                val transform = getTransform()
                transform.z = parentTransform.z + 0.01f

                when (direction) {
                    Direction.LEFT -> {
                        transform.x = parentTransform.x - 32
                        transform.y = parentTransform.y
                        transform.rot = Math.PI.toFloat() * 0.75f
                        animator.setAnimation("attack", true)
                    }
                    Direction.RIGHT -> {
                        transform.x = parentTransform.x + 32
                        transform.y = parentTransform.y
                        transform.rot = Math.PI.toFloat() * 1.75f
                        animator.setAnimation("attack", true)
                    }
                    Direction.UP -> {
                        transform.x = parentTransform.x
                        transform.y = parentTransform.y - 32
                        transform.rot = Math.PI.toFloat()
                        animator.setAnimation("attack", true)
                    }
                    Direction.DOWN -> {
                        transform.x = parentTransform.x
                        transform.y = parentTransform.y + 32
                        transform.rot = 0.0f
                        animator.setAnimation("attack", true)
                    }
                }
            }

            getRenderComponents()[0].visible = true
            if (animator.animationComplete) {
                active = false
                animator.setAnimation("idle")
                getRenderComponents()[0].visible = false
            }
        }
    }
}
