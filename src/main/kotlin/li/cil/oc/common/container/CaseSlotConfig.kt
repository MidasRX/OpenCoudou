package li.cil.oc.common.container

/**
 * Defines what type of component goes in each slot of a computer case, per tier.
 */
object CaseSlotConfig {

    data class SlotDef(val type: String, val maxTier: Int, val x: Int, val y: Int)

    // Slot type constants
    const val CARD = "card"
    const val CPU = "cpu"
    const val MEMORY = "memory"
    const val HDD = "hdd"
    const val EEPROM = "eeprom"
    const val FLOPPY = "floppy"

    /** Tier 1: 7 slots */
    private val TIER1 = listOf(
        SlotDef(CARD,   1,  98, 16),
        SlotDef(CARD,   1,  98, 34),
        SlotDef(CPU,    1, 120, 16),
        SlotDef(MEMORY, 1, 120, 34),
        SlotDef(MEMORY, 1, 142, 16),
        SlotDef(HDD,    1, 142, 34),
        SlotDef(EEPROM, 3,  48, 34),
    )

    /** Tier 2: 8 slots */
    private val TIER2 = listOf(
        SlotDef(CARD,   2,  98, 16),
        SlotDef(CARD,   1,  98, 34),
        SlotDef(CPU,    2, 120, 16),
        SlotDef(MEMORY, 2, 120, 34),
        SlotDef(MEMORY, 2, 120, 52),
        SlotDef(HDD,    2, 142, 16),
        SlotDef(HDD,    1, 142, 34),
        SlotDef(EEPROM, 3,  48, 34),
    )

    /** Tier 3: 10 slots */
    private val TIER3 = listOf(
        SlotDef(CARD,   3,  98, 16),
        SlotDef(CARD,   2,  98, 34),
        SlotDef(CARD,   2,  98, 52),
        SlotDef(CPU,    3, 120, 16),
        SlotDef(MEMORY, 3, 120, 34),
        SlotDef(MEMORY, 3, 120, 52),
        SlotDef(HDD,    3, 142, 16),
        SlotDef(HDD,    2, 142, 34),
        SlotDef(FLOPPY, 1, 142, 52),
        SlotDef(EEPROM, 3,  48, 34),
    )

    /** Creative: 10 slots (all max tier) */
    private val CREATIVE = listOf(
        SlotDef(CARD,   3,  98, 16),
        SlotDef(CARD,   3,  98, 34),
        SlotDef(CARD,   3,  98, 52),
        SlotDef(CPU,    3, 120, 16),
        SlotDef(MEMORY, 3, 120, 34),
        SlotDef(MEMORY, 3, 120, 52),
        SlotDef(HDD,    3, 142, 16),
        SlotDef(HDD,    3, 142, 34),
        SlotDef(FLOPPY, 1, 142, 52),
        SlotDef(EEPROM, 3,  48, 34),
    )

    fun getSlots(tier: Int): List<SlotDef> = when (tier) {
        1 -> TIER1
        2 -> TIER2
        3 -> TIER3
        else -> CREATIVE
    }
}
