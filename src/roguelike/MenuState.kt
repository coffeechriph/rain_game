package roguelike

import org.joml.Vector2i
import org.joml.Vector4f
import rain.api.Input
import rain.api.entity.Entity
import rain.api.gfx.Mesh
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.v2.*
import rain.api.scene.Camera
import rain.api.scene.Scene
import rain.api.scene.SceneManager

class MenuState(sceneManager: SceneManager, resourceFactory: ResourceFactory): Scene(sceneManager, resourceFactory) {
    lateinit var gameScene: GameState
    private var camera = Camera(1000.0f, Vector2i(1280, 768))
    private lateinit var menuContainer: Panel
    private lateinit var startGameButton: Button
    private lateinit var settingsButton: Button
    private lateinit var exitButton: Button
    private lateinit var bannerEntity: Entity
    private lateinit var bannerTexture: Texture2d
    private var selectedButton = 0

    private var buttonsAnimation = 0.0f

    override fun init() {
        sceneManager.activeCamera = camera

        val rowLayout = FillRowLayout()
        rowLayout.componentHeight = 40.0f
        rowLayout.rowPadding = rowLayout.componentHeight
        menuContainer = guiManagerCreatePanel(rowLayout)
        menuContainer.skin.panelStyle.background = false
        menuContainer.skin.buttonStyle.backgroundColor = Vector4f(0.094f, 0.196f, 0.318f, 1.0f)
        menuContainer.skin.buttonStyle.outlineWidth = 1
        menuContainer.skin.buttonStyle.outlineColor = Vector4f(0.194f, 0.296f, 0.418f, 1.0f)
        menuContainer.skin.buttonStyle.textAlign = TextAlign.CENTER
        menuContainer.w = 300.0f
        menuContainer.h = 720.0f
        menuContainer.x = 1280.0f / 2.0f - 150.0f
        menuContainer.y = 300.0f
        menuContainer.moveable = false
        menuContainer.resizable = false

        startGameButton = menuContainer.createButton("New Game")
        settingsButton = menuContainer.createButton("Settings")
        exitButton = menuContainer.createButton("Exit")

        bannerTexture = resourceFactory.buildTexture2d()
                .withName("bannerTexture")
                .fromImageFile("./data/textures/banner.png")
                .withFilter(TextureFilter.NEAREST)
                .build()

        bannerTexture.setTiledTexture(256,64)
        val bannerMaterial = resourceFactory.buildMaterial()
                .withName("bannerMaterial")
                .withVertexShader("./data/shaders/basic.vert.spv")
                .withFragmentShader("./data/shaders/basic.frag.spv")
                .withTexture(bannerTexture)
                .withBatching(false)
                .build()


        val quadVertexBuffer = resourceFactory.buildVertexBuffer().as2dQuad()
        val bannerMesh = Mesh(quadVertexBuffer, null)

        bannerEntity = Entity()
        newEntity(bannerEntity)
                .attachRenderComponent(bannerMaterial, bannerMesh)
                .build()
        bannerEntity.getRenderComponents()[0].addCustomUniformData(0, 1.0f)

        val bannerTransform = bannerEntity.transform
        bannerTransform.sx = 1024.0f
        bannerTransform.sy = 256.0f
        bannerTransform.x = 1280.0f / 2.0f
        bannerTransform.y = 128.0f
        bannerTransform.z = 1.0f
    }

    override fun update(input: Input) {
        when (selectedButton) {
            0 -> {
                startGameButton.active = true
                settingsButton.active = false
                exitButton.active = false
            }
            1 -> {
                settingsButton.active = true
                startGameButton.active = false
                exitButton.active = false
            }
            2 -> {
                exitButton.active = true
                startGameButton.active = false
                settingsButton.active = false
            }
        }

        buttonsAnimation += 4.0f / 60.0f
        if (buttonsAnimation >= Math.PI*2) {
            buttonsAnimation = 0.0f
        }

        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            when (selectedButton) {
                0 -> {
                    sceneManager.unloadScene(this)
                    sceneManager.loadScene(gameScene)
                }
                1 -> {

                }
                2 -> {
                    // TODO: Exit game
                }
            }
        }
        else if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
            if (selectedButton > 0) {
                selectedButton -= 1
            }
        }
        else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
            if (selectedButton < 2) {
                selectedButton += 1
            }
        }
    }
}
