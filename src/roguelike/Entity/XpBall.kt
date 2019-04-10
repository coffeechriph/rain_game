package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.scene.Scene
import roguelike.Level.TILE_WIDTH

class XpBall(private val player: Player): Entity() {
    var cellX = 0
    var cellY = 0
    var pickedUp = false

    private var time = 0.0f
    private var beginPickup = false
    private var acc = 0.0000000000001

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        val transform = transform
        transform.x = pos.x.toFloat()
        transform.y = pos.y.toFloat()
        transform.z = 1.1f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
    }

    override fun init(scene: Scene) {
    }

    override fun update(scene: Scene, input: Input) {
        if (getRenderComponents()[0].visible) {
            time += 1.0f / 60.0f
            val transform = transform
            transform.y += Math.sin(time.toDouble()).toFloat() * 0.1f

            if (!beginPickup && !pickedUp) {
                if (player.transform.x >= transform.x - 128 && player.transform.x <= transform.x + 128 &&
                        player.transform.y >= transform.y - 128 && player.transform.y <= transform.y + 128) {
                    beginPickup = true
                    acc = 0.0000000000001
                }
            }
            else {
                val dx = (player.transform.x - transform.x) / TILE_WIDTH
                val dy = (player.transform.y - transform.y) / TILE_WIDTH
                val ln = Math.sqrt(((dx*dx+dy*dy).toDouble()))
                transform.x += ((dx / ln) * acc).toFloat()
                transform.y += ((dy / ln) * acc).toFloat()
                if (acc < 3.7f) {
                    acc += acc
                }

                if (player.transform.x >= transform.x - 8 && player.transform.x <= transform.x + 8 &&
                        player.transform.y >= transform.y - 8 && player.transform.y <= transform.y + 8) {
                    pickedUp = true
                    beginPickup = false
                    getRenderComponents()[0].visible = false
                    player.addXp(5 * player.currentLevel)
                }
            }
        }
    }
}
