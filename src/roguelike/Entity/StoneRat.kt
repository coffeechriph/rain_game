package roguelike.Entity

import org.joml.Random
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class StoneRat(random: Random, player: Player): Enemy(random, player) {
    init {
        strengthFactor = 0.50f
        healthFactor = 0.55f
        agilityFactor = 4.0f
        walkingSpeedFactor = 2.0f
        attackSpeed = 0.03f
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        super.init(scene, system)

        // TODO: Should be able to animate on the Y axis as well
        animator.addAnimation("idle_up", 0, 0, 2, 1.0f)
        animator.addAnimation("idle_down", 0, 0, 2, 1.0f)
        animator.addAnimation("idle_left", 0, 0, 2, 1.0f)
        animator.addAnimation("idle_right", 0, 0, 2, 1.0f)
        animator.addAnimation("walk_down", 0, 4, 2, 3.5f)
        animator.addAnimation("walk_up", 0, 4, 2, 3.5f)
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
