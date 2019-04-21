package roguelike.Entity

import org.joml.Random
import rain.api.Input
import rain.api.scene.Scene

class StoneOgre(random: Random): Enemy(random) {
    init {
        strengthFactor = 2.0f
        healthFactor = 1.5f
        agilityFactor = 0.1f
        walkingSpeedFactor = 0.55f
        attackSpeed = 0.007f
    }

    override fun init(scene: Scene) {
        super.init(scene)

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

    override fun update(scene: Scene, input: Input) {
        super.update(scene, input)
        val transform = transform
        transform.z = 1.0f + transform.y * 0.001f

        handleDamage(transform)
    }
}
