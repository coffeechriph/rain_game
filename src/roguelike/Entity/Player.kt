package roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.components.Animator
import rain.api.components.RenderComponent
import rain.api.entity.Entity
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.scene.Scene
import roguelike.Level.Level
import roguelike.Level.TILE_WIDTH
import java.util.*

const val GLOBAL_ANIMATION_SPEED = 3.0f
class Player : Entity() {
    class InputTimestamp(var direction: Direction, var time: Long)
    var playerMovedCell = false
    lateinit var inventory: Inventory
    lateinit var attack: Attack
    lateinit var renderComponent: RenderComponent
    lateinit var animator: Animator
    lateinit var chestArmor: Entity
    lateinit var legsArmor: Entity
    lateinit var bootsArmor: Entity
    lateinit var handsArmor: Entity

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
    var movingDirection = Direction.DOWN

    private var attackInRowCount = 0
    private var attackTimeout = 0.0f

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
    lateinit var chestArmorMaterial: Material
    lateinit var legsArmorMaterial: Material
    lateinit var handsArmorMaterial: Material
    lateinit var bootsArmorMaterial: Material
    lateinit var equipmentMesh: Mesh

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
            if (targetedEnemy!!.health <= 0) {
                targetedEnemy = null
            }
        }

        if (input.keyState(Input.Key.KEY_TAB) == Input.InputState.PRESSED) {
            targetedEnemy = null

            if (enemies.size > 0) {
                while (targetedEnemy == null) {
                    targetedEnemyIndex++
                    targetedEnemyIndex %= enemies.size
                    var index = 0
                    for (enemy in enemies) {
                        if (index == targetedEnemyIndex) {
                            if (enemy.getRenderComponents()[0].visible) {
                                break
                            }
                        }

                        index++
                    }

                    if (index >= 0 && index < enemies.size) {
                        targetedEnemy = enemies[index]
                    }
                }
            }
        }
    }

    fun damageEnemies(enemies: ArrayList<Enemy>) {
        if (attack.isActive()) {
            for (enemy in enemies) {
                val enemyTransform = enemy.transform
                val attackTransform = attack.transform.worldTransform()
                if (enemyTransform.x + 32.0f >= attackTransform.x - 32.0f && enemyTransform.x - 32.0f <= attackTransform.x + 32.0f &&
                    enemyTransform.y + 32.0f >= attackTransform.y - 32.0f && enemyTransform.y - 32.0f <= attackTransform.y + 32.0f) {
                    enemy.damage(this)
                    break
                }
            }
        }
    }

    fun setPosition(pos: Vector2i) {
        transform.x = pos.x.toFloat()%1280
        transform.y = pos.y.toFloat()%768
        playerMovedCell = true
    }

    override fun init(scene: Scene) {
        renderComponent = getRenderComponents()[0]
        animator = getAnimatorComponent()[0]
        transform.setScale(TILE_WIDTH, TILE_WIDTH)

        animator.addAnimation("idle_down", 0, 0, 0, 0.0f)
        animator.addAnimation("walk_down", 0, 4, 0, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_right", 0, 0, 1, 0.0f)
        animator.addAnimation("walk_right", 0, 4, 1, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_up", 0, 0, 2, 0.0f)
        animator.addAnimation("walk_up", 0, 4, 2, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("idle_left", 0, 0, 3, 0.0f)
        animator.addAnimation("walk_left", 0, 4, 3, GLOBAL_ANIMATION_SPEED)

        animator.addAnimation("attack1_down", 0, 4, 4, GLOBAL_ANIMATION_SPEED * 10.0f)
        animator.addAnimation("attack2_down", 0, 4, 5, GLOBAL_ANIMATION_SPEED * 10.0f)

        animator.addAnimation("attack1_up", 0, 4, 6, GLOBAL_ANIMATION_SPEED * 10.0f)
        animator.addAnimation("attack2_up", 0, 4, 7, GLOBAL_ANIMATION_SPEED * 10.0f)

        animator.addAnimation("attack1_right", 0, 4, 8, GLOBAL_ANIMATION_SPEED * 10.0f)
        animator.addAnimation("attack2_right", 0, 4, 9, GLOBAL_ANIMATION_SPEED * 10.0f)

        animator.addAnimation("attack1_left", 0, 4, 10, GLOBAL_ANIMATION_SPEED * 10.0f)
        animator.addAnimation("attack2_left", 0, 4, 11, GLOBAL_ANIMATION_SPEED * 10.0f)
        animator.setAnimation("idle_down")

        attack = Attack(transform)
        attack.attacker = this

        chestArmor = Entity()
        scene.newEntity(chestArmor)
            .attachRenderComponent(chestArmorMaterial, equipmentMesh)
            .attachAnimatorComponent(animator)
            .build()
        chestArmor.getRenderComponents()[0].visible = false

        legsArmor = Entity()
        scene.newEntity(legsArmor)
            .attachRenderComponent(legsArmorMaterial, equipmentMesh)
            .attachAnimatorComponent(animator)
            .build()
        legsArmor.getRenderComponents()[0].visible = false

        handsArmor = Entity()
        scene.newEntity(handsArmor)
            .attachRenderComponent(handsArmorMaterial, equipmentMesh)
            .attachAnimatorComponent(animator)
            .build()
        handsArmor.getRenderComponents()[0].visible = false

        bootsArmor = Entity()
        scene.newEntity(bootsArmor)
            .attachRenderComponent(bootsArmorMaterial, equipmentMesh)
            .attachAnimatorComponent(animator)
            .build()
        bootsArmor.getRenderComponents()[0].visible = false
    }

    override fun update(scene: Scene, input: Input) {
        val chestTransform = chestArmor.transform
        val legsTransform = legsArmor.transform
        val handsTransform = handsArmor.transform
        val bootsTransform = bootsArmor.transform
        val transform = transform

        chestTransform.x = transform.x
        chestTransform.y = transform.y
        chestTransform.sx = transform.sx
        chestTransform.sy = transform.sy
        chestTransform.z = transform.z + 0.1f

        legsTransform.x = transform.x
        legsTransform.y = transform.y
        legsTransform.sx = transform.sx
        legsTransform.sy = transform.sy
        legsTransform.z = transform.z + 0.2f

        handsTransform.x = transform.x
        handsTransform.y = transform.y
        handsTransform.sx = transform.sx
        handsTransform.sy = transform.sy
        handsTransform.z = transform.z + 0.3f

        bootsTransform.x = transform.x
        bootsTransform.y = transform.y
        bootsTransform.sx = transform.sx
        bootsTransform.sy = transform.sy
        bootsTransform.z = transform.z + 0.4f

        transform.z = 1.0f + transform.y * 0.001f

        if (damageShake > 0.0f) {
            val shake = Math.sin(damageShake.toDouble() * Math.PI * 32).toFloat() * 3

            if (damagePushVelX < 0.0f || damagePushVelX > 0.0f) {
                if (!level.collides(transform.x + damagePushVelX, transform.y, TILE_WIDTH, TILE_WIDTH)) {
                    transform.x += damagePushVelX
                    isStill = false
                }
            }
            else if (damagePushVelY < 0.0f || damagePushVelY > 0.0f) {
                if (!level.collides(transform.x, transform.y + damagePushVelY, TILE_WIDTH, TILE_WIDTH)) {
                    transform.y += damagePushVelY
                    isStill = false
                }
            }

            if (facingDirection == Direction.UP || facingDirection == Direction.DOWN) {
                scene.activeCamera.y = 0.0f
                scene.activeCamera.x = shake * 2
            }
            else {
                scene.activeCamera.x = 0.0f
                scene.activeCamera.y = shake * 2
            }

            val cl = Math.max(Math.min(1.0f,shake),0.0f)
            renderComponent.color.set(cl,cl,cl,cl)
            damageShake -= 0.05f

            if (damageShake <= 0.0f) {
                damageShake = 0.0f
            }
        }
        else {
            renderComponent.color.set(0.0f, 0.0f, 0.0f, 0.0f)
            scene.activeCamera.x = 0.0f
            scene.activeCamera.y = 0.0f
        }

        if (!inventory.visible) {
            setDirectionBasedOnInput(input)
        }

        movement()

        if (input.keyState(Input.Key.KEY_I) == Input.InputState.PRESSED) {
            inventory.visible = !inventory.visible
        }
    }

    private fun movement() {
        if (attackTimeout > 0.0f) {
            return
        }

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

        val speed = 150.0f * (1.0f / 60.0f)
        if (!dodgeMovement) {
            if (inputTimestamps.size <= 0) {
                if (damageShake <= 0.0f) {
                    if (!isStill) {
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
                    movingDirection = Direction.LEFT
                }
                Direction.RIGHT -> {
                    velX += speed
                    animator.setAnimation("walk_right")
                    movingDirection = Direction.RIGHT
                }
                Direction.UP -> {
                    velY -= speed
                    animator.setAnimation("walk_up")
                    movingDirection = Direction.UP
                }
                Direction.DOWN -> {
                    velY += speed
                    animator.setAnimation("walk_down")
                    movingDirection = Direction.DOWN
                }
                Direction.NONE -> {}
            }

            val transform = transform
            if (level.collides(transform.x + velX, transform.y, TILE_WIDTH, TILE_WIDTH)) {
                velX = 0.0f
            }

            if (level.collides(transform.x, transform.y + velY, TILE_WIDTH, TILE_WIDTH)) {
                velY = 0.0f
            }

            transform.x += velX
            transform.y += velY
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

                val transform = transform
                if (level.collides(transform.x + velX, transform.y, TILE_WIDTH, TILE_WIDTH)) {
                    velX = 0.0f
                }

                if (level.collides(transform.x, transform.y + velY, TILE_WIDTH, TILE_WIDTH)) {
                    velY = 0.0f
                }

                transform.x += velX
                transform.y += velY
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

        if (attackTimeout > 0.0f) {
            val moveFactor = 5.0f
            when(facingDirection) {
                Direction.LEFT -> transform.x -= moveFactor
                Direction.RIGHT -> transform.x += moveFactor
                Direction.UP -> transform.y -= moveFactor
                Direction.DOWN -> transform.y += moveFactor
            }

            attackTimeout -= 1.0f / 60.0f / 8.0f
        }

        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            if (attackTimeout <= 0.0f) {
                attackInRowCount += 1
                attackTimeout = 1.0f / 60.0f

                when (attackInRowCount) {
                    1 -> animator.setAnimation("attack1_" + facingDirection.name.toLowerCase())
                    2 -> animator.setAnimation("attack2_" + facingDirection.name.toLowerCase())
                    3 -> animator.setAnimation("attack1_" + facingDirection.name.toLowerCase())
                }
                if (attackInRowCount >= 3) {
                    attackInRowCount = 0
                    attackTimeout = 2.0f / 60.0f
                    damageShake = 1.0f
                }
            }
        }
        else {
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
        val transform = transform
        if (transform.x < 0 && velX < 0.0f) {
            transform.x = 1270.0f
            transform.x += velX
            transform.y += velY

            playerMovedCell = true
        }
        else if (transform.x > 1280 && velX > 0.0f) {
            transform.x = 10.0f
            transform.x += velX
            transform.y += velY

            playerMovedCell = true
        }

        if (transform.y < 0 && velY < 0.0f) {
            transform.y = 758.0f
            transform.x += velX
            transform.y += velY

            playerMovedCell = true
        }
        else if (transform.y > 768 && velY > 0.0f) {
            transform.y = 10.0f
            transform.x += velX
            transform.y += velY

            playerMovedCell = true
        }
    }
}
