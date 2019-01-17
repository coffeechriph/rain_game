package roguelike

import rain.api.Api

fun main(args: Array<String>) {
    // Settings for release build
    /*ENABLE_LOGGING = false
    Configuration.DISABLE_CHECKS.set(true)
    Configuration.DISABLE_FUNCTION_CHECKS.set(true)
    Configuration.GLFW_CHECK_THREAD0.set(false)
*/
    val app = Roguelike()
    app.create(1280,768,"Hello Rain!", Api.VULKAN)
    app.run()
}
