package roguelike.Level

import roguelike.Entity.*
import org.joml.*
import rain.api.entity.DirectionType
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.ResourceFactory

enum class RoomType {
    DIRT_CAVE,
    ROCK_CAVE,
    SNOW_CAVE,
}

class Room(val tiles: MutableList<Vector2i>, val area: Vector4i, val type: RoomType) {
    var neighbourRooms = ArrayList<Room>()

    private val viableTiles = ArrayList<Vector2i>()
    private var allSlotsTaken = false
    private var firstCheck = true

    val enemies = ArrayList<Enemy>()
    val containers = ArrayList<Container>()
    val torches = ArrayList<LightSource>()
    val campfire = ArrayList<LightSource>()

    fun findNoneEdgeTile(rand: Random): Vector2i? {
        if (viableTiles.size == 0 && !allSlotsTaken) {
            firstCheck = false
            for (tile in tiles) {
                var neighbours = 0
                for (tile2 in tiles) {
                    if (tile == tile2) {
                        continue
                    }

                    if (tile2.x == tile.x && (tile2.y == tile.y - 1 || tile2.y == tile.y + 1)) {
                        neighbours += 1
                    } else if (tile2.y == tile.y && (tile2.x == tile.x - 1 || tile2.x == tile.x + 1)) {
                        neighbours += 1
                    } else if (tile2.x == tile.x - 1 && tile2.y == tile.y - 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x + 1 && tile2.y == tile.y - 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x - 1 && tile2.y == tile.y + 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x + 1 && tile2.y == tile.y + 1) {
                        neighbours += 1
                    }

                    if (neighbours >= 8) {
                        viableTiles.add(tile)
                        break
                    }
                }
            }
        }

        if (viableTiles.isEmpty()) {
            return null
        }

        return viableTiles.removeAt(rand.nextInt(viableTiles.size))
    }

    internal fun generateEnemiesInRoom(random: Random, enemySystem: EntitySystem<Enemy>, enemyAttackSystem: EntitySystem<Entity>, player: Player, count: Int, healthBarSystem: EntitySystem<HealthBar>) {
        for (i in 0 until count) {
            val p = findNoneEdgeTile(random)
            if (p == null) {
                return
            }

            val enemy = random.nextInt(2)

            val kracGuy = if (enemy == 0) {
                Krac(random, player)
            } else {
                MiniKrac(random, player)
            }

            enemySystem.newEntity(kracGuy)
                    .attachTransformComponent()
                    .attachSpriteComponent()
                    .attachAnimatorComponent()
                    .build()

            enemyAttackSystem.newEntity(kracGuy.attackAreaVisual)
                    .attachTransformComponent()
                    .attachSpriteComponent()

            val attackSprite = enemyAttackSystem.findSpriteComponent(kracGuy.getId())!!
            attackSprite.visible = false
            attackSprite.textureTileOffset.set(5,7)

            kracGuy.attackAreaVisualSprite = attackSprite
            kracGuy.attackAreaVisualTransform = enemyAttackSystem.findTransformComponent(kracGuy.getId())!!

            val levelFactor = (player.currentLevel*1.5f).toInt()
            kracGuy.strength = (random.nextInt(levelFactor) + levelFactor*10 * kracGuy.strengthFactor).toInt()
            kracGuy.agility = (random.nextInt(levelFactor) + levelFactor*4 * kracGuy.agilityFactor).toInt()
            kracGuy.health = (100 + random.nextInt(levelFactor) * kracGuy.healthFactor).toInt()
            kracGuy.sprite.visible = false

            val et = enemySystem.findTransformComponent(kracGuy.getId())!!
            kracGuy.healthBar.parentTransform = et

            healthBarSystem.newEntity(kracGuy.healthBar)
                    .attachTransformComponent()
                    .attachSpriteComponent()
                    .build()

            kracGuy.healthBar.sprite.visible = false
            kracGuy.healthBar.transform.sx = 60.0f
            kracGuy.healthBar.transform.sy = 7.0f

            kracGuy.setPosition(Vector2i(p.x*64, p.y*64))
            enemies.add(kracGuy)
        }
    }

