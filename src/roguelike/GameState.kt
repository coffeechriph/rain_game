package roguelike

import org.joml.Vector2f
import org.joml.Vector2i
import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.components.Animator
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.*
import rain.api.gui.v2.*
import rain.api.scene.Camera
import rain.api.scene.Scene
import roguelike.Entity.Attack
import roguelike.Entity.HealthBar
import roguelike.Entity.Inventory
import roguelike.Entity.Player
import roguelike.Level.Level

class GameState(stateManager: StateManager): State(stateManager) {
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
    private lateinit var healthMaterial: Material
    private lateinit var playerSystem: EntitySystem<Player>
    private lateinit var chestArmorSystem: EntitySystem<Entity>
    private lateinit var legsArmorSystem: EntitySystem<Entity>
    private lateinit var bootsArmorSystem: EntitySystem<Entity>
    private lateinit var handsArmorSystem: EntitySystem<Entity>
    private var playerAnimator = Animator()
    private lateinit var attackSystem: EntitySystem<Attack>
    private lateinit var healthBarSystem: EntitySystem<HealthBar>
    private lateinit var player: Player
    private lateinit var inventory: Inventory
    private lateinit var container: Panel
    private lateinit var currentLevelText: Label

    // TODO: The depth range is aquired from the renderer
    // TODO: Create a method in scene to create a new camera which auto-injects the depth range
    private var camera = Camera(1000.0f, Vector2i(1280, 768))
    private lateinit var level: Level

    override fun init(resourceFactory: ResourceFactory, scene: Scene) {
        val quadVertexBuffer = resourceFactory.buildVertexBuffer().as2dQuad()
        quadMesh = Mesh(quadVertexBuffer, null)

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

        chestArmorSystem = scene.newSystem(chestArmorMaterial)
        legsArmorSystem = scene.newSystem(legsArmorMaterial)
        bootsArmorSystem = scene.newSystem(bootsArmorMaterial)
        handsArmorSystem = scene.newSystem(handsArmorMaterial)

        player = Player()
        player.chestArmorSystem = chestArmorSystem
        player.chestArmorMaterial = chestArmorMaterial
        player.legsArmorSystem = legsArmorSystem
        player.legsArmorMaterial = legsArmorMaterial
        player.bootsArmorSystem = bootsArmorSystem
        player.bootsArmorMaterial = bootsArmorMaterial
        player.handsArmorSystem = handsArmorSystem
        player.handsArmorMaterial = handsArmorMaterial
        player.equipmentMesh = quadMesh
        playerSystem = scene.newSystem(mobMaterial)
        playerSystem.newEntity(player)
                .attachMoveComponent(0.0f, 0.0f)
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

        attackSystem = scene.newSystem(attackMaterial)
        attackSystem.newEntity(player.attack)
                .attachRenderComponent(attackMaterial, quadMesh)
                .attachAnimatorComponent(Animator())
                .build()

        player.attack.getAnimatorComponent()[0].addAnimation("idle", 0, 0, 0, 0.0f)
        player.attack.getAnimatorComponent()[0].addAnimation("attack", 0, 4, 0, 40.0f)
        player.attack.getRenderComponents()[0].visible = false

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
        level.buildFirstRoom(scene)
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)

        scene.activeCamera = camera

        player.setPosition(level.getFirstTilePos())
        player.getMoveComponent()!!.update(0.0f, 0.0f)
        level.switchCell(resourceFactory, player.cellX, player.cellY)

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

    override fun update(resourceFactory: ResourceFactory, scene: Scene, input: Input) {
        if (player.health <= 0) {
            stateManager.startState("menu")
        }

        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }

        level.update(input)

        if (inventory.visible) {
            inventory.update(input)
        }

        if (player.transform.x + player.cellX*level.width*64 >= level.exitPosition.x*64 - 32 && player.transform.x + player.cellX*level.width*64 <= level.exitPosition.x*64 + 32 &&
                player.transform.y + player.cellY*level.height*64 >= level.exitPosition.y*64 - 32 && player.transform.y + player.cellY*level.height*64 <= level.exitPosition.y*64 + 32) {
            player.currentLevel += 1
            level.build(System.currentTimeMillis(), scene, healthBarSystem, healthMaterial)
            player.setPosition(level.getFirstTilePos())
            level.switchCell(resourceFactory, player.cellX, player.cellY)

            currentLevelText.string = "Current Level: ${player.currentLevel}"
            currentLevelText.x = currentLevelText.w/2.0f
        }
    }
}
