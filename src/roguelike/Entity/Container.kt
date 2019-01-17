package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Sprite
import rain.api.entity.Transform
import rain.api.scene.Scene

class Container(private val containerType: Int, val numItems: Int) : Entity() {
    var open = false
    var looted = false
    var cellX = 0
    var cellY = 0

    lateinit var transform: Transform
    lateinit var sprite: Sprite

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(64.0f, 64.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = (pos.x%1280).toFloat()
        transform.y = (pos.y%768).toFloat()
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        transform = system.findTransformComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (open) {
            sprite.textureTileOffset.x = 1
            sprite.textureTileOffset.y = containerType+4
        }
        else {
            sprite.textureTileOffset.x = 0
            sprite.textureTileOffset.y = containerType+4
        }
    }
}
