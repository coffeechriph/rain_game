package roguelike.Entity

import org.joml.Random
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class StoneOgre(random: Random, player: Player): Enemy(random, player) {
    init {
        strengthFactor = 2.0f
        healthFactor = 1.5f
        agilityFactor = 0.1f
        walkingSpeedFactor = 0.55f
        attackSpeed = 0.007f
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        super.init(scene, system)

        // TODO: Should be able to animate on the Y axis as well
        animator.addAnimation("idle_up", 0, 0, 1, 1.0f)
        animator.addAnimation("idle_down", 0, 0, 0, 1.0f)
        animator.addAnimation("idle_left", 0, 0, 1, 1.0f)
        animator.addAnimation("idle_right", 0, 0, 0, 1.0f)
        animator.addAnimation("walk_down", 0, 4, 0, 3.0f)
        animator.addAnimation("walk_up", 0, 4, 1, 3.0f)
        animator.setAnimation("idle_up")

        attackTimeoutValue = 100
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        super.update(scene, input, system, deltaTime)
        val transform = transform
        transform.z = 1.0f + transform.y * 0.001f

        handleDamage(transform)
    }
}
