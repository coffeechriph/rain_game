package roguelike.Entity

import org.joml.Random
import org.joml.Vector3f
import org.joml.Vector4f
import rain.api.Input
import rain.api.gui.v2.*

class Inventory(val player: Player) {
    var visible = false
        set(value) {
            field = value
            panel.visible = visible
            statPanel.visible = visible
        }

    private val ItemNone = Item(player, ItemType.NONE, "Empty", 0, 0, 0, 0,
            0, false, 0.0f)
    var equippedWeapon: Item = ItemNone
    var equippedHead: Item = ItemNone
    var equippedGloves: Item = ItemNone
    var equippedBoots: Item = ItemNone
    var equippedChest: Item = ItemNone
    var equippedLegs: Item = ItemNone
    private var equippedWeaponText: Label
    private var equippedHeadText: Label
    private var equippedGlovesText: Label
    private var equippedBootsText: Label
    private var equippedChestText: Label
    private var equippedLegsText: Label

    private var itemButtons = ArrayList<ToggleButton>()
    private var dropButton: Button
    private var items = ArrayList<Item>()
    private var headerText: Label
    private var itemDescName: Label
    private var itemDescType: Label
    private var itemDescAgility: Label
    private var itemDescStamina: Label
    private var itemDescStrength: Label
    private var itemDescLuck: Label
    private val startX = 1280 / 2.0f - 250.0f
    private val startY = 20.0f
    private var panel: Panel
    private var statPanel: Panel
    private var lastButtonClicked: ToggleButton? = null
    private var selectedItem: Item = ItemNone
    private var selectedItemIndex = 0

    // TODO: These shouldn't be part of the inventory but
    // exist as part of PlayerHud or something
    private var healthText: Label
    private var staminaText: Label
    private var strengthText: Label
    private var agilityText: Label
    private var luckText: Label
    private var levelText: Label
    private var xpText: Label

    init {
        val panelLayout = FillRowLayout()
        panelLayout.componentsPerRow = 1

        panel = guiManagerCreatePanel(panelLayout)
        panel.skin.labelStyle.textAlign = TextAlign.CENTER
        panel.skin.panelStyle.outlineWidth = 1
        panel.skin.panelStyle.outlineColor = Vector4f(0.094f, 0.196f, 0.318f, 1.0f)
        panel.skin.buttonStyle.backgroundColor = Vector4f(0.094f, 0.196f, 0.318f, 1.0f)
        panel.skin.buttonStyle.activeColor = Vector4f(0.194f, 0.296f, 0.418f, 1.0f)
        panel.x = startX
        panel.y = startY
        panel.w = 300.0f
        panel.h = 500.0f
        panel.visible = false

//        container.skin.backgroundColors["container"] = Vector3f(113.0f / 255.0f, 84.0f / 255.0f, 43.0f / 255.0f)
//        container.skin.backgroundColors["button"] = Vector3f(143.0f / 255.0f, 114.0f / 255.0f, 73.0f / 255.0f)
//        container.skin.borderColors["button"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)
//        container.skin.activeColors["button"] = Vector3f(143.0f / 255.0f * 1.25f, 114.0f / 255.0f * 1.25f, 73.0f / 255.0f * 1.25f)
//        container.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

        dropButton = panel.createButton("Drop")

        statPanel = guiManagerCreatePanel(FillRowLayout())
        statPanel.skin.labelStyle.textAlign = TextAlign.CENTER
        statPanel.visible = false
        statPanel.w = startX + 200.0f
        statPanel.y = startY
        statPanel.w = 200.0f
        statPanel.h = 500.0f
        statPanel.moveable = false
        statPanel.resizable = false

//        statContainer.skin.foregroundColors["text"] = Vector3f(240.0f / 255.0f, 207.0f / 255.0f, 117.0f / 255.0f)

        headerText = panel.createLabel("<Inventory: 0>")
        player.health = player.baseHealth - player.healthDamaged + (player.stamina * 1.5f).toInt()

        itemDescName = panel.createLabel("Name: ${selectedItem.name}")
        itemDescType = panel.createLabel("Type: ${selectedItem.type.name}")
        itemDescStamina = panel.createLabel("Stamina: ${selectedItem.stamina}")
        itemDescStrength = panel.createLabel("Strength: ${selectedItem.strength}")
        itemDescAgility = panel.createLabel("Agility: ${selectedItem.agility}")
        itemDescLuck = panel.createLabel("Luck: ${selectedItem.luck}")

        healthText = statPanel.createLabel("Health: ${player.health}")
        staminaText = statPanel.createLabel("Stamina: ${player.stamina}")
        strengthText = statPanel.createLabel("Strength: ${player.strength}")
        agilityText = statPanel.createLabel("Agility: ${player.agility}")
        luckText = statPanel.createLabel("Luck: ${player.luck}")
        levelText = statPanel.createLabel("Level: ${player.playerXpLevel}")
        xpText = statPanel.createLabel("Xp: ${player.xp}/${player.xpUntilNextLevel}")

        equippedWeaponText = statPanel.createLabel("Weapon: ${equippedWeapon.name}")
        equippedHeadText = statPanel.createLabel("Head: ${equippedHead.name}")
        equippedChestText = statPanel.createLabel("Chest: ${equippedChest.name}")
        equippedGlovesText = statPanel.createLabel("Gloves: ${equippedGloves.name}")
        equippedBootsText = statPanel.createLabel("Boots: ${equippedBoots.name}")
        equippedLegsText = statPanel.createLabel("Legs: ${equippedLegs.name}")
    }

