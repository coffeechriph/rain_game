package roguelike

import rain.Rain

class Roguelike: Rain() {
    lateinit var gameState: GameState
    lateinit var menuState: MenuState

    // TODO: We can still load resources here which will be cleared once we switch state!!
    // This is "bad practice" but we should try to prevent that
    override fun init() {
        showMouse = false
        gameState = GameState(stateManager)
        stateManager.states.put("game", gameState)

        menuState = MenuState(stateManager)
        stateManager.states.put("menu", menuState)

        stateManager.startState("menu")
    }
}
