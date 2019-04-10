package roguelike

import rain.Rain

class Roguelike: Rain() {
    private lateinit var menuScene: MenuState
    private lateinit var gameScene: GameState

    override fun init() {
        showMouse = true

        menuScene = MenuState(sceneManager, resourceFactory)
        gameScene = GameState(sceneManager, resourceFactory)

        menuScene.gameScene = gameScene
        gameScene.menuScene = menuScene
        sceneManager.loadScene(menuScene)
    }
}
