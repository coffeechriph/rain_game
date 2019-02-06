package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class Container(private val containerType: Int, val numItems: Int) : Entity() {
    var open = false
    var looted = false
    var cellX = 0
    var cellY = 0

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        val transform = getTransform()!!
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(64.0f, 64.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = (pos.x%1280).toFloat()
        transform.y = (pos.y%768).toFloat()
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (open) {
            getRenderComponents()[0].textureTileOffset = Vector2i(1,containerType+4)
        }
        else {
            getRenderComponents()[0].textureTileOffset = Vector2i(0,containerType+4)
        }
    }
}
