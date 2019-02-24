package roguelike

import org.joml.Vector2f
import org.joml.Vector3f
import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.Mesh
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.Container
import rain.api.gui.Gui
import rain.api.gui.ToggleButton
import rain.api.gui.v2.*
import rain.api.scene.Camera
import rain.api.scene.Scene

class MenuState(stateManager: StateManager): State(stateManager) {
    private var camera = Camera(Vector2f(0.0f, 40.0f))
    private lateinit var menuContainer: Container
    private lateinit var startGameButton: ToggleButton
    private lateinit var settingsButton: ToggleButton
    private lateinit var exitButton: ToggleButton
    private lateinit var bannerEntity: Entity
    private lateinit var bannerEntitySystem: EntitySystem<Entity>
    private lateinit var bannerTexture: Texture2d
    private var selectedButton = 0

    private var buttonsAnimation = 0.0f

    lateinit var button: Button
    lateinit var slider: Slider

    override fun init(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
        scene.activeCamera = camera

        menuContainer = gui.newContainer(0.0f, 0.0f, 1280.0f, 768.0f)
        menuContainer.skin.backgroundColors["button"] = Vector3f(143.0f / 255.0f, 114.0f / 255.0f, 73.0f / 255.0f)
        menuContainer.skin.borderColors["button"] = Vector3f(143.0f / 255.0f * 0.5f, 114.0f / 255.0f * 0.5f, 73.0f / 255.0f * 0.5f)
        menuContainer.skin.activeColors["button"] = Vector3f(143.0f / 255.0f * 1.25f, 114.0f / 255.0f * 1.25f, 73.0f / 255.0f * 1.25f)
        menuContainer.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

        startGameButton = ToggleButton()
        startGameButton.x = 1280.0f / 2.0f - 70.0f
        startGameButton.y = 310.0f
        startGameButton.w = 200.0f
        startGameButton.h = 60.0f
        startGameButton.text = "New Game"
        menuContainer.addComponent(startGameButton)

        settingsButton = ToggleButton()
        settingsButton.x = 1280.0f / 2.0f - 100.0f
        settingsButton.y = 370.0f
        settingsButton.w = 200.0f
        settingsButton.h = 60.0f
        settingsButton.text = "Settings"
        menuContainer.addComponent(settingsButton)

        exitButton= ToggleButton()
        exitButton.x = 1280.0f / 2.0f - 100.0f
        exitButton.y = 460.0f
        exitButton.w = 200.0f
        exitButton.h = 60.0f
        exitButton.text = "Exit"
        menuContainer.addComponent(exitButton)

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

        bannerEntity = Entity()

        val quadVertexBuffer = resourceFactory.buildVertexBuffer().as2dQuad()
        val bannerMesh = Mesh(quadVertexBuffer, null)

        bannerEntitySystem = scene.newSystem(bannerMaterial)
        bannerEntitySystem.newEntity(bannerEntity)
                .attachRenderComponent(bannerMaterial, bannerMesh)
                .build()
        bannerEntity.getRenderComponents()[0].addCustomUniformData(0, 1.0f)

        val bannerTransform = bannerEntity.transform
        bannerTransform.sx = 1024.0f
        bannerTransform.sy = 256.0f
        bannerTransform.x = 1280.0f / 2.0f
        bannerTransform.y = 128.0f
        bannerTransform.z = 1.0f

        val gridLayout = FillRowLayout()
        gridLayout.componentHeight = 30.0f
        gridLayout.componentsPerRow = 2
        val panel = guiManagerCreatePanel(gridLayout)
        panel.w = 300.0f
        panel.h = 300.0f
        panel.x = 200.0f
        panel.y = 200.0f

        button = panel.createButton("Reset Slider")
        slider = panel.createSlider(50, 0, 100)
    }

    override fun update(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input, deltaTime: Float) {
        val scale = Math.sin(buttonsAnimation.toDouble()).toFloat() * 7.0f
        when (selectedButton) {
            0 -> {
                startGameButton.w = 200.0f + scale
                startGameButton.h = 60.0f + scale
                startGameButton.x = 1280.0f / 2.0f
                startGameButton.y = 280.0f
                startGameButton.outlineWidth = 4
                startGameButton.active = true
                settingsButton.active = false
                exitButton.active = false

                settingsButton.x = 1280.0f / 2.0f
                settingsButton.y = 370.0f
                settingsButton.w = 200.0f
                settingsButton.h = 60.0f
                settingsButton.outlineWidth = 4

                exitButton.x = 1280.0f / 2.0f
                exitButton.y = 460.0f
                exitButton.w = 200.0f
                exitButton.h = 60.0f
                exitButton.outlineWidth = 4
            }
            1 -> {
                settingsButton.w = 200.0f + scale
                settingsButton.h = 60.0f + scale
                settingsButton.x = 1280.0f / 2.0f
                settingsButton.y = 370.0f
                settingsButton.active = true
                startGameButton.active = false
                exitButton.active = false

                startGameButton.x = 1280.0f / 2.0f
                startGameButton.y = 280.0f
                startGameButton.w = 200.0f
                startGameButton.h = 60.0f

                exitButton.x = 1280.0f / 2.0f
                exitButton.y = 460.0f
                exitButton.w = 200.0f
                exitButton.h = 60.0f
            }
            2 -> {
                exitButton.w = 200.0f + scale
                exitButton.h = 60.0f + scale
                exitButton.x = 1280.0f / 2.0f
                exitButton.y = 460.0f
                exitButton.active = true
                startGameButton.active = false
                settingsButton.active = false

                startGameButton.x = 1280.0f / 2.0f
                startGameButton.y = 280.0f
                startGameButton.w = 200.0f
                startGameButton.h = 60.0f

                settingsButton.x = 1280.0f / 2.0f
                settingsButton.y = 370.0f
                settingsButton.w = 200.0f
                settingsButton.h = 60.0f
            }
        }

        buttonsAnimation += 4.0f / 60.0f
        if (buttonsAnimation >= Math.PI*2) {
            buttonsAnimation = 0.0f
        }
        menuContainer.isDirty = true

        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            when (selectedButton) {
                0 -> {
                    stateManager.startState("game")
                }
                1 -> {

                }
                2 -> {
                    stateManager.exitState()
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