    fun addItem(item: Item) {
        val hasItemHigherScoreThanEquipped = hasItemHigherScoreThanEquipped(item)
        val usefulScore = if (hasItemHigherScoreThanEquipped > 0.0f) {" +"} else if (hasItemHigherScoreThanEquipped < 0.0f){" -"} else {""}
        val button = panel.createToggleButton(item.name + usefulScore)
        items.add(item)
        itemButtons.add(button)

        if (items.size == 1) {
            updateSelectedItemDesc(0)
        }

        headerText.string = "<Inventory: ${items.size}>"
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
        statPanel.x = panel.x + panel.w
        statPanel.y = panel.y
        statPanel.h = panel.h

        if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
            if (selectedItemIndex > 0) {
                selectedItemIndex -= 1
                for (i in 0 until items.size) {
                    itemButtons[i].checked = i == selectedItemIndex
                }
                updateSelectedItemDesc(selectedItemIndex)
            }
        }
        else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
            if (selectedItemIndex < items.size - 1) {
                selectedItemIndex += 1
                for (i in 0 until items.size) {
                    itemButtons[i].checked = i == selectedItemIndex
                }
                updateSelectedItemDesc(selectedItemIndex)
            }
        }

        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            val randomEquipmentOffset = Random(System.currentTimeMillis()).nextInt(2)*4.0f
            if (items.size > 0) {
                when (selectedItem.type) {
                    ItemType.CHEST -> { equippedChest = selectedItem; player.chestArmor.getRenderComponents()[0].addCustomUniformData(0, randomEquipmentOffset)}
                    ItemType.GLOVES -> { equippedGloves = selectedItem; player.handsArmor.getRenderComponents()[0].addCustomUniformData(0, randomEquipmentOffset) }
                    ItemType.LEGS -> { equippedLegs = selectedItem; player.legsArmor.getRenderComponents()[0].addCustomUniformData(0, randomEquipmentOffset) }
                    ItemType.BOOTS -> { equippedBoots = selectedItem; player.bootsArmor.getRenderComponents()[0].addCustomUniformData(0, randomEquipmentOffset) }
                    ItemType.MELEE -> equippedWeapon = selectedItem
                    ItemType.RANGED -> equippedWeapon = selectedItem
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
            panel.removeComponent(button)
            itemButtons.removeAt(index)
            items.remove(selectedItem)
        }

        selectedItem = ItemNone

        if (selectedItemIndex >= items.size) {
            selectedItemIndex = items.size - 1
        }

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
            itemButtons[index].checked = true
            selectedItem = items[index]
        }
        else {
            selectedItem = ItemNone
        }

        itemDescName.string = "Name: ${selectedItem.name}"
        itemDescType.string = "Type: ${selectedItem.type.name}"
        itemDescStamina.string = "Stamina: ${selectedItem.stamina}"
        itemDescStrength.string = "Strength: ${selectedItem.strength}"
        itemDescAgility.string = "Agility: ${selectedItem.agility}"
        itemDescLuck.string = "Luck: ${selectedItem.luck}"
    }

    fun updateEquippedItems() {
        for (i in 0 until items.size) {
            val hasItemHigherScoreThanEquipped = hasItemHigherScoreThanEquipped(items[i])
            val usefulScore = if (hasItemHigherScoreThanEquipped > 0.0f) {" +"} else if (hasItemHigherScoreThanEquipped < 0.0f){" -"} else {""}
            itemButtons[i].string = items[i].name + usefulScore
        }

        equippedWeaponText.string = "Weapon: ${equippedWeapon.name}"
        equippedHeadText.string = "Head: ${equippedHead.name}"
        equippedChestText.string = "Chest: ${equippedChest.name}"
        equippedGlovesText.string = "Gloves: ${equippedGloves.name}"
        equippedBootsText.string = "Boots: ${equippedBoots.name}"
        equippedLegsText.string = "Legs: ${equippedLegs.name}"

        player.stamina = player.baseStamina + equippedWeapon.stamina + equippedHead.stamina + equippedChest.stamina + equippedGloves.stamina +
                equippedBoots.stamina + equippedLegs.stamina

        player.strength = player.baseStrength + equippedWeapon.strength + equippedHead.strength + equippedChest.strength + equippedGloves.strength +
                equippedBoots.strength + equippedLegs.strength

        player.agility = player.baseAgility + equippedWeapon.agility + equippedHead.agility + equippedChest.agility + equippedGloves.agility +
                equippedBoots.agility + equippedLegs.agility

        player.luck = player.baseLuck + equippedWeapon.luck + equippedHead.luck + equippedChest.luck + equippedGloves.luck +
                equippedBoots.luck + equippedLegs.luck

        staminaText.string = "Stamina: ${player.stamina}"
        strengthText.string = "Strength: ${player.strength}"
        agilityText.string = "Agility: ${player.agility}"
        luckText.string = "Luck: ${player.luck}"
        levelText.string = "Level: ${player.playerXpLevel}"
        xpText.string = "Xp: ${player.xp}/${player.xpUntilNextLevel}"
        updateHealthText()

        player.legsArmor.getRenderComponents()[0].visible = equippedLegs != ItemNone
        player.handsArmor.getRenderComponents()[0].visible = equippedGloves != ItemNone
        player.chestArmor.getRenderComponents()[0].visible = equippedChest != ItemNone
        player.bootsArmor.getRenderComponents()[0].visible = equippedBoots != ItemNone
    }

    fun updateHealthText() {
        player.health = player.baseHealth - player.healthDamaged + (player.stamina * 1.5f).toInt()
        healthText.string = "Health: ${player.health}"
    }
}
