package roguelike.Level

import org.joml.Vector2i
import rain.api.gfx.Material
import rain.api.scene.Scene
import rain.api.scene.Tilemap
import roguelike.Entity.Container
import roguelike.Entity.Enemy

class Cell(var cellType: CellType, scene: Scene, material: Material) {
    val layers = ArrayList<Tilemap>()
    val enemies = ArrayList<Enemy>()
    val containers = ArrayList<Container>()
    var mapPosX: Int = 0
    var mapPosY: Int = 0
    var leftNeighbourCell: Cell? = null
    var rightNeighbourCell: Cell? = null
    var topNeighbourCell: Cell? = null
    var botNeighbourCell: Cell? = null
    var visible = false
        set(value) {
            for (tilemap in layers) {
                tilemap.visible = value
            }

            field = value
        }

    init {
        for (map in cellType.definition.map) {
            for ((layerIndex, layer) in map.layers.withIndex()) {
                val tilemap = scene.createTilemap(
                    material,
                    map.tileNumX,
                    map.tileNumY,
                    map.tileWidth,
                    map.tileHeight
                )

                for (group in layer.mapLayerTileGroup) {
                    for (index in group.tileIndicesIntoMap) {
                        tilemap.setTile(
                            index % map.tileNumX,
                            index / map.tileNumX,
                            group.imageX,
                            group.imageY)
                    }
                }

                tilemap.transform.z = layerIndex.toFloat() * 2
                tilemap.visible = false
                layers.add(tilemap)
            }
        }
    }
}
