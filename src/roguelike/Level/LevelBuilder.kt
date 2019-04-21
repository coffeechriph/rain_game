package roguelike.Level

import org.joml.Random
import org.joml.Vector2i
import org.joml.Vector4i
import rain.api.components.Animator
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.scene.Scene
import rain.api.scene.parse.JsonSceneLoader
import rain.assertion
import roguelike.Entity.*
import java.io.File

class LevelBuilder(var cellWidth: Int, var cellHeight: Int) {
    private val cellTypes = ArrayList<CellType>()
    private val currentLevelCells = ArrayList<Cell>()

    fun loadCellTypes() {
        if (!File("./data/cells").exists()) {
            assertion("/data/cells directory does not exist!")
        }

        val directory = File("./data/cells")
        for (file in directory.listFiles()) {
            if (file.isDirectory) {
                continue
            }

            if (file.absolutePath.endsWith(".json")) {
                val loadedScene = JsonSceneLoader().load(file.absolutePath)
                val cell = CellType(cellWidth, cellHeight)
                cell.create(loadedScene)
                cellTypes.add(cell)
            }
        }
    }

    fun createLevel(random: Random, scene: Scene, tilemapMaterial: Material): List<Cell> {
        for (cell in currentLevelCells) {
            for (layer in cell.layers) {
                scene.removeTilemap(layer)
            }
        }
        currentLevelCells.clear()

        val firstCellType = cellTypes.stream().filter { type -> type.hasConnectionTop && type.hasConnectionRight && type.hasConnectionLeft && type.hasConnectionBot }.findFirst().get()
        val firstCell = Cell(firstCellType, scene, tilemapMaterial)
        currentLevelCells.add(firstCell)
        createCellConnections(firstCell, random, scene, tilemapMaterial)

        return currentLevelCells
    }

    private fun createCellConnections(cell: Cell, random: Random, scene: Scene, tilemapMaterial: Material) {
        val MAX_NO_CELLS = 20
        val otherCellTypes = cellTypes.filter { type -> type != cell.cellType }

        if (cell.cellType.hasConnectionBot && cell.botNeighbourCell == null) {
            val viableCells = ArrayList<CellType>()
            for (type in otherCellTypes) {
                if (type.hasConnectionTop) {
                    if (currentLevelCells.size < MAX_NO_CELLS || type.singleConnection()) {
                        viableCells.add(type)
                    }
                }
            }

            if (viableCells.size > 0) {
                val rndType = viableCells[random.nextInt(viableCells.size)]
                var nbot = findCellAtMapPos(cell.mapPosX, cell.mapPosY + 1)
                if (nbot == null) {
                    nbot = Cell(rndType, scene, tilemapMaterial)
                    nbot.mapPosX = cell.mapPosX
                    nbot.mapPosY = cell.mapPosY + 1
                    currentLevelCells.add(nbot)
                }

                cell.botNeighbourCell = nbot
                nbot.topNeighbourCell = cell

                if (!rndType.singleConnection()) {
                    createCellConnections(nbot, random, scene, tilemapMaterial)
                }
            }
        }

        if (cell.cellType.hasConnectionTop && cell.topNeighbourCell == null) {
            val viableCells = ArrayList<CellType>()
            for (type in otherCellTypes) {
                if (type.hasConnectionBot) {
                    if (currentLevelCells.size < MAX_NO_CELLS || type.singleConnection()) {
                        viableCells.add(type)
                    }
                }
            }

            if (viableCells.size > 0) {
                val rndType = viableCells[random.nextInt(viableCells.size)]
                var ntop = findCellAtMapPos(cell.mapPosX, cell.mapPosY - 1)
                if (ntop == null) {
                    ntop = Cell(rndType, scene, tilemapMaterial)
                    ntop.mapPosX = cell.mapPosX
                    ntop.mapPosY = cell.mapPosY - 1
                    currentLevelCells.add(ntop)
                }

                cell.topNeighbourCell = ntop
                ntop.botNeighbourCell = cell

                if (!rndType.singleConnection()) {
                    createCellConnections(ntop, random, scene, tilemapMaterial)
                }
            }
        }

        if (cell.cellType.hasConnectionLeft && cell.leftNeighbourCell == null) {
            val viableCells = ArrayList<CellType>()
            for (type in otherCellTypes) {
                if (type.hasConnectionRight) {
                    if (currentLevelCells.size < MAX_NO_CELLS || type.singleConnection()) {
                        viableCells.add(type)
                    }
                }
            }

            if (viableCells.size > 0) {
                val rndType = viableCells[random.nextInt(viableCells.size)]
                var nleft = findCellAtMapPos(cell.mapPosX - 1, cell.mapPosY)
                if (nleft == null) {
                    nleft = Cell(rndType, scene, tilemapMaterial)
                    nleft.mapPosX = cell.mapPosX - 1
                    nleft.mapPosY = cell.mapPosY
                    currentLevelCells.add(nleft)
                }
                cell.leftNeighbourCell = nleft
                nleft.rightNeighbourCell = cell

                if (!rndType.singleConnection()) {
                    createCellConnections(nleft, random, scene, tilemapMaterial)
                }
            }
        }

        if (cell.cellType.hasConnectionRight && cell.rightNeighbourCell == null) {
            val viableCells = ArrayList<CellType>()
            for (type in otherCellTypes) {
                if (type.hasConnectionLeft) {
                    if (currentLevelCells.size < MAX_NO_CELLS || type.singleConnection()) {
                        viableCells.add(type)
                    }
                }
            }

            if (viableCells.size > 0) {
                val rndType = viableCells[random.nextInt(viableCells.size)]
                var nright = findCellAtMapPos(cell.mapPosX + 1, cell.mapPosY)
                if (nright == null) {
                    nright = Cell(rndType, scene, tilemapMaterial)
                    nright.mapPosX = cell.mapPosX + 1
                    nright.mapPosY = cell.mapPosY
                    currentLevelCells.add(nright)
                }
                cell.rightNeighbourCell = nright
                nright.leftNeighbourCell = cell

                if (!rndType.singleConnection()) {
                    createCellConnections(nright, random, scene, tilemapMaterial)
                }
            }
        }
    }

