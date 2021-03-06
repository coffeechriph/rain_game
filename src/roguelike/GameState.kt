package roguelike

import org.joml.Vector2i
import rain.api.Input
import rain.api.components.Animator
import rain.api.gfx.*
import rain.api.gui.v2.*
import rain.api.scene.Camera
import rain.api.scene.Scene
import rain.api.scene.SceneManager
import roguelike.Entity.Inventory
import roguelike.Entity.Player
import roguelike.Level.Level
import roguelike.Level.TILE_WIDTH

class GameState(sceneManager: SceneManager, resourceFactory: ResourceFactory): Scene(sceneManager, resourceFactory) {
    lateinit var menuScene: MenuState
    lateinit var quadMesh: Mesh
    private lateinit var attackMaterial: Material
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private lateinit var chestArmorTexture: Texture2d
    private lateinit var chestArmorMaterial: Material
    private lateinit var legsArmorTexture: Texture2d
    private lateinit var legsArmorMaterial: Material
    private lateinit var handsArmorTexture: Texture2d
    private lateinit var handsArmorMaterial: Material
    private lateinit var bootsArmorTexture: Texture2d
    private lateinit var bootsArmorMaterial: Material
    private var playerAnimator = Animator()
    private lateinit var player: Player
    private lateinit var inventory: Inventory
    private lateinit var container: Panel
    private lateinit var currentLevelText: Label

    // TODO: The depth range is aquired from the renderer
    // TODO: Create a method in scene to create a new camera which auto-injects the depth range
    private var camera = Camera(1000.0f, Vector2i(1280, 768))
    private lateinit var level: Level

    override fun init() {
        val quadVertexBuffer = resourceFactory.buildVertexBuffer().as2dQuad()
        quadMesh = Mesh(quadVertexBuffer, null)

        mobTexture = resourceFactory.buildTexture2d()
                .withName("mobTexture")
                .fromImageFile("./data/textures/dwarf_tiny.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        mobTexture.setTiledTexture(8, 8)
        mobMaterial = resourceFactory.buildMaterial()
                .withName("mobMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(mobTexture)
                .withBatching(false)
                .build()

        chestArmorTexture = resourceFactory.buildTexture2d()
            .withName("chestArmorTexture")
            .fromImageFile("./data/textures/chest-armor.png")
            .withFilter(TextureFilter.NEAREST)
            .build()
        chestArmorTexture.setTiledTexture(16,16)

        chestArmorMaterial = resourceFactory.buildMaterial()
            .withTexture(chestArmorTexture)
            .withVertexShader("./data/shaders/equipment.vert.spv")
            .withFragmentShader("./data/shaders/basic.frag.spv")
            .withName("chestArmorMaterial")
            .withDepthWrite(false)
            .withDepthTest(false)
            .build()

        legsArmorTexture = resourceFactory.buildTexture2d()
            .withName("legArmorTexture")
            .fromImageFile("./data/textures/leg-armor.png")
            .withFilter(TextureFilter.NEAREST)
            .build()
        legsArmorTexture.setTiledTexture(16,16)

        legsArmorMaterial = resourceFactory.buildMaterial()
            .withTexture(legsArmorTexture)
            .withVertexShader("./data/shaders/equipment.vert.spv")
            .withFragmentShader("./data/shaders/basic.frag.spv")
            .withName("legsArmorMaterial")
            .withDepthWrite(false)
            .withDepthTest(false)
            .build()

        bootsArmorTexture = resourceFactory.buildTexture2d()
            .withName("bootsArmorTexture")
            .fromImageFile("./data/textures/boots-armor.png")
            .withFilter(TextureFilter.NEAREST)
            .build()
        bootsArmorTexture.setTiledTexture(16,16)

        bootsArmorMaterial = resourceFactory.buildMaterial()
            .withTexture(bootsArmorTexture)
            .withVertexShader("./data/shaders/equipment.vert.spv")
            .withFragmentShader("./data/shaders/basic.frag.spv")
            .withName("bootsArmorMaterial")
            .withDepthWrite(false)
            .withDepthTest(false)
            .build()

        handsArmorTexture = resourceFactory.buildTexture2d()
            .withName("handsArmorTexture")
            .fromImageFile("./data/textures/hands-armor.png")
            .withFilter(TextureFilter.NEAREST)
            .build()
        handsArmorTexture.setTiledTexture(16,16)

        handsArmorMaterial = resourceFactory.buildMaterial()
            .withTexture(handsArmorTexture)
            .withVertexShader("./data/shaders/equipment.vert.spv")
            .withFragmentShader("./data/shaders/basic.frag.spv")
            .withName("handsArmorMaterial")
            .withDepthWrite(false)
            .withDepthTest(false)
            .build()

        player = Player()
        player.chestArmorMaterial = chestArmorMaterial
        player.legsArmorMaterial = legsArmorMaterial
        player.bootsArmorMaterial = bootsArmorMaterial
        player.handsArmorMaterial = handsArmorMaterial
        player.equipmentMesh = quadMesh
        newEntity(player)
                .attachRenderComponent(mobMaterial, quadMesh)
                .attachAnimatorComponent(playerAnimator)
                .build()

        level = Level(player, resourceFactory)
        level.quadMesh = quadMesh

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

        newEntity(player.attack)
                .attachRenderComponent(attackMaterial, quadMesh)
                .attachAnimatorComponent(Animator())
                .build()

        player.attack.getAnimatorComponent()[0].addAnimation("idle", 0, 0, 0, 0.0f)
        player.attack.getAnimatorComponent()[0].addAnimation("attack", 0, 4, 0, 40.0f)
        player.attack.getRenderComponents()[0].visible = false

        // TODO: Constant window dimensions
        level.create(resourceFactory, this, (8960 / TILE_WIDTH).toInt(),
            (5376 / TILE_WIDTH).toInt(), (1280 / TILE_WIDTH).toInt(), (768 / TILE_WIDTH).toInt()
        )

        sceneManager.activeCamera = camera

        player.transform.x = 720.0f
        player.transform.y = 384.0f

        inventory = Inventory(player)
        player.inventory = inventory
        player.level = level

        // TODO: Constant window dimensions
        container = guiManagerCreatePanel(FillRowLayout())
        container.moveable = false
        container.resizable = false
        container.x = 1280.0f/2.0f - 100
        container.y = 768.0f - 40.0f
        container.w = 200.0f
        container.h = 40.0f
        currentLevelText = container.createLabel("Current Level: ${player.currentLevel}")
        currentLevelText.x = 0.0f
        currentLevelText.y = 0.0f
        currentLevelText.background = false
        currentLevelText.x = currentLevelText.w/2.0f
    }

    override fun update(input: Input) {
        if (player.health <= 0) {
            sceneManager.unloadScene(this)
            sceneManager.loadScene(menuScene)
        }

        if (player.playerMovedCell) {
            level.switchCell(this, player.movingDirection)
            player.playerMovedCell = false
        }

        level.update(this, input)

        if (inventory.visible) {
            inventory.update(input)
        }
    }
}
