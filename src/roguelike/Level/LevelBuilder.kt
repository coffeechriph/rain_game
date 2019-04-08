package roguelike.Level

import org.joml.Random
import org.joml.Vector4i
import rain.api.gfx.Material
import rain.api.scene.Scene
import rain.api.scene.parse.JsonSceneLoader
import rain.assertion
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

            val loadedScene = JsonSceneLoader().load(file.absolutePath)
            val cell = CellType(cellWidth, cellHeight)
            cell.create(loadedScene)
            cellTypes.add(cell)
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
        val cell = Cell(firstCellType, scene, tilemapMaterial)
        currentLevelCells.add(cell)
        createCellConnections(cell, random, scene, tilemapMaterial)

        println("IS ANY CELL BROKEN?")
        for (cell in currentLevelCells) {
            if (cell.cellType.hasConnectionBot && cell.botNeighbourCell == null) {
                println("BOT NEIGHBOUR DOES NOT EXIST!")
            }
            if (cell.cellType.hasConnectionTop && cell.topNeighbourCell == null) {
                println("TOP NEIGHBOUR DOES NOT EXIST!")
            }
            if (cell.cellType.hasConnectionLeft && cell.leftNeighbourCell == null) {
                println("LEFT NEIGHBOUR DOES NOT EXIST!")
            }
            if (cell.cellType.hasConnectionRight && cell.rightNeighbourCell == null) {
                println("RIGHT NEIGHBOUR DOES NOT EXIST!")
            }
        }
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

    private fun findCellAtMapPos(x: Int, y: Int): Cell? {
        for (cell in currentLevelCells) {
            if (cell.mapPosX == x && cell.mapPosY == y) {
                return cell
            }
        }

        return null
    }
}