    fun createCollisionBoxes(currentCell: Cell): List<Vector4i> {
        val collisionBoxes = ArrayList<Vector4i>()

        for (map in currentCell.cellType.definition.map) {
            for (layer in map.layers) {
                var hasCollision = false
                for (metadata in layer.metadata) {
                    if (metadata.name == "hasCollision") {
                        hasCollision = true
                    }
                }

                if (hasCollision) {
                    for (group in layer.mapLayerTileGroup) {
                        for (index in group.tileIndicesIntoMap) {
                            val x = index % cellWidth
                            val y = index / cellWidth
                            val w = TILE_WIDTH
                            val h = TILE_WIDTH
                            collisionBoxes.add(Vector4i((x*w).toInt(), (y*h).toInt(), (w).toInt(), (h).toInt()))
                        }
                    }
                }
            }
        }

        return collisionBoxes
    }

    fun createEnemies(random: Random, currentCell: Cell, scene: Scene, enemyMaterial: Material,
                      enemyAttackMaterial: Material, quadMesh: Mesh, enemyAnimator: Animator,
                      level: Int, healthMaterial: Material, player: Player): MutableList<Enemy> {
        val enemies = ArrayList<Enemy>()
        for (entities in currentCell.cellType.definition.entities) {
            for (metadata in entities.value.metadata) {
                // TODO: Specify way to more precisely specify enemy type
                if (metadata.name == "enemy") {
                    for (instance in entities.value.definitionInstances) {
                        val typeIndex = random.nextInt(EnemyType.values().size)
                        val type = EnemyType.values()[typeIndex]
                        val enemyEntity = when (type) {
                            EnemyType.STONE_GOBLIN -> StoneGoblin(random)
                            EnemyType.STONE_OGRE -> StoneOgre(random)
                            EnemyType.STONE_RAT -> StoneRat(random)
                        }
                        enemyEntity.transform.x = instance.posX
                        enemyEntity.transform.y = instance.posY
                        enemyEntity.transform.z = instance.posZ
                        enemyEntity.transform.sx = instance.width
                        enemyEntity.transform.sy = instance.height
                        scene.newEntity(enemyEntity)
                            .attachRenderComponent(enemyMaterial, quadMesh)
                            .attachAnimatorComponent(enemyAnimator)
                            .build()

                        scene.newEntity(enemyEntity.attackAreaVisual)
                            .attachRenderComponent(enemyAttackMaterial, quadMesh)
                            .build()

                        val attackRenderComponent = enemyEntity.attackAreaVisual.getRenderComponents()[0]
                        attackRenderComponent.visible = true
                        attackRenderComponent.textureTileOffset.set(5,15)

                        enemyEntity.attackAreaVisualRenderComponent = attackRenderComponent
                        enemyEntity.attackAreaVisualTransform = enemyEntity.attackAreaVisual.transform

                        val levelFactor = (level*1.5f).toInt()
                        enemyEntity.strength = (random.nextInt(levelFactor) + levelFactor*10 * enemyEntity.strengthFactor).toInt()
                        enemyEntity.agility = (random.nextInt(levelFactor) + levelFactor*4 * enemyEntity.agilityFactor).toInt()
                        enemyEntity.health = (100 + random.nextInt(levelFactor) * enemyEntity.healthFactor).toInt()
                        enemyEntity.getRenderComponents()[0].visible = true

                        val et = enemyEntity.transform
                        enemyEntity.healthBar.parentTransform = et

                        scene.newEntity(enemyEntity.healthBar)
                            .attachRenderComponent(healthMaterial, quadMesh)
                            .build()

                        enemyEntity.healthBar.getRenderComponents()[0].visible = true
                        enemyEntity.healthBar.transform.sx = 60.0f
                        enemyEntity.healthBar.transform.sy = 7.0f

                        enemyEntity.setPosition(Vector2i(instance.posX.toInt(), instance.posY.toInt()))
                        enemyEntity.player = player
                        enemies.add(enemyEntity)
                    }
                }
            }
        }

        return enemies
    }

    private fun findCellAtMapPos(x: Int, y: Int): Cell? {
        for (cell in currentLevelCells) {
            if (cell.mapPosX == x && cell.mapPosY == y) {
                return cell
            }
        }

        return null
    }
}
