package roguelike

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.Mesh
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.v2.*
import rain.api.scene.Camera
import rain.api.scene.Scene

class MenuState(stateManager: StateManager): State(stateManager) {
    private var camera = Camera(1000.0f)
    private lateinit var menuContainer: Panel
    private lateinit var startGameButton: Button
    private lateinit var settingsButton: Button
    private lateinit var exitButton: Button
    private lateinit var bannerEntity: Entity
    private lateinit var bannerEntitySystem: EntitySystem<Entity>
    private lateinit var bannerTexture: Texture2d
    private var selectedButton = 0

    private var buttonsAnimation = 0.0f

    override fun init(resourceFactory: ResourceFactory, scene: Scene, input: Input) {
        scene.activeCamera = camera

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

//        menuContainer.skin.backgroundColors["button"] = Vector3f(143.0f / 255.0f, 114.0f / 255.0f, 73.0f / 255.0f)
//        menuContainer.skin.borderColors["button"] = Vector3f(143.0f / 255.0f * 0.5f, 114.0f / 255.0f * 0.5f, 73.0f / 255.0f * 0.5f)
//        menuContainer.skin.activeColors["button"] = Vector3f(143.0f / 255.0f * 1.25f, 114.0f / 255.0f * 1.25f, 73.0f / 255.0f * 1.25f)
//        menuContainer.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

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
    }

    override fun update(resourceFactory: ResourceFactory, scene: Scene, input: Input, deltaTime: Float) {
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
