package roguelike.Entity

import org.joml.Vector3f
import rain.api.Input
import rain.api.entity.*
import rain.api.gui.*
import rain.api.gui.Container

class Inventory(val gui: Gui, val player: Player) {
    var visible = false
        set(value) {
            field = value
            container.visible = value
        }

    private val ItemNone = Item(player, ItemType.NONE, "Empty", 0, 0, 0, 0,
            0, false, 0.0f)
    var equippedWeapon: Item = ItemNone
    var equippedHead: Item = ItemNone
    var equippedGloves: Item = ItemNone
    var equippedBoots: Item = ItemNone
    var equippedChest: Item = ItemNone
    var equippedLegs: Item = ItemNone
    private var equippedWeaponText: Text
    private var equippedHeadText: Text
    private var equippedGlovesText: Text
    private var equippedBootsText: Text
    private var equippedChestText: Text
    private var equippedLegsText: Text

    private var itemButtons = ArrayList<ToggleButton>()
    private var dropButton = Button()
    private var items = ArrayList<Item>()
    private lateinit var headerText: Text
    private lateinit var itemDescName: Text
    private lateinit var itemDescType: Text
    private lateinit var itemDescAgility: Text
    private lateinit var itemDescStamina: Text
    private lateinit var itemDescStrength: Text
    private lateinit var itemDescLuck: Text
    private val startX = 1280 / 2.0f - 250.0f
    private val startY = 20.0f
    private var container: Container
    private var statContainer: Container
    private var lastButtonClicked: ToggleButton? = null
    private var selectedItem: Item = ItemNone
    private var selectedItemIndex = 0

    // TODO: These shouldn't be part of the inventory but
    // exist as part of PlayerHud or something
    private var healthText: Text
    private var staminaText: Text
    private var strengthText: Text
    private var agilityText: Text
    private var luckText: Text
    private var levelText: Text
    private var xpText: Text

