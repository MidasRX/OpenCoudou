package li.cil.oc.server.machine

/**
 * Represents the components installed in a computer case.
 * Scanned from the case inventory before boot.
 */
data class InstalledComponents(
    val cpuTier: Int = -1,           // -1 = no CPU
    val totalMemory: Int = 0,        // Total RAM in bytes
    val gpuTier: Int = -1,           // -1 = no GPU
    val hddTiers: List<Int> = emptyList(),
    val hasEEPROM: Boolean = false
)
