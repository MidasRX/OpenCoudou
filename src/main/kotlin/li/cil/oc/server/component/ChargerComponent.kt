package li.cil.oc.server.component

/**
 * Charger component - charges OC items (tablets, robots, etc).
 * Takes items from a slot and charges them using energy from capacitors.
 */
class ChargerComponent : AbstractComponent("charger") {
    
    companion object {
        const val CHARGE_RATE = 100.0 // OC energy per tick
    }
    
    init {
        registerMethod("getChargeRate", true, "getChargeRate():number -- Get charge rate per tick") { _ ->
            arrayOf(CHARGE_RATE)
        }
    }
}
