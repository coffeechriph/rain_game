package roguelike.Level

import com.badlogic.gdx.physics.box2d.BodyDef
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.stb.*
import rain.api.Input
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.*
import rain.api.scene.*
import rain.vulkan.VertexAttribute
import roguelike.Entity.*
import java.lang.Math
import java.nio.ByteBuffer
import kotlin.math.sign

class Level(private val player: Player, val resourceFactory: ResourceFactory) {
    lateinit var map: IntArray
        private set
    var mapWidth = 0
        private set
    var mapHeight = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    var maxCellX = 0
        private set
    var maxCellY = 0
        private set
    var backTilemap = Tilemap()
        private set
    var frontTilemap = Tilemap()
        private set
    private lateinit var lightMap: VertexBuffer
    private lateinit var lightVertices: FloatArray
    private lateinit var lightVerticesBuffer: ByteBuffer
    private lateinit var lightValues: Array<Vector4f>
    private lateinit var lightMapMaterial: Material

    private lateinit var tilemapMaterial: Material
    private lateinit var itemMaterial: Material
    private lateinit var texture: Texture2d
    private lateinit var torchTexture: Texture2d
    private lateinit var torchMaterial: Material
    private lateinit var torchSystem: EntitySystem<LightSource>
    private var firstBuild = true

    private var mapBackIndices = Array(0){ TileGfxNone }
    private var mapFrontIndices = Array(0){ TileGfxNone }
    private var rooms = ArrayList<Room>()
    private lateinit var random: Random
    private lateinit var enemySystem: EntitySystem<Enemy>
    private lateinit var enemyAttackSystem: EntitySystem<Entity>
    private lateinit var enemyTexture: Texture2d
    private lateinit var enemyMaterial: Material
    private lateinit var enemyAttackMaterial: Material
    private lateinit var collisionSystem: EntitySystem<Entity>
    private lateinit var containerSystem: EntitySystem<Container>
    private lateinit var levelItemSystem: EntitySystem<Item>
    private lateinit var enemyTargetEntity: Entity
    private lateinit var enemyTargetSystem: EntitySystem<Entity>
    private lateinit var xpBallSystem: EntitySystem<XpBall>
    private lateinit var navMesh: NavMesh
    var startPosition = Vector2i()
    var exitPosition = Vector2i()

    private var delayLightUpdate = 0

    private val activeEnemies = ArrayList<Enemy>()
    private val activeContainers = ArrayList<Container>()
    private val activeLightSources = ArrayList<LightSource>()
    private val collisionBoxes = ArrayList<Vector4i>()
    lateinit var quadMesh: Mesh

    fun lightIntensityAt(x: Float, y: Float): Float {
        var tx = (x / 64.0f).toInt()
        var ty = (y / 64.0f).toInt()
        if (tx >= width) {
            tx = width -1
        }
        if (ty >= height) {
            ty = height -1
        }

        return lightValues[tx + ty * width].w
    }

    fun collides(x: Float, y: Float, w: Float, h: Float): Boolean {
        val w2 = w * 0.5f
        val h2 = h * 0.5f
        for (box in collisionBoxes) {
            if (x + w2 >= box.x && x - w2 <= box.x + box.z &&
                y + h2 >= box.y && y - h2 <= box.y + box.w) {
                return true
            }
        }

        return false
    }

    fun update(deltaTime: Float, input: Input) {
        val lc = lightIntensityAt(player.transform.x, player.transform.y)
        player.getRenderComponents()[0].addCustomUniformData(0, lc)
        player.bootsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.chestArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.legsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.handsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)

        if (delayLightUpdate == 0) {
            spreadLightOnTilemap(player.cellX, player.cellY)
            delayLightUpdate = 2
        }
        else {
            delayLightUpdate -= 1
        }

        // Remove dead enemies
        val enemiesToRemove = ArrayList<Enemy>()
        for (enemy in activeEnemies) {
            if (enemy.health <= 0) {
                enemiesToRemove.add(enemy)
            }
        }

        for (e in enemiesToRemove) {
            enemySystem.removeEntity(e)
            activeEnemies.remove(e)
        }

        // Add navmesh blockers at the location of enemies
        for (enemy in activeEnemies) {
            if (enemy.health <= 0) {
                continue
            }

            if (enemy.lastX > -1 && enemy.lastY > -1) {
                navMesh.map[enemy.lastX + enemy.lastY * width] = 0
            }

            var x = enemy.transform.x.toInt()/64
            var y = enemy.transform.y.toInt()/64

            val elc = lightValues[x + y * width].w
            enemy.getRenderComponents()[0].addCustomUniformData(0, elc)

            if (x >= width) {
                x = width-1
            }
            if (y >= height) {
                y = height - 1
            }
            if (x < 0) {
                x = 0
            }
            if (y < 0) {
                y = 0
            }

            navMesh.map[x + y * width] = 127.toByte()
            enemy.lastX = x
            enemy.lastY = y
        }

        player.damageEnemies(activeEnemies)
        player.targetEnemy(activeEnemies, input)
        if (player.targetedEnemy != null) {
            val enemy = player.targetedEnemy!!
            if (!enemy.getRenderComponents()[0].visible) {
                enemyTargetEntity.getRenderComponents()[0].visible = false
            }
            else {
                var pdx = player.transform.x - enemy.transform.x
                var pdy = player.transform.y - enemy.transform.y
                val pln = Math.sqrt((pdx * pdx + pdy * pdy).toDouble()).toFloat()
                pdx /= pln
                pdy /= pln

                val inCharge = Math.abs(pdx) > Math.abs(pdy)
                if (inCharge) {
                    if (pdx > 0) {
                        player.facingDirection = Direction.LEFT
                    } else {
                        player.facingDirection = Direction.RIGHT
                    }
                } else {
                    if (pdy > 0) {
                        player.facingDirection = Direction.UP
                    } else {
                        player.facingDirection = Direction.DOWN
                    }
                }

                enemyTargetEntity.getRenderComponents()[0].visible = true

                val targetTransform = enemyTargetEntity.transform
                targetTransform.z = 19.0f
                targetTransform.x = enemy.transform.x
                targetTransform.y = enemy.transform.y
                targetTransform.sx = 64.0f
                targetTransform.sy = 64.0f
            }
        }
        else {
            enemyTargetEntity.getRenderComponents()[0].visible = false
        }

