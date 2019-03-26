package roguelike.Level

import rain.api.scene.parse.SceneDefinition
import rain.assertion

private var cellTypeId: Long = 0
class CellType(
    val numTileX: Int,
    val numTileY: Int) {
    private val id = cellTypeId++
    var hasExit: Boolean = false
    var hasConnectionTop = false
    var hasConnectionBot = false
    var hasConnectionLeft = false
    var hasConnectionRight = false
    lateinit var definition: SceneDefinition

    fun singleConnection(): Boolean {
        if (hasConnectionTop) {
            return !(hasConnectionBot || hasConnectionLeft || hasConnectionRight)
        }

        if (hasConnectionBot) {
            return !(hasConnectionTop || hasConnectionLeft || hasConnectionRight)
        }

        if (hasConnectionLeft) {
            return !(hasConnectionBot || hasConnectionTop || hasConnectionRight)
        }

        if (hasConnectionRight) {
            return !(hasConnectionBot || hasConnectionLeft || hasConnectionTop)
        }

        assertion("Not all cases are covered")
    }

    fun create(sceneDefinition: SceneDefinition) {
        if (sceneDefinition.map.isEmpty() || sceneDefinition.map.size > 1) {
            assertion("Cell must only include a single map!")
        }

        for (mapDefinition in sceneDefinition.map) {
            if (mapDefinition.tileNumX != numTileX || mapDefinition.tileNumY != numTileY) {
                assertion("The size of the map must be ${numTileX}x$numTileY but is ${mapDefinition.tileNumX}x${mapDefinition.tileNumY}")
            }

            for (layer in mapDefinition.layers) {
                for (metadata in layer.metadata) {
                    when {
                        metadata.name == "levelExit" -> hasExit = true
                        metadata.name == "hasConnectionLeft" -> hasConnectionLeft = true
                        metadata.name == "hasConnectionRight" -> hasConnectionRight = true
                        metadata.name == "hasConnectionTop" -> hasConnectionTop = true
                        metadata.name == "hasConnectionBot" -> hasConnectionBot = true
                    }
                }
            }
        }

        definition = sceneDefinition
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CellType) {
            return false
        }

        return other.id == id
    }
}
