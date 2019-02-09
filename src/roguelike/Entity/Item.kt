package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

enum class ItemType {
    NONE,
    MELEE,
    RANGED,
    HEAD,
    SHOULDER,
    LEGS,
    CHEST,
    GLOVES,
    BOOTS,
    POTION
}

val ITEM_COMBINATIONS = arrayOf(
    Pair(ItemType.MELEE, arrayOf(   "Iron Sword", "Steel Sword", "Wood Sword", "Bronze Sword", "Diamond Sword", "Mithril Sword",
                                    "Iron Dagger", "Steel Dagger", "Wood Dagger", "Bronze Dagger", "Diamond Dagger", "Mithril Dagger",
                                    "Iron Spear", "Steel Spear", "Wood Spear", "Bronze Spear", "Diamond Spear", "Mithril Spear",
                                    "Iron Mace", "Steel Mace", "Wood Mace", "Bronze Mace", "Diamond Mace", "Mithril Mace")),
    Pair(ItemType.RANGED, arrayOf(  "Bow", "Long Bow", "Crossbow")),
    Pair(ItemType.HEAD, arrayOf(    "Helmet", "Hood", "Mask", "Cap")),
    Pair(ItemType.CHEST, arrayOf(   "Chest Armor", "Bronze Breastplate", "Bone Cage", "Iron Breastplate", "Mithril Breastplate")),
    Pair(ItemType.LEGS, arrayOf(    "Leather Leggings", "Cloth Leggings", "Old Underwear", "Simple Leggings", "Leather Pants", "Cloth Pants")),
    Pair(ItemType.GLOVES, arrayOf(  "Leather Gloves", "Cloth Gloves", "Iron Gloves", "Bronze Gloves", "Mithril Gloves")),
    Pair(ItemType.BOOTS, arrayOf(   "Leather Boots", "Cloth Boots", "Sandals", "Iron Boots", "Mithril Boots", "Bronze Boots")),
    Pair(ItemType.POTION, arrayOf(  "Minor Health Potion", "Lesser Health Potion", "Health Potion", "Great Health Potion"))
)

val ITEM_QUALITIES = arrayOf(
    Pair(0..24, "[Poor]"),
    Pair(25..60, "[Common]"),
    Pair(61..81, "[Good]"),
    Pair(82..90, "[Great]"),
    Pair(91..98, "[Rare]"),
    Pair(98..100, "[Epic]")
)

class Item (val player: Player,
            val type: ItemType,
            val name: String,
            val stamina: Int,
            val strength: Int,
            val agility: Int,
            val luck: Int,
            val healthIncrease: Int,
            val instantHealth: Boolean,
            val increaseTick: Float): Entity() {
    var cellX = 0
    var cellY = 0
    var pickedUp = false

    private var time = 0.0f
    private var beginPickup = false
    private var acc = 0.0000000000001

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        val transform = getTransform()
        transform.x = pos.x.toFloat()
        transform.y = pos.y.toFloat()
        transform.z = 1.1f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (getRenderComponents()[0].visible) {
            time += 1.0f / 60.0f
            val transform = getTransform()
            transform.y += Math.sin(time.toDouble()).toFloat() * 0.1f
            getRenderComponents()[0].addCustomUniformData(0, player.level.lightIntensityAt(transform.x, transform.y))

            if (!beginPickup && !pickedUp) {
                if (player.getTransform().x >= transform.x - 128 && player.getTransform().x <= transform.x + 128 &&
                    player.getTransform().y >= transform.y - 128 && player.getTransform().y <= transform.y + 128) {
                    beginPickup = true
                    acc = 0.0000000000001
                }
            }
            else {
                val dx = (player.getTransform().x - transform.x) / 64.0
                val dy = (player.getTransform().y - transform.y) / 64.0
                val ln = Math.sqrt((dx*dx+dy*dy))
                transform.x += ((dx / ln) * acc).toFloat()
                transform.y += ((dy / ln) * acc).toFloat()
                if (acc < 3.7f) {
                    acc += acc
                }

                if (player.getTransform().x >= transform.x - 8 && player.getTransform().x <= transform.x + 8 &&
                    player.getTransform().y >= transform.y - 8 && player.getTransform().y <= transform.y + 8) {
                    pickedUp = true
                    beginPickup = false
                    getRenderComponents()[0].visible = false
                    player.inventory.addItem(this)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        if (type != other.type) return false
        if (name != other.name) return false
        if (stamina != other.stamina) return false
        if (strength != other.strength) return false
        if (agility != other.agility) return false
        if (luck != other.luck) return false
        if (cellX != other.cellX) return false
        if (cellY != other.cellY) return false
        if (pickedUp != other.pickedUp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + stamina
        result = 31 * result + strength
        result = 31 * result + agility
        result = 31 * result + luck
        result = 31 * result + cellX
        result = 31 * result + cellY
        result = 31 * result + pickedUp.hashCode()
        return result
    }
}
