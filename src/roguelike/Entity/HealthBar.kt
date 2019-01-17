package roguelike.Entity

import rain.api.*
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Sprite
import rain.api.entity.Transform
import rain.api.scene.Scene

class HealthBar: Entity() {
    var parentTransform: Transform? = null
    lateinit var transform: Transform
    lateinit var sprite: Sprite

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        transform = system.findTransformComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        transform.z = 11.0f

        if (parentTransform != null) {
            transform.x = parentTransform!!.x
            transform.y = parentTransform!!.y - 48
        }
    }
}