    init {
        container = gui.newContainer(startX, startY, 500.0f, 500.0f)
        container.background = true
        container.visible = false

        container.skin.backgroundColors["container"] = Vector3f(113.0f / 255.0f, 84.0f / 255.0f, 43.0f / 255.0f)
        container.skin.backgroundColors["button"] = Vector3f(143.0f / 255.0f, 114.0f / 255.0f, 73.0f / 255.0f)
        container.skin.borderColors["button"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)
        container.skin.activeColors["button"] = Vector3f(143.0f / 255.0f * 1.25f, 114.0f / 255.0f * 1.25f, 73.0f / 255.0f * 1.25f)
        container.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

        dropButton.x = 230.0f
        dropButton.y = 142.0f
        dropButton.w = 268.0f
        dropButton.h = 30.0f
        dropButton.text = "Drop"
        container.addComponent(dropButton)

        statContainer = gui.newContainer(0.0f, 768.0f - 140.0f, 100.0f, 140.0f)
        statContainer.visible = true
        statContainer.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

        player.health = player.baseHealth - player.healthDamaged + (player.stamina * 1.5f).toInt()
        healthText = statContainer.addText("Health: ${player.health}", 0.0f, 0.0f)
        staminaText = statContainer.addText("Stamina: ${player.stamina}", 0.0f, 20.0f)
        strengthText = statContainer.addText("Strength: ${player.strength}", 0.0f, 40.0f)
        agilityText = statContainer.addText("Agility: ${player.agility}", 0.0f, 60.0f)
        luckText = statContainer.addText("Luck: ${player.luck}", 0.0f, 80.0f)
        levelText = statContainer.addText("Level: ${player.playerXpLevel}", 0.0f, 100.0f)
        xpText = statContainer.addText("Xp: ${player.xp}/${player.xpUntilNextLevel}", 0.0f, 120.0f)

        equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 175.0f)
        equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 195.0f)
        equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 215.0f)
        equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 235.0f)
        equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 255.0f)
        equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 275.0f)

        if (::headerText.isInitialized) {
            container.removeText(headerText)
        }

        headerText = container.addText("<Inventory: 0>", 0.0f, 0.0f)
        headerText.x = 250.0f - headerText.w/2.0f
    }

    fun addItem(item: Item) {
        val hasItemHigherScoreThanEquipped = hasItemHigherScoreThanEquipped(item)
        val usefulScore = if (hasItemHigherScoreThanEquipped > 0.0f) {" +"} else if (hasItemHigherScoreThanEquipped < 0.0f){" -"} else {""}
        val button = ToggleButton()
        button.text = item.name + usefulScore
        button.x = 0.0f
        button.y = 20.0f + (itemButtons.size*25).toFloat()
        button.w = 230.0f
        button.h = 25.0f
        items.add(item)
        itemButtons.add(button)
        container.addComponent(button)

        if (items.size == 1) {
            updateSelectedItemDesc(0)
        }

        if (::headerText.isInitialized) {
            container.removeText(headerText)
        }

        headerText = container.addText("<Inventory: ${items.size}>", 0.0f, 0.0f)
        headerText.x = 250.0f - headerText.w/2.0f
    }

    private fun hasItemHigherScoreThanEquipped(item: Item): Int {
        val itemScore = (item.agility + item.luck + item.strength + item.stamina).toFloat() / 4.0f
        val compItem = when (item.type) {
            ItemType.BOOTS -> if (equippedBoots != ItemNone) {
                equippedBoots
            }
            else { ItemNone }
            ItemType.CHEST -> if (equippedChest != ItemNone) {
                equippedChest
            }
            else { ItemNone }
            ItemType.GLOVES-> if (equippedGloves!= ItemNone) {
                equippedGloves
            }
            else { ItemNone }
            ItemType.HEAD -> if (equippedHead!= ItemNone) {
                equippedHead
            }
            else { ItemNone }
            ItemType.LEGS -> if (equippedLegs!= ItemNone) {
                equippedLegs
            }
            else { ItemNone }
            ItemType.MELEE -> if (equippedWeapon!= ItemNone) {
                equippedWeapon
            }
            else { ItemNone }
            ItemType.RANGED -> if (equippedWeapon!= ItemNone) {
                equippedWeapon
            }
            else { ItemNone }
            else -> { ItemNone }
        }

        val compItemScore = (compItem.agility + compItem.luck + compItem.strength + compItem.stamina).toFloat() / 4.0f
        val r = itemScore - compItemScore
        return if(r > 0.0f) { 1 } else if (r < 0.0f) { -1 } else { 0 }
    }

    fun update(input: Input) {
        if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
            if (selectedItemIndex > 0) {
                selectedItemIndex -= 1
                for (i in 0 until items.size) {
                    itemButtons[i].active = i == selectedItemIndex
                }
                updateSelectedItemDesc(selectedItemIndex)
            }
        }
        else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
            if (selectedItemIndex < items.size - 1) {
                selectedItemIndex += 1
                for (i in 0 until items.size) {
                    itemButtons[i].active = i == selectedItemIndex
                }
                updateSelectedItemDesc(selectedItemIndex)
            }
        }

        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            if (items.size > 0) {
                when (selectedItem.type) {
                    ItemType.CHEST -> equippedChest = selectedItem
                    ItemType.GLOVES -> equippedGloves = selectedItem
                    ItemType.MELEE -> equippedWeapon = selectedItem
                    ItemType.RANGED -> equippedWeapon = selectedItem
                    ItemType.LEGS -> equippedLegs = selectedItem
                    ItemType.BOOTS -> equippedBoots = selectedItem
                    ItemType.HEAD -> equippedHead = selectedItem
                    ItemType.POTION -> eatConsumable()
                }

                updateEquippedItems()
            }
        }
        else if(input.keyState(Input.Key.KEY_X) == Input.InputState.PRESSED && selectedItemIndex >= 0 && selectedItemIndex < items.size) {
            selectedItem = items[selectedItemIndex]
            if (selectedItem != ItemNone) {
                if (selectedItem == equippedWeapon) {
                    equippedWeapon = ItemNone
                }
                else if (selectedItem == equippedChest) {
                    equippedChest = ItemNone
                }
                else if (selectedItem == equippedLegs) {
                    equippedLegs = ItemNone
                }
                else if (selectedItem == equippedBoots) {
                    equippedBoots = ItemNone
                }
                else if (selectedItem == equippedGloves) {
                    equippedGloves = ItemNone
                }
                else if (selectedItem == equippedHead) {
                    equippedHead = ItemNone
                }

                removeItem()
            }
        }
    }

    private fun removeItem() {
        val index = items.indexOf(selectedItem)
        if (index >= 0) {
            val button = itemButtons[index]
            container.removeComponent(button)
            itemButtons.removeAt(index)
            items.remove(selectedItem)
        }

        selectedItem = ItemNone

        if (selectedItemIndex >= items.size) {
            selectedItemIndex = items.size - 1
        }

        for (i in 0 until itemButtons.size) {
            itemButtons[i].y = 20.0f + i * 25.0f
        }
        container.isDirty = true

        updateEquippedItems()
        updateSelectedItemDesc(selectedItemIndex)
    }

    private fun eatConsumable() {
        if (player.healthDamaged > 0 && selectedItem.instantHealth) {
            player.healPlayer(selectedItem.healthIncrease)
            updateHealthText()
            removeItem()
        }
        else {
            throw NotImplementedError("Slow health increase for consumables are not implemented!")
        }
    }

    private fun updateSelectedItemDesc(index: Int) {
        if (items.size > 0) {
            lastButtonClicked = itemButtons[index]
            itemButtons[index].active = true
            selectedItem = items[index]
        }
        else {
            selectedItem = ItemNone
        }

        if (::itemDescName.isInitialized) {
            container.removeText(itemDescName)
            container.removeText(itemDescType)
            container.removeText(itemDescStamina)
            container.removeText(itemDescStrength)
            container.removeText(itemDescAgility)
            container.removeText(itemDescLuck)
        }

        itemDescName = container.addText("Name: ${selectedItem.name}", 230.0f, 20.0f)
        itemDescType = container.addText("Type: ${selectedItem.type.name}", 230.0f, 40.0f)
        itemDescStamina = container.addText("Stamina: ${selectedItem.stamina}", 230.0f, 60.0f)
        itemDescStrength = container.addText("Strength: ${selectedItem.strength}", 230.0f, 80.0f)
        itemDescAgility = container.addText("Agility: ${selectedItem.agility}", 230.0f, 100.0f)
        itemDescLuck = container.addText("Luck: ${selectedItem.luck}", 230.0f, 120.0f)
    }

    fun updateEquippedItems() {
        for (i in 0 until items.size) {
            val hasItemHigherScoreThanEquipped = hasItemHigherScoreThanEquipped(items[i])
            val usefulScore = if (hasItemHigherScoreThanEquipped > 0.0f) {" +"} else if (hasItemHigherScoreThanEquipped < 0.0f){" -"} else {""}
            itemButtons[i].text = items[i].name + usefulScore
        }

        container.removeText(equippedWeaponText)
        container.removeText(equippedHeadText)
        container.removeText(equippedChestText)
        container.removeText(equippedGlovesText)
        container.removeText(equippedBootsText)
        container.removeText(equippedLegsText)

        statContainer.removeText(staminaText)
        statContainer.removeText(strengthText)
        statContainer.removeText(agilityText)
        statContainer.removeText(luckText)
        statContainer.removeText(levelText)
        statContainer.removeText(xpText)

        equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 175.0f)
        equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 195.0f)
        equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 215.0f)
        equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 235.0f)
        equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 255.0f)
        equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 275.0f)

        player.stamina = player.baseStamina + equippedWeapon.stamina + equippedHead.stamina + equippedChest.stamina + equippedGloves.stamina +
                equippedBoots.stamina + equippedLegs.stamina

        player.strength = player.baseStrength + equippedWeapon.strength + equippedHead.strength + equippedChest.strength + equippedGloves.strength +
                equippedBoots.strength + equippedLegs.strength

        player.agility = player.baseAgility + equippedWeapon.agility + equippedHead.agility + equippedChest.agility + equippedGloves.agility +
                equippedBoots.agility + equippedLegs.agility

        player.luck = player.baseLuck + equippedWeapon.luck + equippedHead.luck + equippedChest.luck + equippedGloves.luck +
                equippedBoots.luck + equippedLegs.luck

        staminaText = statContainer.addText("Stamina: ${player.stamina}", 0.0f, 20.0f)
        strengthText = statContainer.addText("Strength: ${player.strength}", 0.0f, 40.0f)
        agilityText = statContainer.addText("Agility: ${player.agility}", 0.0f, 60.0f)
        luckText = statContainer.addText("Luck: ${player.luck}", 0.0f, 80.0f)
        levelText = statContainer.addText("Level: ${player.playerXpLevel}", 0.0f, 100.0f)
        xpText = statContainer.addText("Xp: ${player.xp}/${player.xpUntilNextLevel}", 0.0f, 120.0f)
        updateHealthText()
    }

    fun updateHealthText() {
        player.health = player.baseHealth - player.healthDamaged + (player.stamina * 1.5f).toInt()
        statContainer.removeText(healthText)
        healthText = statContainer.addText("Health: ${player.health}", 0.0f, 0.0f)
    }
}
