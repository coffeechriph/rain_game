package roguelike.Level

import org.joml.*
import rain.api.components.Animator
import rain.api.entity.DirectionType
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.scene.Scene
import roguelike.Entity.*

enum class EnemyType {
    STONE_OGRE,
    STONE_GOBLIN,
    STONE_RAT
}

class RoomType {
    val hasLights: Boolean
    val hasEnemies: Boolean
    val enemyTypes: Array<EnemyType>

    constructor(hasLights: Boolean, hasEnemies: Boolean, enemyTypes: Array<EnemyType>) {
        this.hasLights = hasLights
        this.hasEnemies = hasEnemies
        this.enemyTypes = enemyTypes
    }
}

val DIRT_ROOM = RoomType(false, true, arrayOf(EnemyType.STONE_RAT))
val COLD_DIRT_ROOM = RoomType(false, true, arrayOf(EnemyType.STONE_GOBLIN))
val KRAC_BASE = RoomType(true, true, arrayOf(EnemyType.STONE_OGRE, EnemyType.STONE_GOBLIN))
val RoomTypes = arrayOf(DIRT_ROOM, COLD_DIRT_ROOM, KRAC_BASE)

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

    internal fun generateEnemiesInRoom(
        random: Random,
        enemySystem: EntitySystem<Enemy>,
        enemyMaterial: Material,
        quadMesh: Mesh,
        enemyAttackSystem: EntitySystem<Entity>,
        player: Player,
        count: Int,
        healthBarSystem: EntitySystem<HealthBar>,
        healthMaterial: Material,
        enemyAttackMaterial: Material,
        enemyTypes: Array<EnemyType>
    ) {
        for (i in 0 until count) {
            val p = findNoneEdgeTile(random)
            if (p == null) {
                return
            }

            val enemy = random.nextInt(enemyTypes.size)
            val type = enemyTypes[enemy]
            val enemyEntity = when (type) {
                EnemyType.STONE_GOBLIN -> StoneGoblin(random, player)
                EnemyType.STONE_OGRE -> StoneOgre(random,player)
                EnemyType.STONE_RAT -> StoneRat(random,player)
            }

            val enemyAnimator = Animator()
            enemySystem.newEntity(enemyEntity)
                    .attachRenderComponent(enemyMaterial, quadMesh)
                    .attachAnimatorComponent(enemyAnimator)
                    .build()

            enemyAttackSystem.newEntity(enemyEntity.attackAreaVisual)
                    .attachRenderComponent(enemyAttackMaterial, quadMesh)
                    .build()

            val attackRenderComponent = enemyEntity.attackAreaVisual.getRenderComponents()[0]
            attackRenderComponent.visible = false
            attackRenderComponent.textureTileOffset.set(5,15)

            enemyEntity.attackAreaVisualRenderComponent = attackRenderComponent
            enemyEntity.attackAreaVisualTransform = enemyEntity.attackAreaVisual.transform

            val levelFactor = (player.currentLevel*1.5f).toInt()
            enemyEntity.strength = (random.nextInt(levelFactor) + levelFactor*10 * enemyEntity.strengthFactor).toInt()
            enemyEntity.agility = (random.nextInt(levelFactor) + levelFactor*4 * enemyEntity.agilityFactor).toInt()
            enemyEntity.health = (100 + random.nextInt(levelFactor) * enemyEntity.healthFactor).toInt()
            enemyEntity.getRenderComponents()[0].visible = false

            val et = enemyEntity.transform
            enemyEntity.healthBar.parentTransform = et

            healthBarSystem.newEntity(enemyEntity.healthBar)
                    .attachRenderComponent(healthMaterial, quadMesh)
                    .build()

            enemyEntity.healthBar.getRenderComponents()[0].visible = false
            enemyEntity.healthBar.transform.sx = 60.0f
            enemyEntity.healthBar.transform.sy = 7.0f

            enemyEntity.setPosition(Vector2i(p.x*64, p.y*64))
            enemies.add(enemyEntity)
        }
    }

    internal fun generateContainersInRoom(random: Random, count: Int, containerSystem: EntitySystem<Container>, containerMaterial: Material, quadMesh: Mesh) {
        for (i in 0 until count) {
            val tile = findNoneEdgeTile(random)
            if (tile == null) {
                return
            }

            val container = Container(random.nextInt(2), random.nextInt(7) + 1)
            containerSystem.newEntity(container)
                    .attachRenderComponent(containerMaterial, quadMesh)
                    .build()

            container.setPosition(Vector2i(tile.x*64 + 32, tile.y*64 + 32))
            container.getRenderComponents()[0].visible = false
            containers.add(container)
        }
    }

    internal fun generateLightsInRoom(scene: Scene, random: Random, map: IntArray, mapWidth: Int, width: Int, height: Int, torches: Int, campfire: Boolean, torchSystem: EntitySystem<LightSource>, torchMaterial: Material, quadMesh: Mesh) {
        var numTorches = 0
        for (tile in tiles) {
            val x = tile.x
            val y = tile.y
            if (y > 0 && map[x + (y-1)*mapWidth] == 1) {
                val tx = x % width
                val ty = y % height

                val emitter = scene.createParticleEmitter(2.0f, 8, 20.0f)
                // emitter.startSize = 5.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(1.0f, 0.3f, 0.0f, 0.1f)
                emitter.enabled = false
                emitter.transform.sx = 1.0f
                emitter.transform.sy = 1.0f
                emitter.velocity.y = -30.0f

                val et = LightSource(x / width, y / height, Vector3f(0.9f, 0.55f, 0.1f), emitter)
                torchSystem.newEntity(et)
                        .attachRenderComponent(torchMaterial, quadMesh)
                        .build()
                et.transform.setPosition((tx*64 + 32).toFloat(), (ty*64 - 32).toFloat(), 18.0f)
                et.transform.sx = 24.0f
                et.transform.sy = 24.0f
                emitter.transform.parentTransform = et.transform
                et.getRenderComponents()[0].visible = false

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

                val emitter = scene.createParticleEmitter(0.7f, 20, 20.0f)
                // emitter.startSize = 20.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(0.8f, 0.2f, 0.0f, 0.0f)
                emitter.enabled = false

                val et = LightSource(tile.x / width, tile.y / height, Vector3f(0.9f, 0.55f, 0.1f), emitter)
                torchSystem.newEntity(et)
                        .build()
                et.transform.setPosition(((tx*64) + 32).toFloat(), ((ty*64) - 32).toFloat(), 18.0f)
                et.transform.sx = 64.0f
                et.transform.sy = 64.0f
                emitter.transform.parentTransform = et.transform
                this.campfire.add(et)
            }
        }
    }
}
