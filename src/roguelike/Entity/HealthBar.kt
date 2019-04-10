package roguelike.Entity

import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.scene.Scene

class HealthBar: Entity() {
    var parentTransform: Transform? = null

    override fun init(scene: Scene) {
    }

    override fun update(scene: Scene, input: Input) {
        transform.z = 11.0f

        if (parentTransform != null) {
            transform.x = parentTransform!!.x
            transform.y = parentTransform!!.y - 48
        }
    }
}
