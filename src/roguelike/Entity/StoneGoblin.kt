package roguelike.Entity

import org.joml.Random
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class StoneGoblin(random: Random, player: Player): Enemy(random, player) {
    init {
        strengthFactor = 0.70f
        healthFactor = 0.65f
        agilityFactor = 2.0f
        walkingSpeedFactor = 1.0f
        attackSpeed = 0.013f
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        super.init(scene, system)

        // TODO: Should be able to animate on the Y axis as well
        animator.addAnimation("idle_up", 4, 4, 1, 1.0f)
        animator.addAnimation("idle_down", 4, 4, 0, 1.0f)
        animator.addAnimation("idle_left", 4, 4, 1, 1.0f)
        animator.addAnimation("idle_right", 4, 4, 0, 1.0f)
        animator.addAnimation("walk_down", 4, 8, 0, 3.5f)
        animator.addAnimation("walk_up", 4, 8, 1, 3.5f)
        animator.setAnimation("idle_up")

        attackTimeoutValue = 30
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>) {
        super.update(scene, input, system)
        val transform = transform
        transform.z = 1.0f + transform.y * 0.001f
        handleDamage(transform)
    }
}
