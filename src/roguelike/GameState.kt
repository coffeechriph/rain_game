package roguelike

import roguelike.Entity.Attack
import roguelike.Entity.HealthBar
import roguelike.Entity.Inventory
import roguelike.Entity.Player
import roguelike.Level.Level
import org.joml.Vector2f
import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.entity.EntitySystem
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.Container
import rain.api.gui.Gui
import rain.api.gui.Text
import rain.api.scene.Camera
import rain.api.scene.Scene

class GameState(stateManager: StateManager): State(stateManager) {
    private lateinit var attackMaterial: Material
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private lateinit var healthMaterial: Material
    private lateinit var playerSystem: EntitySystem<Player>
    private lateinit var attackSystem: EntitySystem<Attack>
    private lateinit var healthBarSystem: EntitySystem<HealthBar>
    private lateinit var player: Player
    private lateinit var inventory: Inventory
    private lateinit var container: Container
    private lateinit var currentLevelText: Text

    // TODO: The depth range is aquired from the renderer
    // TODO: Create a method in scene to create a new camera which auto-injects the depth range
    private var camera = Camera(Vector2f(0.0f, 20.0f))
    private lateinit var level: Level

    override fun init(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
        mobTexture = resourceFactory.buildTexture2d()
                .withName("mobTexture")
                .fromImageFile("./data/textures/dwarf.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        mobTexture.setTiledTexture(16,16)
        mobMaterial = resourceFactory.buildMaterial()
                .withName("mobMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(mobTexture)
                .withBatching(false)
                .build()

        player = Player()
        playerSystem = scene.newSystem(mobMaterial)
        playerSystem.newEntity(player)
                .attachTransformComponent()
                .attachSpriteComponent()
                .attachAnimatorComponent()
                .build()

        level = Level(player, resourceFactory)

        val attackTexture = resourceFactory.buildTexture2d()
                .withName("attackTexture")
                .fromImageFile("./data/textures/attack.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        attackTexture.setTiledTexture(16,16)
        attackMaterial = resourceFactory.buildMaterial()
                .withName("attackMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(attackTexture)
                .withBatching(false)
                .build()

        attackSystem = scene.newSystem(attackMaterial)
        attackSystem.newEntity(player.attack)
                .attachTransformComponent()
                .attachSpriteComponent()
                .attachAnimatorComponent()
                .build()

        val healthTexture = resourceFactory.buildTexture2d()
                .withName("healthTexture")
                .fromImageFile("./data/textures/health.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        healthMaterial = resourceFactory.buildMaterial()
                .withName("healthMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(healthTexture)
                .withBatching(false)
                .build()
        healthBarSystem = scene.newSystem(healthMaterial)

        // TODO: Constant window dimensions
        level.create(resourceFactory, scene, 8960 / 64, 5376 / 64, 1280 / 64, 768 / 64)
        level.buildFirstRoom()
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)

        camera = Camera(Vector2f(0.0f, 20.0f))
        scene.activeCamera = camera

        player.setPosition(level.getFirstTilePos())
        level.switchCell(resourceFactory, player.cellX, player.cellY)

        inventory = Inventory(gui, player)
        player.inventory = inventory
        player.level = level

        // TODO: Constant window dimensions
        container = gui.newContainer(1280.0f/2.0f - 100, 768.0f - 40.0f, 200.0f, 40.0f)
        currentLevelText = container.addText("Current Level: ${player.currentLevel}", 0.0f, 0.0f, background = true)
        currentLevelText.x += currentLevelText.w/2.0f
    }

    override fun update(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input, deltaTime: Float) {
        if (player.health <= 0) {
            stateManager.startState("menu")
        }

        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }

        level.update(deltaTime, input)

        if (inventory.visible) {
            inventory.update(input)
        }

        if (player.transform.x + player.cellX*level.width*64 >= level.exitPosition.x*64 - 32 && player.transform.x + player.cellX*level.width*64 <= level.exitPosition.x*64 + 32 &&
                player.transform.y + player.cellY*level.height*64 >= level.exitPosition.y*64 - 32 && player.transform.y + player.cellY*level.height*64 <= level.exitPosition.y*64 + 32) {
            player.currentLevel += 1
            level.build(System.currentTimeMillis(), healthBarSystem, healthMaterial)
            player.setPosition(level.getFirstTilePos())
            level.switchCell(resourceFactory, player.cellX, player.cellY)

            container.removeText(currentLevelText)
            currentLevelText = container.addText("Current Level: ${player.currentLevel}", 0.0f, 0.0f, background = true)
            currentLevelText.x += currentLevelText.w/2.0f
        }
    }
}
