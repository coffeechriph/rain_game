package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.scene.Scene
import roguelike.Level.TILE_WIDTH

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

class ItemDesc(val type: ItemType, val textureOffset: Vector2i)

val ITEM_COMBINATIONS = arrayOf(
    Pair(ItemDesc(ItemType.MELEE, Vector2i(0, 1)), arrayOf(   "Iron Sword", "Steel Sword", "Wood Sword", "Bronze Sword", "Diamond Sword", "Mithril Sword",
                                    "Iron Dagger", "Steel Dagger", "Wood Dagger", "Bronze Dagger", "Diamond Dagger", "Mithril Dagger",
                                    "Iron Spear", "Steel Spear", "Wood Spear", "Bronze Spear", "Diamond Spear", "Mithril Spear",
                                    "Iron Mace", "Steel Mace", "Wood Mace", "Bronze Mace", "Diamond Mace", "Mithril Mace")),
    Pair(ItemDesc(ItemType.RANGED, Vector2i(0, 4)), arrayOf(  "Bow", "Long Bow", "Crossbow")),
    Pair(ItemDesc(ItemType.HEAD, Vector2i(0, 1)), arrayOf(    "Helmet", "Hood", "Mask", "Cap")),
    Pair(ItemDesc(ItemType.CHEST, Vector2i(0, 1)), arrayOf(   "Chest Armor", "Bronze Breastplate", "Bone Cage", "Iron Breastplate", "Mithril Breastplate")),
    Pair(ItemDesc(ItemType.LEGS, Vector2i(0, 1)), arrayOf(    "Leather Leggings", "Cloth Leggings", "Old Underwear", "Simple Leggings", "Leather Pants", "Cloth Pants")),
    Pair(ItemDesc(ItemType.GLOVES, Vector2i(0, 1)), arrayOf(  "Leather Gloves", "Cloth Gloves", "Iron Gloves", "Bronze Gloves", "Mithril Gloves")),
    Pair(ItemDesc(ItemType.BOOTS, Vector2i(0, 1)), arrayOf(   "Leather Boots", "Cloth Boots", "Sandals", "Iron Boots", "Mithril Boots", "Bronze Boots")),
    Pair(ItemDesc(ItemType.POTION, Vector2i(0, 0)), arrayOf(  "Minor Health Potion", "Lesser Health Potion", "Health Potion", "Great Health Potion"))
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
            getRenderComponents()[0].addCustomUniformData(0, player.level.lightIntensityAt(transform.x, transform.y))

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
