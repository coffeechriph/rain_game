package roguelike.Entity

import roguelike.Level.Level
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene
import java.util.*

val GLOBAL_ANIMATION_SPEED = 1.0f
class Player : Entity() {
    class InputTimestamp(var direction: Direction, var time: Long)
    var playerMovedCell = false
    var cellX = 0
        private set
    var cellY = 0
        private set
    lateinit var inventory: Inventory
    lateinit var attack: Attack
    lateinit var transform: Transform
    lateinit var renderComponent: RenderComponent
    lateinit var animator: Animator

    var health = 107
    var stamina = 5
    var strength = 5
    var agility = 5
    var luck = 5
    lateinit var level: Level

    var baseHealth = 100
        private set
    var healthDamaged = 0
        private set
    var baseStamina = 5
        private set
    var baseStrength = 5
        private set
    var baseAgility = 5
        private set
    var baseLuck = 5
        private set

    var currentLevel = 0
    var playerXpLevel = 0
        private set
    var xp = 0
        private set
    var xpUntilNextLevel = 100
        private set
    var facingDirection = Direction.DOWN

    private val inputTimestamps = ArrayList<InputTimestamp>()
    private var lastDirection = Direction.DOWN
    private var dodgeMovement = false
    private var dodgeDirection = Direction.NONE
    private var dodgeTick = 0.0f
    private var dodgeTimeout = 0.0f
    private var stopMovement = 0.0f
    private var damageShake = 0.0f
    private var damagePushVelX = 0.0f
    private var damagePushVelY = 0.0f
    private var isStill = true

    var targetedEnemy: Enemy? = null
        private set
    private var targetedEnemyIndex = -1

    fun damagePlayer(value: Int, fromX: Float, fromY: Float) {
        damageShake = 1.0f
        healthDamaged += value

        val dx = transform.x - fromX
        val dy = transform.y - fromY
        val ln = Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
        damagePushVelX = dx / ln
        damagePushVelY = dy / ln
    }

    fun healPlayer(value: Int) {
        healthDamaged -= value
        if (healthDamaged < 0) {
            healthDamaged = 0
        }
    }

    fun addXp(increase: Int) {
        this.xp += increase
        if (this.xp >= xpUntilNextLevel) {
            this.xp = 0
            playerXpLevel += 1
            xpUntilNextLevel += xpUntilNextLevel

            baseStamina = (baseStamina.toFloat() * 1.3f).toInt()
            baseStrength = (baseStrength.toFloat() * 1.2f).toInt()
            baseAgility = (baseAgility.toFloat() * 1.2f).toInt()
            baseLuck = (baseLuck.toFloat() * 1.1f).toInt()
        }
        inventory.updateEquippedItems()
    }

