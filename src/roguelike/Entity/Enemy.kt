package roguelike.Entity

import org.joml.Random
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import rain.api.Input
import rain.api.components.Animator
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene
import kotlin.math.sin

open class Enemy(private val random: Random, val player: Player) : Entity() {
    var cellX = 0
        private set
    var cellY = 0
        private set
    private var attackAnimation = 0.0f
    private var wasAttacked = false

    var strength = 1
    var agility = 1
    var health = 100

    var strengthFactor = 1.0f
    var agilityFactor = 1.0f
    var healthFactor = 1.0f

    var healthBar = HealthBar()
    var traversing = false
    var path = ArrayList<Vector2i>()
    var pathIndex = 0
    var traverseSleep = 0L
    var direction: Direction = Direction.NONE
    var lastPlayerAngle = 0.0f
    lateinit var animator: Animator

    protected var attackTimeoutValue = 30
    protected var attackTimeout = 0
    protected var attackSpeed = 0.01f
    var walkingSpeedFactor = 1.0f
        protected set
    var pushBack = 0
    var pushBackImmune = false
    var pushDirection = Vector2i(0,0)
    var lastX = -1
    var lastY = -1

    // Attack
    var attacking = false
        private set
    private var prepareAttack = 0.0f
    private var attackArea = Vector2f()
    var attackAreaVisual = Entity()
    lateinit var attackAreaVisualRenderComponent: RenderComponent
    lateinit var attackAreaVisualTransform: Transform

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        val transform = getTransform()!!
        transform.setScale(80.0f, 80.0f)
        transform.z = 1.0f + pos.y%768 * 0.001f
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = pos.x.toFloat()%1280
        transform.y = pos.y.toFloat()%768
    }

    fun damage(player: Player) {
        if (!wasAttacked) {
            wasAttacked = true
            attackAnimation = 0.0f

            val baseDamage = player.strength * 1.5f
            val critChange = Math.min((0.05f * player.agility - 0.005f * agility), 0.0f)
            val critted = random.nextFloat() < critChange
            var damage = baseDamage * random.nextFloat()
            if (critted) {
                damage *= random.nextInt(4) + 1.5f
            }

            health -= damage.toInt()
        }
    }

    fun attack() {
        if (attackTimeout == 0) {
            attacking = true
            attackTimeout = attackTimeoutValue

            val transform = getTransform()!!
            var px = (player.getTransform().x - transform.x)
            var py = (player.getTransform().y - transform.y)
            val ln = Math.sqrt((px*px+py*py).toDouble()).toFloat()
            px /= ln
            py /= ln
            val inCharge = Math.abs(px) > Math.abs(py)
            if (inCharge) {
                direction = if(px > 0){
                    Direction.RIGHT
                } else {
                    Direction.LEFT
                }
            }
            else {
                direction = if(py > 0) {
                    Direction.DOWN
                } else {
                    Direction.UP
                }
            }

            when (direction) {
                Direction.LEFT -> attackArea = Vector2f(-128.0f, 48.0f)
                Direction.RIGHT -> attackArea = Vector2f(128.0f, 48.0f)
                Direction.UP -> attackArea = Vector2f(48.0f, -128.0f)
                Direction.DOWN -> attackArea = Vector2f(48.0f, 128.0f)
            }

            attackAreaVisualTransform.x = transform.x
            attackAreaVisualTransform.y = transform.y
            attackAreaVisualTransform.z = 1.1f
            attackAreaVisualRenderComponent.visible = true

            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                attackAreaVisualTransform.sy = attackArea.y
                attackAreaVisualTransform.sx = 0.0f
            }
            else if (direction == Direction.UP || direction == Direction.DOWN) {
                attackAreaVisualTransform.sy = 0.0f
                attackAreaVisualTransform.sx = attackArea.x
            }
        }
    }

    protected fun handleDamage(transform: Transform) {
        if (wasAttacked) {
            transform.y = transform.y + sin(attackAnimation * 100.0f) * 2.0f
            attackAnimation += 0.075f

            if (attackAnimation >= 1.0f) {
                wasAttacked = false
                attackAnimation = 0.0f
            }
        }
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        animator = getAnimatorComponent()!![0]
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (health <= 0) {
            attacking = false
        }

        if (attacking) {
            if (prepareAttack < 1.0f) {
                val transform = getTransform()!!
                if (direction == Direction.LEFT) {
                    attackAreaVisualTransform.sx = attackArea.x * prepareAttack
                    attackAreaVisualTransform.x = transform.x + (attackArea.x * prepareAttack) * 0.5f
                }
                else if (direction == Direction.RIGHT) {
                    attackAreaVisualTransform.sx = attackArea.x * prepareAttack
                    attackAreaVisualTransform.x = transform.x + (attackArea.x * prepareAttack) * 0.5f
                }
                else if (direction == Direction.UP) {
                    attackAreaVisualTransform.sy = attackArea.y * prepareAttack
                    attackAreaVisualTransform.y = transform.y + (attackArea.y * prepareAttack) * 0.5f
                }
                else if (direction == Direction.DOWN) {
                    attackAreaVisualTransform.sy = attackArea.y * prepareAttack
                    attackAreaVisualTransform.y = transform.y + (attackArea.y * prepareAttack) * 0.5f
                }

                prepareAttack += attackSpeed
            }
            else {
                prepareAttack = 0.0f
                attacking = false

                val baseDamage = strength * 3.0f
                val critChange = Math.min((0.075f * agility) - (0.005f * player.agility), 0.0f)
                val critted = random.nextFloat() < critChange
                var damage = baseDamage * (random.nextFloat() + 0.1f)
                if (critted) {
                    damage *= random.nextInt(4) + 2.0f
                }

                val transform = getTransform()!!
                val area = when (direction) {
                    Direction.LEFT -> Vector4f(transform.x - 128.0f - 32.0f, transform.y - 24.0f, 128.0f, 48.0f)
                    Direction.RIGHT -> Vector4f(transform.x + 32.0f, transform.y - 24.0f, 128.0f, 48.0f)
                    Direction.UP -> Vector4f(transform.x - 24.0f, transform.y - 128.0f - 32.0f, 48.0f, 128.0f)
                    Direction.DOWN -> Vector4f(transform.x - 24.0f, transform.y + 32.0f, 48.0f, 128.0f)
                    Direction.NONE -> {Vector4f(0.0f, 0.0f, 0.0f, 0.0f)}
                }

                // Attacking in Y direction
                if (player.getTransform().x + 12.0f >= area.x && player.getTransform().x - 12.0f <= area.x + area.z
                 && player.getTransform().y + 12.0f >= area.y && player.getTransform().y - 12.0f <= area.y + area.w) {
                    player.damagePlayer(Math.max(1, damage.toInt()), transform.x, transform.y)
                    player.inventory.updateHealthText()
                }
            }
        }
        else {
            attackAreaVisualRenderComponent.visible = false
            if (attackTimeout > 0) {
                attackTimeout -= 1
            }
        }
    }
}
