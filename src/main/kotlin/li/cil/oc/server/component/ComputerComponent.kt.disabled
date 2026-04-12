package li.cil.oc.server.component

import li.cil.oc.util.OCLogger
import java.util.UUID

/**
 * Computer component that provides core computer APIs.
 * This is an internal component that's always present.
 */
class ComputerComponent(
    private val tier: Int = 1,
    private val computerAddress: String = UUID.randomUUID().toString()
) : AbstractComponent("computer") {
    
    // Maximum call budget based on tier
    private val maxCallBudget = when (tier) {
        1 -> 0.5
        2 -> 1.0
        3 -> 1.5
        else -> 1.0
    }
    
    // Architecture name
    private val architecture = "Lua 5.3"
    
    // Boot address (eeprom address)
    var bootAddress: String? = null
    
    // Temp filesystem address  
    var tmpAddress: String? = null
    
    // Users that can interact with this computer
    private val users = mutableSetOf<String>()
    
    init {
        registerMethod("isRobot", true, "isRobot():boolean -- Whether this is a robot") { _ ->
            arrayOf(false) // Will be overridden in Robot
        }
        
        registerMethod("getArchitecture", true, "getArchitecture():string -- Get Lua architecture") { _ ->
            arrayOf(architecture)
        }
        
        registerMethod("getArchitectures", true, "getArchitectures():table -- List available architectures") { _ ->
            arrayOf(arrayOf(architecture))
        }
        
        registerMethod("setArchitecture", false, "setArchitecture(arch:string):boolean -- Set architecture") { _ ->
            // Only Lua 5.3 supported
            arrayOf(true)
        }
        
        registerMethod("getDeviceInfo", true, "getDeviceInfo():table -- Get info about components") { _ ->
            // Would return detailed component info
            arrayOf(emptyMap<String, Any>())
        }
        
        registerMethod("getProgramLocations", true, "getProgramLocations():table -- Get standard program paths") { _ ->
            arrayOf(mapOf(
                "bin" to "/bin",
                "lib" to "/lib", 
                "usr" to "/usr"
            ))
        }
        
        registerMethod("addUser", false, "addUser(name:string):boolean -- Add user to whitelist") { args ->
            val name = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(false, "no name")
            if (users.size >= 32) {
                return@registerMethod arrayOf(false, "user list full")
            }
            users.add(name)
            arrayOf(true)
        }
        
        registerMethod("removeUser", false, "removeUser(name:string):boolean -- Remove user from whitelist") { args ->
            val name = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(false)
            arrayOf(users.remove(name))
        }
        
        registerMethod("users", true, "users():table -- Get whitelisted users") { _ ->
            arrayOf(users.toTypedArray())
        }
        
        registerMethod("stop", false, "stop():boolean -- Stop the computer") { _ ->
            // Would stop the machine
            arrayOf(true)
        }
        
        registerMethod("start", false, "start():boolean -- Start the computer") { _ ->
            // Would start the machine
            arrayOf(true)
        }
        
        registerMethod("isRunning", true, "isRunning():boolean -- Whether computer is running") { _ ->
            arrayOf(true) // If this is called, we're running
        }
        
        registerMethod("beep", false, "beep([freq:number[,duration:number]])") { args ->
            val freq = (args.getOrNull(0) as? Number)?.toInt() ?: 440
            val duration = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.1
            OCLogger.computer("BEEP", "freq=$freq duration=$duration")
            // Would play beep sound
            arrayOf()
        }
        
        registerMethod("getBootAddress", true, "getBootAddress():string -- Get boot EEPROM address") { _ ->
            arrayOf(bootAddress)
        }
        
        registerMethod("setBootAddress", false, "setBootAddress(addr:string) -- Set boot address") { args ->
            bootAddress = args.getOrNull(0)?.toString()
            arrayOf()
        }
        
        registerMethod("tmpAddress", true, "tmpAddress():string -- Get tmpfs address") { _ ->
            arrayOf(tmpAddress)
        }
    }
}
