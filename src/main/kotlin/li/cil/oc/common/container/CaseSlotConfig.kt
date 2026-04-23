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

    /** Tier 1: 7 slots — 1 card, 1 CPU, 2 memory, 1 HDD, 1 EEPROM + power button */
    private val TIER1 = listOf(
        SlotDef(CPU,    1,  98, 18),
        SlotDef(MEMORY, 1, 116, 18),
        SlotDef(MEMORY, 1, 134, 18),
        SlotDef(CARD,   1,  98, 36),
        SlotDef(HDD,    1, 116, 36),
        SlotDef(EEPROM, 3, 134, 36),
    )

    /** Tier 2: 9 slots */
    private val TIER2 = listOf(
        SlotDef(CPU,    2,  98, 18),
        SlotDef(MEMORY, 2, 116, 18),
        SlotDef(MEMORY, 2, 134, 18),
        SlotDef(CARD,   2,  98, 36),
        SlotDef(CARD,   1, 116, 36),
        SlotDef(HDD,    2, 134, 36),
        SlotDef(HDD,    1,  98, 54),
        SlotDef(EEPROM, 3, 116, 54),
    )

    /** Tier 3: 10 slots */
    private val TIER3 = listOf(
        SlotDef(CPU,    3,  98, 18),
        SlotDef(MEMORY, 3, 116, 18),
        SlotDef(MEMORY, 3, 134, 18),
        SlotDef(CARD,   3,  98, 36),
        SlotDef(CARD,   2, 116, 36),
        SlotDef(CARD,   2, 134, 36),
        SlotDef(HDD,    3,  98, 54),
        SlotDef(HDD,    2, 116, 54),
        SlotDef(FLOPPY, 1, 134, 54),
        SlotDef(EEPROM, 3, 152, 18),
    )

    /** Creative: 10 slots (all max tier) */
    private val CREATIVE = listOf(
        SlotDef(CPU,    3,  98, 18),
        SlotDef(MEMORY, 3, 116, 18),
        SlotDef(MEMORY, 3, 134, 18),
        SlotDef(CARD,   3,  98, 36),
        SlotDef(CARD,   3, 116, 36),
        SlotDef(CARD,   3, 134, 36),
        SlotDef(HDD,    3,  98, 54),
        SlotDef(HDD,    3, 116, 54),
        SlotDef(FLOPPY, 1, 134, 54),
        SlotDef(EEPROM, 3, 152, 18),
    )

    fun getSlots(tier: Int): List<SlotDef> = when (tier) {
        1 -> TIER1
        2 -> TIER2
        3 -> TIER3
        else -> CREATIVE
    }
}