    internal fun generateContainersInRoom(random: Random, count: Int, containerSystem: EntitySystem<Container>, resourceFactory: ResourceFactory) {
        for (i in 0 until count) {
            val tile = findNoneEdgeTile(random)
            if (tile == null) {
                return
            }

            val container = Container(random.nextInt(2), random.nextInt(7) + 1)
            containerSystem.newEntity(container)
                    .attachTransformComponent()
                    .attachSpriteComponent()
                    .attachBurstParticleEmitter(resourceFactory, 25, 16.0f, 0.2f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 32.0f, 0.5f)
                    .build()

            val emitter = containerSystem.findBurstEmitterComponent (container.getId())!!
            emitter.burstFinished = true
            emitter.singleBurst = true
            emitter.particlesPerBurst = 5
            emitter.startColor = Vector4f(0.4f, 0.4f, 0.4f, 1.0f)
            emitter.endColor = Vector4f(0.4f, 0.4f, 0.4f, 0.0f)
            emitter.transform.z = 16.0f
            emitter.enabled = false

            container.setPosition(Vector2i(tile.x*64 + 32, tile.y*64 + 32))
            container.sprite.visible = false
            containers.add(container)
        }
    }

    internal fun generateLightsInRoom(random: Random, map: IntArray, mapWidth: Int, width: Int, height: Int, torches: Int, campfire: Boolean, torchSystem: EntitySystem<LightSource>, resourceFactory: ResourceFactory) {
        var numTorches = 0
        for (tile in tiles) {
            val x = tile.x
            val y = tile.y
            if (y > 0 && map[x + (y-1)*mapWidth] == 1) {
                val tx = x % width
                val ty = y % height
                val et = LightSource(x / width, y / height, Vector3f(0.9f, 0.55f, 0.1f))
                torchSystem.newEntity(et)
                        .attachTransformComponent()
                        .attachSpriteComponent()
                        .attachParticleEmitter(resourceFactory, 10000, 32.0f, 5.0f, Vector2f(0.0f, -10.0f), DirectionType.LINEAR, 4.0f, 0.5f)
                        .build()
                val etTransform = torchSystem.findTransformComponent(et.getId())
                etTransform!!.setPosition((tx*64 + 32).toFloat(), (ty*64 - 32).toFloat() + 256, 18.0f)
                etTransform.sx = 48.0f
                etTransform.sy = 48.0f

                val sprite = torchSystem.findSpriteComponent(et.getId())!!
                sprite.visible = false

                val emitter = torchSystem.findEmitterComponent(et.getId())!!
                emitter.startSize = 5.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(1.0f, 0.3f, 0.0f, 0.5f)
                emitter.enabled = false
                this.torches.add(et)
                numTorches++
            }

            if (numTorches >= torches) {
                break
            }
        }

        if (campfire) {
            val tile = findNoneEdgeTile(random)
            if (tile != null) {
                val tx = tile.x % width
                val ty = tile.y % height
                val et = LightSource(tile.x / width, tile.y / height, Vector3f(0.9f, 0.55f, 0.1f))
                torchSystem.newEntity(et)
                        .attachTransformComponent()
                        .attachParticleEmitter(resourceFactory, 20, 40.0f, 0.7f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 20.0f, 0.5f)
                        .build()
                val etTransform = torchSystem.findTransformComponent(et.getId())
                etTransform!!.setPosition(((tx*64) + 32).toFloat(), ((ty*64) - 32).toFloat(), 18.0f)
                etTransform.sx = 64.0f
                etTransform.sy = 64.0f

                val emitter = torchSystem.findEmitterComponent(et.getId())!!
                emitter.startSize = 20.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(0.8f, 0.2f, 0.0f, 0.0f)
                emitter.enabled = false
                this.campfire.add(et)
            }
        }
    }
}
