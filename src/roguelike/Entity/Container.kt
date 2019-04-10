package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.scene.Scene
import roguelike.Level.TILE_WIDTH

class Container(private val containerType: Int, val numItems: Int) : Entity() {
    var open = false
    var looted = false
    var cellX = 0
    var cellY = 0

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        val transform = transform!!
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(TILE_WIDTH, TILE_WIDTH)
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = (pos.x%1280).toFloat()
        transform.y = (pos.y%768).toFloat()
    }

    override fun init(scene: Scene) {
    }

    override fun update(scene: Scene, input: Input) {
        if (open) {
            getRenderComponents()[0].textureTileOffset = Vector2i(1,containerType+12)
        }
        else {
            getRenderComponents()[0].textureTileOffset = Vector2i(0,containerType+12)
        }
    }
}