        for (enemy in activeEnemies) {
            if (enemy.health <= 0) {
                if (enemy.getRenderComponents()[0].visible) {
                    for (i in 0 until random.nextInt(5)+1) {
                        // Add xp balls to the world
                        val xpBall = XpBall(player)
                        xpBallSystem.newEntity(xpBall)
                                .attachRenderComponent(itemMaterial, quadMesh)
                                .build()

                        var px = enemy.transform.x.toInt() + (Math.sin(random.nextFloat()*Math.PI*2) * 32.0f).toInt()
                        var py = enemy.transform.y.toInt() + (Math.cos(random.nextFloat()*Math.PI*2) * 32.0f).toInt()

                        if (px < 0) {
                            px = 0
                        }
                        if (px > (width-1)*64.0f) {
                            px = ((width - 1) * 64.0f).toInt()
                        }

                        if (py < 0) {
                            py = 0
                        }
                        if (py > (height-1)*64.0f) {
                            py = ((height - 1) * 64.0f).toInt()
                        }

                        xpBall.setPosition(xpBallSystem, Vector2i(px, py))
                        xpBall.transform.sx = random.nextFloat() * 16.0f + 40.0f
                        xpBall.transform.sy = random.nextFloat() * 16.0f + 40.0f
                        xpBall.getRenderComponents()[0].textureTileOffset = Vector2i(5,14)
                        xpBall.getRenderComponents()[0].addCustomUniformData(0, 1.0f)
                    }
                }

                enemy.getRenderComponents()[0].visible = false
                enemy.healthBar.getRenderComponents()[0].visible = false
                continue
            }

            val etx = (enemy.transform.x / 64.0f).toInt()
            val ety = (enemy.transform.y / 64.0f).toInt()
            val ptx = (player.transform.x / 64.0f).toInt()
            val pty = (player.transform.y / 64.0f).toInt()

            if (etx == ptx && Math.abs(pty-ety) <= 2 ||
                ety == pty && Math.abs(ptx-etx) <= 2) {
                enemy.attack()
            }

            enemy.healthBar.transform.sx = enemy.health / 2.0f

            // Enemies will not move when attacking
            if (enemy.attacking) {
                continue
            }

            if (!enemy.traversing) {
                val kx = player.transform.x - enemy.transform.x
                val ky = player.transform.y - enemy.transform.y
                val dd = Math.sqrt((kx*kx+ky*ky).toDouble())
                if (dd > 32.0f) {
                    val worldX = enemy.transform.x / 64
                    val worldY = enemy.transform.y / 64
                    val px = (player.transform.x / 64).toInt()
                    val py = (player.transform.y / 64).toInt()

                    if (px < width && py < height && px >= 0 && py >= 0) {
                        val path = navMesh.findPath(Vector2i(worldX.toInt(), worldY.toInt()), Vector2i(px, py))
                        if (path[path.size-1].x == px && path[path.size-1].y == py) {
                            if (path.size > 1) {
                                enemy.path = path
                                enemy.pathIndex = 0
                                enemy.traversing = true
                                enemy.traverseSleep = System.currentTimeMillis()

                                val dx2 = player.transform.x - enemy.transform.x
                                val dy2 = player.transform.y - enemy.transform.y
                                enemy.lastPlayerAngle = Math.atan2(dy2.toDouble(), dx2.toDouble()).toFloat()
                            }
                        }
                    }
                }
            }
            else if (enemy.path.size > 0 && enemy.pathIndex < enemy.path.size) {
                // Move to first tile
                val target = Vector2i(enemy.path[enemy.pathIndex])
                target.x *= 64
                target.y *= 64
                val dx2 = (target.x + 32) - enemy.transform.x
                val dy2 = (target.y + 32) - enemy.transform.y
                val ln = Math.sqrt((dx2*dx2+dy2*dy2).toDouble());
                var vx: Float
                var vy: Float
                if (ln == 0.0) {
                    vx = dx2
                    vy = dy2
                }
                else {
                    vx = (dx2 / ln).toFloat()
                    vy = (dy2 / ln).toFloat()
                }

                if (enemy.pushBack == 0) {
                    enemy.pushBackImmune = false
                    enemy.transform.x += vx * enemy.walkingSpeedFactor
                    enemy.transform.y += vy * enemy.walkingSpeedFactor
                    if (vy > 0.0f) {
                        enemy.animator.setAnimation("walk_down")
                    }
                    else {
                        enemy.animator.setAnimation("walk_up")
                    }
                }
                else if (enemy.pushBack > 5) {
                    enemy.transform.x += enemy.pushDirection.x.toFloat()
                    enemy.transform.y += enemy.pushDirection.y.toFloat()
                    enemy.pushBack -= 1
                    enemy.animator.setAnimation("idle_down")
                }
                else {
                    enemy.pushBack -= 1
                    enemy.animator.setAnimation("idle_down")
                }

                val dx3 = (target.x + 32) - enemy.transform.x
                val dy3 = (target.y + 32) - enemy.transform.y
                val ln2 = Math.sqrt((dx3 * dx3 + dy3 * dy3).toDouble());
                if (ln2 <= 4.0f) {
                    enemy.pathIndex += 1
                    if (enemy.pathIndex >= enemy.path.size - 1) {
                        enemy.traversing = false
                    }
                }
                else if (enemy.pathIndex >= 3) {
                    enemy.traversing = false
                }
            }
            else {
                enemy.traversing = false
            }
        }

