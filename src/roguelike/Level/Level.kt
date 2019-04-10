package roguelike.Level

import org.joml.*
import rain.api.Input
import rain.api.entity.Entity
import rain.api.gfx.*
import rain.api.scene.*
import roguelike.Entity.*
import java.lang.Math

const val TILE_WIDTH = 48.0f
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

    private lateinit var lightValues: Array<Vector4f>
    private lateinit var tilemapMaterial: Material
    private lateinit var itemMaterial: Material
    private lateinit var texture: Texture2d
    private lateinit var torchTexture: Texture2d
    private lateinit var torchMaterial: Material
    private var firstBuild = true

    private var random = Random(System.currentTimeMillis())
    private lateinit var enemyTexture: Texture2d
    private lateinit var enemyMaterial: Material
    private lateinit var enemyAttackMaterial: Material
    private lateinit var enemyTargetEntity: Entity
    private lateinit var navMesh: NavMesh

    private var delayLightUpdate = 0

    private val activeEnemies = ArrayList<Enemy>()
    private val activeContainers = ArrayList<Container>()
    private val activeLightSources = ArrayList<LightSource>()
    private lateinit var collisionBoxes: List<Vector4i>
    lateinit var quadMesh: Mesh

    private lateinit var currentLevelCells: List<Cell>
    private var currentCell: Cell? = null
    private lateinit var levelBuilder: LevelBuilder

    fun lightIntensityAt(x: Float, y: Float): Float {
        var tx = (x / TILE_WIDTH).toInt()
        var ty = (y / TILE_WIDTH).toInt()
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

    fun create(resourceFactory: ResourceFactory, scene: Scene, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
        this.mapWidth = mapWidth
        this.mapHeight = mapHeight
        this.width = width
        this.height = height
        firstBuild = true

        maxCellX = mapWidth / width
        maxCellY = mapHeight / height
        texture = resourceFactory.buildTexture2d()
            .withName("tilemapTexture")
            .fromImageFile("./data/textures/tilemap_8x8.png")
            .withFilter(TextureFilter.NEAREST)
            .build()

        texture.setTiledTexture(8,8)
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

        enemyAttackMaterial = resourceFactory.buildMaterial()
            .withName("enemyAttackMaterial")
            .withVertexShader("./data/shaders/basic.vert.spv")
            .withFragmentShader("./data/shaders/basic.frag.spv")
            .withTexture(texture)
            .withDepthWrite(true)
            .withBlendEnabled(false)
            .withBatching(false)
            .build()

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
        lightValues = Array(width*height){Vector4f()}

        enemyTargetEntity = Entity()
        scene.newEntity(enemyTargetEntity)
            .attachRenderComponent(itemMaterial, quadMesh)
            .build()

        val enemyTargetEntityRenderer = enemyTargetEntity.getRenderComponents()[0]
        enemyTargetEntityRenderer.visible = false
        enemyTargetEntityRenderer.textureTileOffset.set(4,15)
        enemyTargetEntityRenderer.addCustomUniformData(0, 1.0f)

        levelBuilder = LevelBuilder(width, height)
        levelBuilder.loadCellTypes()
        currentLevelCells = levelBuilder.createLevel(random, scene, tilemapMaterial)
        currentCell = currentLevelCells[0]
        collisionBoxes = levelBuilder.createCollisionBoxes(currentCell!!)
    }

    fun update(scene: Scene, input: Input) {
        currentCell!!.visible = true

        val lc = lightIntensityAt(player.transform.x, player.transform.y)
        player.getRenderComponents()[0].addCustomUniformData(0, lc)
        player.bootsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.chestArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.legsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)
        player.handsArmor.getRenderComponents()[0].addCustomUniformData(1, lc)

        if (delayLightUpdate == 0) {
            spreadLightOnTilemap(0, 0)
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
            scene.removeEntity(e)
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

            var x = enemy.transform.x.toInt()/TILE_WIDTH
            var y = enemy.transform.y.toInt()/TILE_WIDTH

            val elc = lightValues[(x + y * width).toInt()].w
            enemy.getRenderComponents()[0].addCustomUniformData(0, elc)

            if (x >= width) {
                x = (width-1).toFloat()
            }
            if (y >= height) {
                y = (height - 1).toFloat()
            }
            if (x < 0) {
                x = 0.0f
            }
            if (y < 0) {
                y = 0.0f
            }

            navMesh.map[(x + y * width).toInt()] = 127.toByte()
            enemy.lastX = x.toInt()
            enemy.lastY = y.toInt()
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
                targetTransform.sx = TILE_WIDTH
                targetTransform.sy = TILE_WIDTH
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
                        scene.newEntity(xpBall)
                                .attachRenderComponent(itemMaterial, quadMesh)
                                .build()

                        var px = enemy.transform.x.toInt() + (Math.sin(random.nextFloat()*Math.PI*2) * 32.0f).toInt()
                        var py = enemy.transform.y.toInt() + (Math.cos(random.nextFloat()*Math.PI*2) * 32.0f).toInt()

                        if (px < 0) {
                            px = 0
                        }
                        if (px > (width-1)*TILE_WIDTH) {
                            px = ((width - 1) * TILE_WIDTH).toInt()
                        }

                        if (py < 0) {
                            py = 0
                        }
                        if (py > (height-1)*TILE_WIDTH) {
                            py = ((height - 1) * TILE_WIDTH).toInt()
                        }

                        xpBall.setPosition(Vector2i(px, py))
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

            val etx = (enemy.transform.x / TILE_WIDTH).toInt()
            val ety = (enemy.transform.y / TILE_WIDTH).toInt()
            val ptx = (player.transform.x / TILE_WIDTH).toInt()
            val pty = (player.transform.y / TILE_WIDTH).toInt()

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
                    val worldX = enemy.transform.x / TILE_WIDTH
                    val worldY = enemy.transform.y / TILE_WIDTH
                    val px = (player.transform.x / TILE_WIDTH).toInt()
                    val py = (player.transform.y / TILE_WIDTH).toInt()

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
                target.x *= TILE_WIDTH.toInt()
                target.y *= TILE_WIDTH.toInt()
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
            val tx = (container.transform.x / TILE_WIDTH).toInt()
            val ty = (container.transform.y / TILE_WIDTH).toInt()
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

                    scene.newEntity(item)
                            .attachRenderComponent(itemMaterial, quadMesh)
                            .build()
                    val angle = random.nextFloat()*Math.PI
                    val direction = Vector2f(Math.sin(angle).toFloat(), Math.cos(angle).toFloat())
                    direction.x *= TILE_WIDTH
                    direction.y *= TILE_WIDTH
                    item.setPosition(Vector2i(container.transform.x.toInt()+direction.x.toInt(), container.transform.y.toInt()+direction.y.toInt()))
                    item.transform.sx = 40.0f
                    item.transform.sy = 40.0f
                    item.transform.z = 2.0f
                    item.getRenderComponents()[0].textureTileOffset = Vector2i(3+combination.first.textureOffset.x,11+combination.first.textureOffset.y)
                }
            }
        }
    }

    fun switchCell(dir: Direction) {
        when(dir) {
            Direction.LEFT -> {
                if (currentCell!!.leftNeighbourCell != null) {
                    currentCell!!.visible = false
                    currentCell = currentCell!!.leftNeighbourCell
                    collisionBoxes = levelBuilder.createCollisionBoxes(currentCell!!)
                    currentCell!!.visible = true
                }
            }
            Direction.RIGHT -> {
                if (currentCell!!.rightNeighbourCell != null) {
                    currentCell!!.visible = false
                    currentCell = currentCell!!.rightNeighbourCell
                    collisionBoxes = levelBuilder.createCollisionBoxes(currentCell!!)
                    currentCell!!.visible = true
                }
            }
            Direction.UP -> {
                if (currentCell!!.topNeighbourCell != null) {
                    currentCell!!.visible = false
                    currentCell = currentCell!!.topNeighbourCell
                    collisionBoxes = levelBuilder.createCollisionBoxes(currentCell!!)
                    currentCell!!.visible = true
                }
            }
            Direction.DOWN -> {
                if (currentCell!!.botNeighbourCell != null) {
                    currentCell!!.visible = false
                    currentCell = currentCell!!.botNeighbourCell
                    collisionBoxes = levelBuilder.createCollisionBoxes(currentCell!!)
                    currentCell!!.visible = true
                }
            }
        }
    }

    private fun spreadLightOnTilemap(cellX: Int, cellY: Int) {
    }

    private fun generateLightMap() {
        // Clear old light values
        for (i in 0 until lightValues.size) {
            lightValues[i] = Vector4f(0.48f, 0.62f, 0.69f, 0.2f)
        }

        // Put out light values
        for (light in activeLightSources) {
            val t = light.transform
            var x = ((t.x-32.0f) / TILE_WIDTH).toInt()
            var y = ((t.y-32.0f) / TILE_WIDTH).toInt()

            if (x < 0) { x = 0 }
            else if (x >= width) { x = width - 1 }
            if (y < 0) { y = 0 }
            else if (y >= height) { y = height - 1 }
            lightValues[x + y * width] = Vector4f(light.color.x, light.color.y, light.color.z, 0.9f)
            spreadLight(x, y, lightValues[x + y * width])
        }

        // TODO: Put out light values at XpBalls
        /*for (xp in xpBallSystem.getEntityList()) {
            if (xp!!.getRenderComponents()[0].visible) {
                val t = xp.transform
                val x = (t.x / TILE_WIDTH).toInt()
                val y = (t.y / TILE_WIDTH).toInt()
                spreadLight(x, y, Vector4f(0.0f, 1.0f, 0.0f, 1.0f))
            }
        }*/

        val px = ((player.transform.x) / TILE_WIDTH).toInt()
        val py = ((player.transform.y) / TILE_WIDTH).toInt()
        spreadLight(px, py, Vector4f(0.48f, 0.62f, 0.69f, 1.0f))

        var x = 0.0f
        var y = 0.0f
        var ix = 0
        var iy = 0
        var index = 0
        for (i in 0 until lightValues.size) {
            // mapLayers[0].setTile(ix, iy, lightValues[i].x, lightValues[i].y, lightValues[i].z, lightValues[i].w)

            index += 36
            x += TILE_WIDTH
            if (x >= width*TILE_WIDTH) {
                x = 0.0f
                y += TILE_WIDTH
            }

            ix += 1
            if (ix >= width) {
                ix = 0
                iy += 1
            }
        }
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

        val mx = width + x
        val my = height + y
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
}
