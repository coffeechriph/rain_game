package roguelike.Entity

import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Transform
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
    lateinit var transform: Transform

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
        transform = system.findTransformComponent(getId())!!
        transform.setPosition(1200.0f,600.0f, 9.0f)
        transform.setScale(96.0f,96.0f)

        val animator = system.findAnimatorComponent(getId())!!
        animator.addAnimation("down", 0, 0, 0, 0.0f)
        animator.addAnimation("right", 1, 1, 0, 0.0f)
        animator.addAnimation("up", 2, 2, 0, 0.0f)
        animator.addAnimation("left", 3, 3, 0, 0.0f)

        // TODO: A problem here was that I had to add a idle animation for the LEFT
        // animation to actually trigger due to how it works in the Sprite
        animator.addAnimation("idle", 0, 0, 0, 0.0f)
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(getId())!!
        val animator = system.findAnimatorComponent(getId())!!

        if (active) {
            transform.z = parentTransform.z + 0.01f

            when(direction) {
                Direction.LEFT -> {
                    transform.x = parentTransform.x - 32
                    transform.y = parentTransform.y
                    animator.setAnimation("left")
                }
                Direction.RIGHT -> {
                    transform.x = parentTransform.x + 32
                    transform.y = parentTransform.y
                    animator.setAnimation("right")
                }
                Direction.UP -> {
                    transform.x = parentTransform.x
                    transform.y = parentTransform.y - 32
                    animator.setAnimation("up")
                }
                Direction.DOWN -> {
                    transform.x = parentTransform.x
                    transform.y = parentTransform.y + 32
                    animator.setAnimation("down")
                }
            }

            sprite.visible = true
            activeTime++
            if (activeTime > 10) {
                active = false
                activeTime = 0
            }
        }
        else {
            sprite.visible = false
        }
    }
}
