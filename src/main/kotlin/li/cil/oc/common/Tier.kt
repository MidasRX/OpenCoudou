package li.cil.oc.common

/**
 * Tier levels for OpenComputers components.
 * 
 * Components come in different tiers with increasing capabilities:
 * - ONE: Basic tier, cheapest and lowest specs
 * - TWO: Intermediate tier, moderate price and specs
 * - THREE: Advanced tier, expensive but powerful
 * - FOUR/CREATIVE: Unlimited/creative tier (not always available)
 * 
 * Used throughout the mod for:
 * - CPUs (affects call budget)
 * - Memory (affects RAM size)
 * - Hard drives (affects storage capacity)
 * - Graphics cards (affects GPU capabilities)
 * - Network cards (affects network speed)
 * - Cases (affects component slots)
 * - Screens (affects resolution)
 */
enum class Tier {
    ONE,
    TWO,
    THREE,
    FOUR,
    CREATIVE;
    
    companion object {
        /** Convert integer tier to enum */
        fun fromInt(tier: Int): Tier = when(tier) {
            0 -> ONE
            1 -> TWO
            2 -> THREE
            3 -> FOUR
            else -> CREATIVE
        }
        
        /** Convert enum to integer tier */
        fun toInt(tier: Tier): Int = tier.ordinal
    }
    
    /** Get the tier as a 1-based display number */
    val displayNumber: Int get() = ordinal + 1
    
    /** Check if this tier is creative/unlimited */
    val isCreative: Boolean get() = this == CREATIVE || this == FOUR
}
