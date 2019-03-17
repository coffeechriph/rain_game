package roguelike.Entity

import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class HealthBar: Entity() {
    var parentTransform: Transform? = null

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>) {
        val transform = transform!!
        transform.z = 11.0f

        if (parentTransform != null) {
            transform.x = parentTransform!!.x
            transform.y = parentTransform!!.y - 48
        }
    }
}