        for (container in activeContainers) {
            val tx = (container.transform.x / 64.0f).toInt()
            val ty = (container.transform.y / 64.0f).toInt()
            val clc = lightValues[tx + ty * width].w
            container.getRenderComponents()[0].addCustomUniformData(0, clc)

            if (!container.open && !container.looted) {
                val attackTransform = player.attack.transform.worldTransform()
                if (attackTransform.x + 32.0f >= container.transform.x - 32.0f && attackTransform.x - 32.0f <= container.transform.x + 32.0f &&
                    attackTransform.y + 32.0f >= container.transform.y - 32.0f && attackTransform.y - 32.0f <= container.transform.y + 32.0f) {
                    container.open = true
                }
            }

            if (container.open && !container.looted) {
                container.looted = true

                for (i in 0 until container.numItems) {
                    val combination = ITEM_COMBINATIONS[random.nextInt(ITEM_COMBINATIONS.size)]
                    val name = combination.second[random.nextInt(combination.second.size)]

                    var quality = random.nextFloat() * random.nextFloat()
                    quality *= 100
                    quality += random.nextFloat() * (player.currentLevel.toFloat()) * 0.02f
                    var qualityName = "None"
                    var qualityIndex = 1.0f
                    for (q in ITEM_QUALITIES) {
                        if (quality.toInt() in q.first) {
                            qualityName = q.second
                            break
                        }

                        qualityIndex += 1
                    }

                    val finalQuality = (qualityIndex*qualityIndex + qualityIndex).toInt()

                    val item = if (combination.first.type != ItemType.POTION) {
                        Item(player, combination.first.type, "$qualityName $name", random.nextInt(finalQuality)+1, random.nextInt(finalQuality)+1,
                                random.nextInt(finalQuality)+1,random.nextInt(finalQuality)+1, 0, false, 0.0f)
                    }
                    else {
                        val health = (combination.second.size+1)*10
                        Item(player, combination.first.type, "$qualityName $name", 0, 0, 0, 0, health, true, 0.0f)
                    }

                    levelItemSystem.newEntity(item)
                            .attachRenderComponent(itemMaterial, quadMesh)
                            .build()
                    val angle = random.nextFloat()*Math.PI
                    val direction = Vector2f(Math.sin(angle).toFloat(), Math.cos(angle).toFloat())
                    direction.x *= 64.0f
                    direction.y *= 64.0f
                    item.setPosition(Vector2i(container.transform.x.toInt()+direction.x.toInt(), container.transform.y.toInt()+direction.y.toInt()))
                    item.cellX = player.cellX
                    item.cellY = player.cellY
                    item.transform.sx = 40.0f
                    item.transform.sy = 40.0f
                    item.transform.z = 2.0f
                    item.getRenderComponents()[0].textureTileOffset = Vector2i(3+combination.first.textureOffset.x,11+combination.first.textureOffset.y)
                }
            }
        }
    }

    fun getFirstTilePos(): Vector2i {
        return Vector2i(startPosition.x * 64, startPosition.y * 64)
    }

    fun create(resourceFactory: ResourceFactory, scene: Scene, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
        firstBuild = true
        maxCellX = mapWidth / width
        maxCellY = mapHeight / height
        texture = resourceFactory.buildTexture2d()
                .withName("tilemapTexture")
                .fromImageFile("./data/textures/tiles.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        texture.setTiledTexture(16,16)
        tilemapMaterial = resourceFactory.buildMaterial()
                .withName("tilemapMaterial")
                .withVertexShader("./data/shaders/tilemap.vert.spv")
                .withFragmentShader("./data/shaders/tilemap.frag.spv")
                .withTexture(texture)
                .withBlendEnabled(false)
                .build()

        itemMaterial = resourceFactory.buildMaterial()
                .withName("itemMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(texture)
                .withBatching(false)
                .build()

        this.mapWidth = mapWidth
        this.mapHeight = mapHeight
        this.width = width
        this.height = height
        map = IntArray(mapWidth*mapHeight)
        navMesh = NavMesh(width, height)
        navMesh.allowDiagonals = false

        enemyTexture = resourceFactory.buildTexture2d()
                .withName("enemyTexture")
                .fromImageFile("./data/textures/krac2.0.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        enemyTexture.setTiledTexture(16,16)
        enemyMaterial = resourceFactory.buildMaterial()
                .withName("enemyMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(enemyTexture)
                .withBatching(false)
                .build()

        enemySystem = scene.newSystem(enemyMaterial)

        enemyAttackMaterial = resourceFactory.buildMaterial()
                .withName("enemyAttackMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(texture)
                .withDepthWrite(true)
                .withBlendEnabled(false)
                .withBatching(false)
                .build()

        enemyAttackSystem = scene.newSystem(enemyAttackMaterial)

        collisionSystem = scene.newSystem(null)
        containerSystem = scene.newSystem(itemMaterial)
        levelItemSystem = scene.newSystem(itemMaterial)
        xpBallSystem = scene.newSystem(itemMaterial)

        torchTexture = resourceFactory.buildTexture2d()
                .withName("torch")
                .fromImageFile("./data/textures/torch.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        torchMaterial = resourceFactory.buildMaterial()
                .withName("torchMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(torchTexture)
                .withBatching(false)
                .build()
        torchSystem = scene.newSystem(torchMaterial)

        lightVertices = FloatArray(width*height*6*6){0.0f}
        lightVerticesBuffer = memAlloc(lightVertices.size*4)
        lightValues = Array(width*height){Vector4f()}
        lightMap = resourceFactory.buildVertexBuffer()
                .withVertices(lightVerticesBuffer)
                .withState(VertexBufferState.DYNAMIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 4))
                .build()

        lightMapMaterial = resourceFactory.buildMaterial()
                .withName("lightMapMaterial")
                .withVertexShader("./data/shaders/light.vert.spv")
                .withFragmentShader("./data/shaders/light.frag.spv")
                .withTexture(torchTexture)
                .withDepthWrite(true)
                .withBlendEnabled(true)
                .withSrcColor(BlendMode.BLEND_FACTOR_DST_COLOR)
                .withDstColor(BlendMode.BLEND_FACTOR_ZERO)
                .build()

        val lightTransform = Transform()
        lightTransform.z = 17.0f
        //scene.addSimpleDraw(SimpleDraw(lightTransform, lightMap, lightMapMaterial))

        enemyTargetSystem = scene.newSystem(itemMaterial)
        enemyTargetEntity = Entity()
        enemyTargetSystem.newEntity(enemyTargetEntity)
                .attachRenderComponent(itemMaterial, quadMesh)
                .build()

        val enemyTargetEntityRenderer = enemyTargetEntity.getRenderComponents()[0]
        enemyTargetEntityRenderer.visible = false
        enemyTargetEntityRenderer.textureTileOffset.set(4,15)
        enemyTargetEntityRenderer.addCustomUniformData(0, 1.0f)
    }

    fun switchCell(resourceFactory: ResourceFactory, cellX: Int, cellY: Int) {
        for (enemy in activeEnemies) {
            enemy.getRenderComponents()[0].visible = false
            enemy.healthBar.getRenderComponents()[0].visible = false
        }

        for (container in activeContainers) {
            container.getRenderComponents()[0].visible = false
        }

        for (lights in activeLightSources) {
            if (lights.getRenderComponents().isNotEmpty()) {
                lights.getRenderComponents()[0].visible = false
            }

            lights.emitter.enabled = false
        }

        activeEnemies.clear()
        activeContainers.clear()
        activeLightSources.clear()

        for (room in rooms) {
            val removeMe = ArrayList<Enemy>()
            for (enemy in room.enemies) {
                if (enemy.health <= 0) {
                    removeMe.add(enemy)
                    continue
                }

                if (enemy.cellX == cellX && enemy.cellY == cellY) {
                    enemy.getRenderComponents()[0].visible = enemy.health > 0 && enemy.cellX == cellX && enemy.cellY == cellY
                    enemy.healthBar.getRenderComponents()[0].visible = enemy.getRenderComponents()[0].visible
                    if (enemy.getRenderComponents()[0].visible) {
                        activeEnemies.add(enemy)
                    }
                }
            }

            for (enemy in removeMe) {
                room.enemies.remove(enemy)
            }

            for (container in room.containers) {
                if (container.cellX == cellX && container.cellY == cellY) {
                    container.getRenderComponents()[0].visible = container.cellX == cellX && container.cellY == cellY
                    activeContainers.add(container)
                }
            }

            for (light in room.torches) {
                if (light.cellX == cellX && light.cellY == cellY) {
                    light.getRenderComponents()[0].visible = false

                    light.emitter.enabled = true
                    activeLightSources.add(light)
                }
            }

            for (light in room.campfire) {
                if (light.cellX == cellX && light.cellY == cellY) {
                    light.emitter.enabled = true
                    activeLightSources.add(light)
                }
            }
        }

        val backIndices = Array(width*height){ TileGfxNone }
        val frontIndices = Array(width*height){ TileGfxNone }
        var sx = cellX * width
        var sy = cellY * height

        var cx = 0.0f
        var cy = 0.0f
        collisionSystem.clear()
        collisionBoxes.clear()
        for (i in 0 until width*height) {
            if (sx + sy*mapWidth >= map.size) {
                break
            }

            backIndices[i] = mapBackIndices[sx + sy*mapWidth]
            frontIndices[i] = mapFrontIndices[sx + sy*mapWidth]

            if (map[sx + sy*mapWidth] == 1) {
                navMesh.map[i] = 127
                collisionBoxes.add(Vector4i(cx.toInt(), cy.toInt(), 64, 64))

                val e = Entity()
                collisionSystem.newEntity(e)
                        .attachBoxColliderComponent(64.0f, 64.0f, BodyDef.BodyType.StaticBody)
                        .build()
                val tr = e.transform
                val cl = collisionSystem.findColliderComponent(e.getId())!!
                cl.setPosition(cx + 32.0f, cy + 32.0f)
                cl.setFriction(0.15f)
                tr.sx = 64.0f
                tr.sy = 64.0f
                tr.z = 13.0f
            }
            else {
                navMesh.map[i] = 0
            }

            sx += 1
            if (sx >= cellX * width + width) {
                sx = cellX * width
                sy += 1
            }

            cx += 64.0f
            if (cx >= width * 64.0f) {
                cx = 0.0f
                cy += 64.0f
            }
        }

        for (container in activeContainers) {
            if (container.cellX == cellX && container.cellY == cellY) {
                val ix: Int = (container.transform.x/64).toInt()
                val iy: Int = (container.transform.y/64).toInt()
                navMesh.map[ix + iy*width] = 127.toByte()
            }
        }

        if (firstBuild) {
            backTilemap.create(resourceFactory, tilemapMaterial, width, height, 64.0f, 64.0f, backIndices)
            frontTilemap.create(resourceFactory, tilemapMaterial, width, height, 64.0f, 64.0f, frontIndices)
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
            backTilemap.transform.setPosition(0.0f, 0.0f, 1.0f)
            frontTilemap.transform.setPosition(0.0f, 0.0f, 10.0f)

            firstBuild = false
        }
        else {
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
        }
    }

    private fun spreadLightOnTilemap(cellX: Int, cellY: Int) {
        val backIndices = Array(width*height){ TileGfxNone }
        var sx = cellX * width
        var sy = cellY * height

        for (i in 0 until width*height) {
            if (sx + sy*mapWidth >= map.size) {
                break
            }

            backIndices[i] = mapBackIndices[sx + sy*mapWidth]
            backIndices[i].red = 0.0f
            backIndices[i].green = 0.0f
            backIndices[i].blue = 0.0f
            backIndices[i].alpha = 1.0f

            sx += 1
            if (sx >= cellX * width + width) {
                sx = cellX * width
                sy += 1
            }
        }

        generateLightMap(backIndices)
        backTilemap.update(backIndices)
    }

    private fun generateLightMap(backIndices: Array<TileGfx>) {
        // Clear old light values
        for (i in 0 until lightValues.size) {
            lightValues[i] = Vector4f(0.48f, 0.62f, 0.69f, 0.2f)
        }

        // Put out light values
        for (light in activeLightSources) {
            val t = light.transform
            var x = ((t.x-32.0f) / 64.0f).toInt()
            var y = ((t.y-32.0f) / 64.0f).toInt()

            if (x < 0) { x = 0 }
            else if (x >= width) { x = width - 1 }
            if (y < 0) { y = 0 }
            else if (y >= height) { y = height - 1 }
            lightValues[x + y * width] = Vector4f(light.color.x, light.color.y, light.color.z, 0.9f)
            spreadLight(x, y, lightValues[x + y * width])
        }

        // Put out light values at XpBalls
        for (xp in xpBallSystem.getEntityList()) {
            if (xp!!.getRenderComponents()[0].visible) {
                val t = xp.transform
                val x = (t.x / 64.0f).toInt()
                val y = (t.y / 64.0f).toInt()
                spreadLight(x, y, Vector4f(0.0f, 1.0f, 0.0f, 1.0f))
            }
        }

        val px = ((player.transform.x) / 64.0f).toInt()
        val py = ((player.transform.y) / 64.0f).toInt()
        spreadLight(px, py, Vector4f(0.48f, 0.62f, 0.69f, 1.0f))

        var x = 0.0f
        var y = 0.0f
        var ix = 0
        var iy = 0
        var index = 0
        for (i in 0 until lightValues.size) {
            val top = if (iy > 0) { lightValues[ix + (iy-1)*width] } else { lightValues[i] }
            val bot = if (iy < height-1) { lightValues[ix + (iy+1)*width] } else { lightValues[i] }

            val topleft = if (ix > 0 && iy > 0) { lightValues[(ix-1) + (iy-1)*width] } else { lightValues[i] }
            val left = if (ix > 0) { lightValues[(ix-1) + iy*width] } else { lightValues[i] }
            val botleft = if (ix > 0 && iy < height-1) { lightValues[(ix-1) + (iy+1)*width]} else { lightValues[i] }

            val topRight = if (ix < width-1 && iy > 0) { lightValues[(ix+1) + (iy-1)*width]} else { lightValues[i] }
            val right = if (ix < width-1) { lightValues[(ix+1) + iy*width] } else { lightValues[i] }
            val botRight = if (ix < width-1 && iy < height-1) { lightValues[(ix+1) + (iy+1)*width] } else { lightValues[i] }

            backIndices[ix + iy * width].red = (lightValues[i].x)
            backIndices[ix + iy * width].green = (lightValues[i].y)
            backIndices[ix + iy * width].blue = (lightValues[i].z)
            backIndices[ix + iy * width].alpha = (lightValues[i].w)

            lightVertices[index] = x
            lightVertices[index+1] = y
            lightVertices[index+2] = (lightValues[i].x+topleft.x+left.x+top.x)*0.25f
            lightVertices[index+3] = (lightValues[i].y+topleft.y+left.y+top.y)*0.25f
            lightVertices[index+4] = (lightValues[i].z+topleft.z+left.z+top.z)*0.25f
            lightVertices[index+5] = (lightValues[i].w+topleft.w+left.w+top.w)*0.25f

            lightVertices[index+6] = x
            lightVertices[index+7] = y+64.0f
            lightVertices[index+8] = (lightValues[i].x+botleft.x+left.x+bot.x)*0.25f
            lightVertices[index+9] = (lightValues[i].y+botleft.y+left.y+bot.y)*0.25f
            lightVertices[index+10] = (lightValues[i].z+botleft.z+left.z+bot.z)*0.25f
            lightVertices[index+11] = (lightValues[i].w+botleft.w+left.w+bot.w)*0.25f

            lightVertices[index+12] = x+64.0f
            lightVertices[index+13] = y+64.0f
            lightVertices[index+14] = (lightValues[i].x+botRight.x+right.x+bot.x)*0.25f
            lightVertices[index+15] = (lightValues[i].y+botRight.y+right.y+bot.y)*0.25f
            lightVertices[index+16] = (lightValues[i].z+botRight.z+right.z+bot.z)*0.25f
            lightVertices[index+17] = (lightValues[i].w+botRight.w+right.w+bot.w)*0.25f

            lightVertices[index+18] = x+64.0f
            lightVertices[index+19] = y+64.0f
            lightVertices[index+20] = (lightValues[i].x+botRight.x+right.x+bot.x)*0.25f
            lightVertices[index+21] = (lightValues[i].y+botRight.y+right.y+bot.y)*0.25f
            lightVertices[index+22] = (lightValues[i].z+botRight.z+right.z+bot.z)*0.25f
            lightVertices[index+23] = (lightValues[i].w+botRight.w+right.w+bot.w)*0.25f

            lightVertices[index+24] = x+64.0f
            lightVertices[index+25] = y
            lightVertices[index+26] = (lightValues[i].x+topRight.x+right.x+top.x)*0.25f
            lightVertices[index+27] = (lightValues[i].y+topRight.y+right.y+top.y)*0.25f
            lightVertices[index+28] = (lightValues[i].z+topRight.z+right.z+top.z)*0.25f
            lightVertices[index+29] = (lightValues[i].w+topRight.w+right.w+top.w)*0.25f

            lightVertices[index+30] = x
            lightVertices[index+31] = y
            lightVertices[index+32] = (lightValues[i].x+topleft.x+left.x+top.x)*0.25f
            lightVertices[index+33] = (lightValues[i].y+topleft.y+left.y+top.y)*0.25f
            lightVertices[index+34] = (lightValues[i].z+topleft.z+left.z+top.z)*0.25f
            lightVertices[index+35] = (lightValues[i].w+topleft.w+left.w+top.w)*0.25f

            index += 36
            x += 64.0f
            if (x >= width*64.0f) {
                x = 0.0f
                y += 64.0f
            }

            ix += 1
            if (ix >= width) {
                ix = 0
                iy += 1
            }
        }

        lightVerticesBuffer.asFloatBuffer().put(lightVertices).flip()
        lightMap.update(lightVerticesBuffer)
    }

    private fun spreadLight(x: Int, y: Int, value: Vector4f) {
        if (value.w <= 0.0f) {
            return
        }

        val att = 0.2f
        if (x in 0..(width - 1) && y >= 0 && y < height) {
            val r = (lightValues[x + y * width].x + value.x) * 0.5f
            val g = (lightValues[x + y * width].y + value.y) * 0.5f
            val b = (lightValues[x + y * width].z + value.z) * 0.5f
            val a = Math.max(lightValues[x + y * width].w, value.w)
            lightValues[x + y * width] = Vector4f(r,g,b,a)
        }

        val mx = player.cellX * width + x
        val my = player.cellY * height + y
        val color = Vector4f(value.x, value.y, value.z, value.w - att)

        if (x in 1..width) {
            if (y in 0..(height - 1) && lightValues[(x-1) + y * width].w < color.w) {
                if (map[(mx-1) + my * mapWidth] == 0) {
                    spreadLight(x - 1, y, color)
                }
            }

            if (y in 1..height) {
                if (lightValues[(x-1) + (y-1) * width].w < color.w) {
                    if (map[(mx-1) + (my-1) * mapWidth] == 0) {
                        spreadLight(x - 1, y - 1, color)
                    }
                }
            }

            if (y in 0..(height-2)) {
                if (lightValues[(x-1) + (y+1) * width].w < color.w) {
                    if (map[(mx-1) + (my+1) * mapWidth] == 0) {
                        spreadLight(x - 1, y + 1, color)
                    }
                }
            }
        }

        if (x in 0..(width-2)) {
            if (y in 0..(height-1) && lightValues[(x+1) + y * width].w < color.w) {
                if (map[(mx+1) + my * mapWidth] == 0) {
                    spreadLight(x + 1, y, color)
                }
            }

            if (y in 1..height && lightValues[(x+1) + (y-1) * width].w < color.w) {
                if (map[(mx+1) + (my-1) * mapWidth] == 0) {
                    spreadLight(x + 1, y - 1, color)
                }
            }

            if (y in 0..(height-2) && lightValues[(x+1) + (y+1) * width].w < color.w) {
                if (map[(mx+1) + (my+1) * mapWidth] == 0) {
                    spreadLight(x + 1, y + 1, color)
                }
            }
        }

        if (x in 0..(width-1) && y in 1..height) {
            if (lightValues[x + (y-1) * width].w < color.w) {
                if (map[mx + (my-1) * mapWidth] == 0) {
                    spreadLight(x, y - 1, color)
                }
            }
        }

        if (x in 0..(width-1) && y in 0..(height-2)) {
            if (lightValues[x + (y+1) * width].w < color.w) {
                if (map[mx + (my+1) * mapWidth] == 0) {
                    spreadLight(x, y + 1, color)
                }
            }
        }
    }

    fun buildFirstRoom(scene: Scene) {
        random = Random(System.currentTimeMillis())
        var x = 0
        var y = 0
        val firstRoomTiles = ArrayList<Vector2i>()
        for (i in 0 until width*height) {
            if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                map[x + (y*mapWidth)] = 1
            }
            else if (y == 1 && (x == 1 || x == width-2)) {
                map[x + (y*mapWidth)] = 1
            }
            else {
                firstRoomTiles.add(Vector2i(x, y))
            }

            x += 1
            if (x >= width) {
                x = 0
                y += 1
            }
        }

        val room = Room(firstRoomTiles, Vector4i(0,0,width,height), DIRT_ROOM)
        rooms.add(room)

        mapBackIndices = Array(mapWidth*mapHeight){ TileGfxNone }
        mapFrontIndices = Array(mapWidth*mapHeight){ TileGfxNone }
        populateTilemap()

        // Set position of start and exit
        val startRoom = rooms[0]
        val endRoom = rooms[0]

        startPosition = Vector2i(width/2, 3)
        exitPosition = Vector2i(width/2, height/2)

        val exitType = when (endRoom.type) {
            DIRT_ROOM -> 0
            COLD_DIRT_ROOM -> 2
            KRAC_BASE -> 1
            else -> throw IllegalStateException("Room type " + endRoom.type + " is not supported!")
        }

        mapBackIndices[exitPosition.x + exitPosition.y * mapWidth] = TileGfx(2, exitType)
        room.generateLightsInRoom(scene, random, map, mapWidth, width, height, width*2+height*2, false, torchSystem, itemMaterial, quadMesh)

        // Put campfire next to exit on first level
        val emitter = scene.createParticleEmitter(2.0f, 40, 20.0f)
        emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
        emitter.endColor.set(0.8f, 0.2f, 0.0f, 0.1f)
        emitter.velocity.y = -20.0f
        emitter.transform.y = 16.0f
        emitter.transform.sx = 48.0f
        emitter.transform.sy = 48.0f
        emitter.transform.z = 5.0f
        emitter.enabled = false

        val tx = exitPosition.x % width
        val ty = exitPosition.y % height
        val et = LightSource(exitPosition.x / width, exitPosition.y / height, Vector3f(0.9f, 0.55f, 0.1f), emitter)
        torchSystem.newEntity(et)
                .build()

        et.transform.setPosition(((tx*64) + 32).toFloat(), ((ty*64) - 32).toFloat(), 1.0f)
        et.transform.sx = 1.0f
        et.transform.sy = 1.0f

        emitter.transform.parentTransform = et.transform
        room.campfire.add(et)

        // Give the player a nice chest to start off with
        val container = Container(0, 5)
        containerSystem.newEntity(container)
                .attachRenderComponent(itemMaterial, quadMesh)
                .build()

        container.setPosition(Vector2i((tx+1)*64 + 32, ty*64 + 32))
        container.getRenderComponents()[0].visible = true
        room.containers.add(container)
    }

    fun build(seed: Long, scene: Scene, healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        random = Random(seed)
        map = IntArray(mapWidth*mapHeight){1}
        buildRooms()

        mapBackIndices = Array(mapWidth * mapHeight) { TileGfxNone }
        mapFrontIndices = Array(mapWidth * mapHeight) { TileGfxNone }
        populateTilemap()

        // Set position of start and exit
        val startRoom = rooms[0]
        val endRoom = rooms[0]

        startPosition = startRoom.findNoneEdgeTile(random)!!
        exitPosition = endRoom.findNoneEdgeTile(random)!!

        val exitType = when (endRoom.type) {
            DIRT_ROOM -> 0
            COLD_DIRT_ROOM -> 2
            KRAC_BASE -> 1
            else -> throw IllegalStateException("Room type " + endRoom.type + " is not supported!")
        }

        mapBackIndices[exitPosition.x + exitPosition.y * mapWidth] = TileGfx(2, exitType)

        generateRooms(scene, healthBarSystem, healthBarMaterial)

        val pixelData = BufferUtils.createByteBuffer(mapWidth * mapHeight * 3)
        for (room in rooms) {
            var R = random.nextInt(127).toByte()
            var G = random.nextInt(127).toByte()
            var B = random.nextInt(127).toByte()

            /*if (room.type == KRAC_BASE) {
                R = 0
                G = 127
                B = 0
            } else if (room.type == DIRT_ROOM) {
                R = 127
                G = 64
                B = 0
            } else if (room.type == COLD_DIRT_ROOM) {
                R = 127
                G = 96
                B = 96
            }*/

            for (tile in room.tiles) {
                pixelData.put((tile.x + tile.y * mapWidth) * 3, R)
                pixelData.put((tile.x + tile.y * mapWidth) * 3 + 1, G)
                pixelData.put((tile.x + tile.y * mapWidth) * 3 + 2, B)
            }

            for (enemy in room.enemies) {
                val x = ((enemy.transform.x / 64.0f) + (enemy.cellX * width)).toInt()
                val y = ((enemy.transform.y / 64.0f) + (enemy.cellY * height)).toInt()

                pixelData.put((x + y * mapWidth) * 3, 127)
                pixelData.put((x + y * mapWidth) * 3 + 1, 0)
                pixelData.put((x + y * mapWidth) * 3 + 2, 0)
            }

            for (container in room.containers) {
                val x = ((container.transform.x / 64.0f) + (container.cellX * width)).toInt()
                val y = ((container.transform.y / 64.0f) + (container.cellY * height)).toInt()
                pixelData.put((x + y * mapWidth) * 3, 127)
                pixelData.put((x + y * mapWidth) * 3 + 1, 100)
                pixelData.put((x + y * mapWidth) * 3 + 2, 0)
            }
        }
        STBImageWrite.stbi_write_png("levels/level.png", mapWidth, mapHeight, 3, pixelData, mapWidth * 3)
    }

    private fun addWallBlockersAtEdges() {
        var x = 0
        var y = 0
        for (i in 0 until map.size) {
            if (x > 0 && x < mapWidth-1 && (x%width) == 0 && map[x + y*mapWidth] == 1) {
                map[(x-1)+y*mapWidth] = 1
                map[(x+1)+y*mapWidth] = 1
            }

            if (y > 0 && y < mapHeight-1 && (y%height) == 0 && map[x + y*mapWidth] == 1) {
                map[x+(y-1)*mapWidth] = 1
                map[x+(y+1)*mapWidth] = 1
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }
    }

    private fun populateTilemap() {
        for (room in rooms) {
            val tileY = when (room.type) {
                DIRT_ROOM -> 0
                COLD_DIRT_ROOM -> 2
                KRAC_BASE -> 1
                else -> throw IllegalStateException("Not implemented!")
            }

            for (tile in room.tiles) {
                val index = tile.x + tile.y * mapWidth
                mapBackIndices[index] = TileGfx(0, tileY)

                if (tile.y > 0) {
                    if (map[tile.x + (tile.y-1)*mapWidth] == 1) {
                        mapBackIndices[tile.x + (tile.y - 1) * mapWidth] = TileGfx(1, tileY)

                        if (tile.y > 1) {
                            if (map[tile.x + (tile.y-2)*mapWidth] == 1) {
                                mapFrontIndices[tile.x + (tile.y - 2) * mapWidth] = TileGfx(3, tileY)
                            }
                            else {
                                mapFrontIndices[tile.x + (tile.y - 2) * mapWidth] = TileGfx(3, tileY)
                                map[tile.x + (tile.y - 2) * mapWidth] = 1
                            }
                        }
                    }
                }

                // Add black tiles at the edges where there's atleast 3 walls below a floor tile
                // The wall closest to the top floor tile will be populated with a black tile
                if (tile.y < mapHeight - 3) {
                    if (map[tile.x + (tile.y+1) * mapWidth] == 1 &&
                        map[tile.x + (tile.y+2) * mapWidth] == 1 &&
                        map[tile.x + (tile.y+3) * mapWidth] == 1) {
                        mapFrontIndices[tile.x + (tile.y+1) * mapWidth] = TileGfx(0, 11)
                        map[tile.x + (tile.y + 1) * mapWidth] = 1
                    }
                }

                val r = random.nextInt(20)
                if (r == 1){
                    mapBackIndices[tile.x + tile.y * mapWidth] = TileGfx(random.nextInt(3) + 4, tileY)
                }
            }
        }

        // Remove tiles that are now solid
        for (room in rooms) {
            val tilesToRemove = ArrayList<Vector2i>()
            for (tile in room.tiles) {
                val index = tile.x + tile.y * mapWidth
                if (map[index] == 1) {
                    tilesToRemove.add(tile)
                }
            }

            for (tile in tilesToRemove) {
                room.tiles.remove(tile)
            }
        }
    }

    private fun generate(repeats: Int) {
        var x = 0
        var y = 0
        for (i in 0 until map.size) {
            map[i] = random.nextInt(2)

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        repeat(repeats) {
            var x1 = 0
            var y1 = 0
            for (i in 0 until map.size) {
                val c = numNeighbours(x1,y1)

                if (map[x1 + y1 * mapWidth] == 0 && c < 4) {
                    map[x1 + y1 * mapWidth] = 1
                }
                else if (map[x1 + y1 * mapWidth] == 1 && c >= 5) {
                    map[x1 + y1 * mapWidth] = 0
                }

                x1 += 1
                if (x1 >= mapWidth) {
                    x1 = 0
                    y1 += 1
                }
            }
        }

        x = 0
        y = 0
        for (i in 0 until map.size) {
            if (x == 0 || x == mapWidth-1 || y == 0 || y == mapHeight-1) {
                map[x + y*mapWidth] = 1
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }
    }

    private fun numNeighbours(x: Int, y: Int): Int {
        var count = 0
        if (x > 0) {
            if (map[(x-1) + y * mapWidth] == 0) {
                count += 1
            }

            if (y > 0) {
                if (map[(x-1) + (y-1) * mapWidth] == 0) {
                    count += 1
                }
            }

            if (y < mapHeight - 1) {
                if (map[(x-1) + (y+1) * mapWidth] == 0) {
                    count += 1
                }
            }
        }

        if (x < mapWidth - 1) {
            if (map[(x+1) + y * mapWidth] == 0) {
                count += 1
            }

            if (y > 0) {
                if (map[(x+1) + (y-1) * mapWidth] == 0) {
                    count += 1
                }
            }

            if (y < mapHeight - 1) {
                if (map[(x+1) + (y+1) * mapWidth] == 0) {
                    count += 1
                }
            }
        }

        if (y > 0) {
            if (map[x + (y-1) * mapWidth] == 0) {
                count += 1
            }
        }

        if (y < mapHeight - 1) {
            if (map[x + (y+1) * mapWidth] == 0) {
                count += 1
            }
        }

        return count
    }

    private fun buildRooms() {
        rooms.clear()
        val numRooms = random.nextInt(40) + 10
        val maxW = mapWidth / 8
        val maxH = mapHeight / 8

        val roomAreaList = ArrayList<Vector4i>()
        for (i in 0 until numRooms) {
            var roomX = random.nextInt(mapWidth)
            var roomY = random.nextInt(mapHeight)
            val roomW = random.nextInt(maxW) + 10
            val roomH = random.nextInt(maxH) + 10

            if (roomX + roomW >= mapWidth) {
                roomX = mapWidth - roomW - 1
            }
            if (roomY + roomH >= mapHeight) {
                roomY = mapHeight - roomH - 1
            }

            roomAreaList.add(Vector4i(roomX, roomY,roomW,roomH))
        }

        var collision: Boolean
        var iterationsCount = 0
        do {
            collision = false
            for (area in roomAreaList) {
                for (other in roomAreaList) {
                    if (area != other) {
                        if (area.x + area.z >= other.x && area.x <= other.x + other.z
                        && area.y + area.w >= other.y && area.y <= other.y + other.w) {
                            collision = true
                            var dx = ((area.x+area.z/2) - (other.x+other.z/2)).sign
                            var dy = ((area.y+area.w/2) - (other.y+other.w/2)).sign

                            if (dx == 0 && dy == 0) {
                                dx = -1
                                dy = -1
                            }

                            if (area.x + dx in 0..(mapWidth - 1 - area.z)) {
                                area.x += dx
                            }

                            if (area.y + dy in 0..(mapHeight - 1 - area.w)) {
                                area.y += dy
                            }

                            if (other.x - dx in 0..(mapWidth - 1 - other.z)) {
                                other.x -= dx
                            }

                            if (other.y - dy in 0..(mapHeight - 1 - other.w)) {
                                other.y -= dy
                            }
                        }
                    }
                }
            }
            iterationsCount++

            // If we arn't finished after 100 rounds - we should break
            if (iterationsCount >= 100) {
                break
            }

        } while (collision)

        // If any rooms are still colliding at this point we want to remove them
        val toRemove = ArrayList<Vector4i>()
        for (area in roomAreaList) {
            for (other in roomAreaList) {
                if (area != other) {
                    if (area.x + area.z >= other.x && area.x <= other.x + other.z
                        && area.y + area.w >= other.y && area.y <= other.y + other.w
                    ) {
                        if (area.z*area.w < other.z*other.w) {
                            toRemove.add(area)
                        }
                        else {
                            toRemove.add(other)
                        }
                    }
                }
            }
        }

        for (area in toRemove) {
            roomAreaList.remove(area)
        }

        for (area in roomAreaList) {
            val tiles = ArrayList<Vector2i>()
            var x = area.x
            var y = area.y
            for (i in 0 until area.z*area.w) {
                tiles.add(Vector2i(x, y))
                map[x + y * mapWidth] = 0
                x += 1
                if (x >= area.x + area.z) {
                    x = area.x
                    y += 1
                }
            }

            val room = Room(tiles, area, DIRT_ROOM)
            rooms.add(room)
        }

        findNearestNeighbourOfRooms()
        connectRooms()
    }

    private fun findNearestNeighbourOfRooms() {
        for (room in rooms) {
            while (room.neighbourRooms.size < 2) {
                var nearestRoom: Room? = null
                var shortestDist = Double.MAX_VALUE
                for (otherRoom in rooms) {
                    if (otherRoom == room || room.neighbourRooms.contains(otherRoom) || otherRoom.neighbourRooms.contains(room)) {
                        continue
                    }

                    val dist = otherRoom.area.distance(room.area)
                    if (dist < shortestDist) {
                        shortestDist = dist
                        nearestRoom = otherRoom
                    }
                }

                if (nearestRoom != null) {
                    room.neighbourRooms.add(nearestRoom)
                }
                else {
                    break
                }
            }
        }
    }

    private fun connectRooms() {
        for (room in rooms) {
            for (neighbour in room.neighbourRooms) {
                var shortestLength = Int.MAX_VALUE
                var firstTile = Vector2i(0,0)
                var secondTile = Vector2i(0,0)

                // Find the tile closest to the neighbour rooms center as a starting point
                var ln = Int.MAX_VALUE
                for (tile in room.tiles) {
                    val dx = ((neighbour.area.x+neighbour.area.z)/2) - tile.x
                    val dy = ((neighbour.area.y+neighbour.area.w)/2) - tile.y
                    val k = dx*dx + dy*dy
                    if (k < ln) {
                        firstTile = tile
                        ln = k
                    }
                }

                // Find the tile in the neighbouring room which is closest to the selected
                // tile
                for (otherTile in neighbour.tiles) {
                    val dx = otherTile.x - firstTile.x
                    val dy = otherTile.y - firstTile.y
                    val d = dx * dx + dy * dy

                    if (d < shortestLength) {
                        shortestLength = d
                        secondTile = otherTile
                    }
                }

                val dx = (secondTile.x - firstTile.x).sign
                val dy = (secondTile.y - firstTile.y).sign

                var x = firstTile.x
                var y = firstTile.y

                // Create a tunnel between the two rooms, but stop as soon as we hit a collision
                // This happens in cases when there's another room in between the two neighbouring
                // rooms
                while (true) {
                    var traversalDone = 0

                    if (map[x + y * mapWidth] == 1) {
                        map[x + y * mapWidth] = 0
                        room.tiles.add(Vector2i(x,y))
                    }

                    if (x != secondTile.x) {
                        if (y > 0) {
                            if (map[x + (y - 1) * mapWidth] == 1) {
                                map[x + (y - 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x, y - 1))
                            }
                        }

                        if (y < mapHeight - 1) {
                            if (map[x + (y + 1) * mapWidth] == 1) {
                                map[x + (y + 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x, y + 1))
                            }
                        }
                        x += dx
                    }
                    else {
                        if (x > 0) {
                            if (map[(x - 1) + y * mapWidth] == 1) {
                                map[(x - 1) + y * mapWidth] = 0
                                room.tiles.add(Vector2i(x - 1, y))
                            }
                        }

                        if (x < mapWidth - 1) {
                            if (map[(x + 1) + y * mapWidth] == 1) {
                                map[(x + 1) + y * mapWidth] = 0
                                room.tiles.add(Vector2i(x + 1, y))
                            }
                        }

                        traversalDone += 1
                        if (y != secondTile.y) {
                            y += dy
                        }
                        else {
                            traversalDone += 1
                        }
                    }

                    if (traversalDone == 2) {
                        break
                    }
                }
            }
        }
    }

    private fun generateRooms(scene: Scene, healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        for (room in rooms) {
            // Only generate enemies in the correct rooms
            if (room.type.hasEnemies) {
                val enemyDensity = room.tiles.size / 64
                val numEnemies = random.nextInt(enemyDensity) + 1

                room.generateEnemiesInRoom(
                    random,
                    enemySystem,
                    enemyMaterial,
                    quadMesh,
                    enemyAttackSystem,
                    player,
                    numEnemies,
                    healthBarSystem,
                    healthBarMaterial,
                    enemyAttackMaterial,
                    room.type.enemyTypes
                )
            }

            val thisRoomContainerCount = room.tiles.size/72
            room.generateContainersInRoom(random, thisRoomContainerCount, containerSystem, itemMaterial, quadMesh)

            if (room.type.hasLights) {
                val lightDensity = room.tiles.size / 48
                val numLights = random.nextInt(lightDensity) + 1
                room.generateLightsInRoom(
                    scene,
                    random,
                    map,
                    mapWidth,
                    width,
                    height,
                    numLights,
                    random.nextInt(10) == 1,
                    torchSystem,
                    itemMaterial,
                    quadMesh
                )
            }
        }
    }
}