    fun targetEnemy(enemies: ArrayList<Enemy>, input: Input) {
        if (targetedEnemy != null) {
            if (!targetedEnemy!!.sprite.visible) {
                targetedEnemy = null
            }
        }

        if (input.keyState(Input.Key.KEY_TAB) == Input.InputState.PRESSED) {
            targetedEnemy = null

            if (enemies.size > 0) {
                while (targetedEnemy == null) {
                    targetedEnemyIndex++
                    targetedEnemyIndex %= enemies.size
                    for ((index, enemy) in enemies.withIndex()) {
                        if (index == targetedEnemyIndex) {
                            if (enemy.sprite.visible) {
                                targetedEnemy = enemy
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    fun damageEnemies(enemies: ArrayList<Enemy>) {
        if (attack.isActive()) {
            for (enemy in enemies) {
                if (enemy.transform.x + 32.0f >= attack.transform.x - 32.0f && enemy.transform.x - 32.0f <= attack.transform.x + 32.0f &&
                    enemy.transform.y + 32.0f >= attack.transform.y - 32.0f && enemy.transform.y - 32.0f <= attack.transform.y + 32.0f) {
                    enemy.damage(this)
                    break
                }
            }
        }
    }

    fun setPosition(pos: Vector2i) {
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = pos.x.toFloat()%1280
        transform.y = pos.y.toFloat()%768
        changeMoveComponent(transform, 0.0f, 0.0f)
        playerMovedCell = true
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        renderComponent = system.getRenderComponent(getId())!!
        transform = system.findTransformComponent(getId())!!
        animator = system.findAnimatorComponent(getId())!!
        transform.setScale(64.0f,64.0f)

        animator.addAnimation("idle_down", 0, 0, 0, 0.0f)
        animator.addAnimation("walk_down", 0, 4, 0, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_right", 0, 0, 1, 0.0f)
        animator.addAnimation("walk_right", 0, 4, 1, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_left", 0, 0, 2, 0.0f)
        animator.addAnimation("walk_left", 0, 4, 2, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_up", 0, 0, 3, 0.0f)
        animator.addAnimation("walk_up", 0, 4, 3, GLOBAL_ANIMATION_SPEED)
        animator.setAnimation("idle_down")

        attack = Attack(transform)
        attack.attacker = this
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        transform.z = 1.0f + transform.y * 0.001f

        if (damageShake > 0.0f) {
            val shake = Math.sin(damageShake.toDouble() * Math.PI * 32).toFloat() * 3
            
            if (!level.collides(transform.x + damagePushVelX, transform.y, 64.0f, 64.0f)) {
                changeMoveComponent(transform, damagePushVelX, 0.0f)
                isStill = false
            }

            if (!level.collides(transform.x, transform.y + damagePushVelY, 64.0f, 64.0f)) {
                changeMoveComponent(transform, 0.0f, damagePushVelY)
                isStill = false
            }

            scene.activeCamera.view.translate(0.0f, shake * 2, 0.0f)

            val cl = Math.max(Math.min(1.0f,shake),0.0f)
            renderComponent.color.set(cl,cl,cl,cl)
            damageShake -= 0.05f

            if (damageShake <= 0.0f) {
                damageShake = 0.0f
            }
        }
        else {
            renderComponent.color.set(0.0f, 0.0f, 0.0f, 0.0f)
            scene.activeCamera.view.identity()
        }

        if (!inventory.visible) {
            setDirectionBasedOnInput(input)
            if (attack.isReady()) {
                if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
                    attack.attack(facingDirection)
                    stopMovement = 0.1f
                }
            }
        }

        movement()

        if (input.keyState(Input.Key.KEY_I) == Input.InputState.PRESSED) {
            inventory.visible = !inventory.visible
        }
    }

    private fun movement() {
        if (targetedEnemy == null) {
            facingDirection = lastDirection
        }

        if (inputTimestamps.size <= 0) {
            when (facingDirection) {
                Direction.LEFT  -> animator.setAnimation("idle_left")
                Direction.RIGHT -> animator.setAnimation("idle_right")
                Direction.UP    -> animator.setAnimation("idle_up")
                Direction.DOWN  -> animator.setAnimation("idle_down")
            }
        }

        if (dodgeTimeout > 0.0f) {
            dodgeTimeout -= 0.1f
        }
        else {
            dodgeTimeout = 0.0f
        }

        if (stopMovement > 0.0f) {
            stopMovement -= (1.0f / 60.0f)
            return
        }
        else {
            stopMovement = 0.0f
        }

        val speed = 100.0f * (1.0f / 60.0f)
        if (!dodgeMovement) {
            if (inputTimestamps.size <= 0) {
                if (damageShake <= 0.0f) {
                    if (!isStill) {
                        changeMoveComponent(transform, 0.0f, 0.0f)
                        isStill = true
                    }
                }
                return
            }

            var velX = 0.0f
            var velY = 0.0f
            lastDirection = (inputTimestamps.sortedBy { i -> i.time })[0].direction
            when (lastDirection) {
                Direction.LEFT -> {
                    velX -= speed
                    animator.setAnimation("walk_left")
                }
                Direction.RIGHT -> {
                    velX += speed
                    animator.setAnimation("walk_right")
                }
                Direction.UP -> {
                    velY -= speed
                    animator.setAnimation("walk_up")
                }
                Direction.DOWN -> {
                    velY += speed
                    animator.setAnimation("walk_down")
                }
                Direction.NONE -> {}
            }

            if (level.collides(transform.x + velX, transform.y, 32.0f, 32.0f)) {
                velX = 0.0f
            }

            if (level.collides(transform.x, transform.y + velY, 32.0f, 32.0f)) {
                velY = 0.0f
            }

            changeMoveComponent(transform, velX, velY)
            isStill = false
            keepPlayerWithinBorder(velX, velY)
        }
        else {
            // Delay before movement starts
            if (dodgeTick > 0.32f) {
                var velX = 0.0f
                var velY = 0.0f
                when (dodgeDirection) {
                    Direction.LEFT -> velX -= speed * 2
                    Direction.RIGHT -> velX += speed * 2
                    Direction.UP -> velY -= speed * 2
                    Direction.DOWN -> velY += speed * 2
                }

                if (level.collides(transform.x + velX, transform.y, 32.0f, 32.0f)) {
                    velX = 0.0f
                }

                if (level.collides(transform.x, transform.y + velY, 32.0f, 32.0f)) {
                    velY = 0.0f
                }

                changeMoveComponent(transform, velX, velY)
                isStill = false
                keepPlayerWithinBorder(velX, velY)
            }

            dodgeTick += (10.0f / 60.0f)
            if (dodgeTick >= 3.0f) {
                dodgeMovement = false
                dodgeTick = 0.0f
                dodgeDirection = Direction.NONE
                dodgeTimeout = 3.0f
            }
        }
    }

    private fun setDirectionBasedOnInput(input: Input) {
        if (!dodgeMovement && dodgeTimeout <= 0.0f) {
            if (input.keyState(Input.Key.KEY_Q) == Input.InputState.PRESSED) {
                dodgeMovement = true

                if (facingDirection == Direction.UP) {
                    dodgeDirection = Direction.LEFT
                }
                else if (facingDirection == Direction.DOWN) {
                    dodgeDirection = Direction.LEFT
                }
                else if (facingDirection == Direction.LEFT) {
                    dodgeDirection = Direction.UP
                }
                else if (facingDirection == Direction.RIGHT) {
                    dodgeDirection = Direction.UP
                }
            }
            else if (input.keyState(Input.Key.KEY_E) == Input.InputState.PRESSED) {
                dodgeMovement = true
                if (facingDirection == Direction.UP) {
                    dodgeDirection = Direction.RIGHT
                }
                else if (facingDirection == Direction.DOWN) {
                    dodgeDirection = Direction.RIGHT
                }
                else if (facingDirection == Direction.LEFT) {
                    dodgeDirection = Direction.DOWN
                }
                else if (facingDirection == Direction.RIGHT) {
                    dodgeDirection = Direction.DOWN
                }
            }
        }

        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.LEFT, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.LEFT)
        }

        if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.RIGHT, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.RIGHT)
        }

        if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.UP, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_UP) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.UP)
        }

        if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.DOWN, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.DOWN)
        }
    }

    private fun removeInputDirection(dir: Direction) {
        for (i in inputTimestamps) {
            if (i.direction == dir) {
                inputTimestamps.remove(i)
                break
            }
        }
    }

    // TODO: This method uses constant window dimensions
    private fun keepPlayerWithinBorder(velX: Float, velY: Float) {
        if (transform.x < 0 && velX < 0.0f) {
            if (cellX > 0) {
                transform.x = 1270.0f
                changeMoveComponent(transform, velX, velY)

                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (transform.x > 1280 && velX > 0.0f) {
            if (cellX < level.maxCellX) {
                transform.x = 10.0f
                changeMoveComponent(transform, velX, velY)

                playerMovedCell = true
                cellX += 1
            }
        }

        if (transform.y < 0 && velY < 0.0f) {
            if (cellY > 0) {
                transform.y = 758.0f
                changeMoveComponent(transform, velX, velY)

                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (transform.y > 768 && velY > 0.0f) {
            if (cellY < level.maxCellY) {
                transform.y = 10.0f
                changeMoveComponent(transform, velX, velY)

                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
