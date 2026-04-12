package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.*
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModMenus {
    private val MENUS: DeferredRegister<MenuType<*>> = 
        DeferredRegister.create(Registries.MENU, OpenComputers.MOD_ID)

    val CASE: DeferredHolder<MenuType<*>, MenuType<CaseMenu>> =
        MENUS.register("case") { ->
            IMenuTypeExtension.create(CaseMenu::fromNetwork)
        }

    val ASSEMBLER: DeferredHolder<MenuType<*>, MenuType<AssemblerMenu>> =
        MENUS.register("assembler") { ->
            IMenuTypeExtension.create(AssemblerMenu::fromNetwork)
        }

    val DISASSEMBLER: DeferredHolder<MenuType<*>, MenuType<DisassemblerMenu>> =
        MENUS.register("disassembler") { ->
            IMenuTypeExtension.create(DisassemblerMenu::fromNetwork)
        }

    val RAID: DeferredHolder<MenuType<*>, MenuType<RaidMenu>> =
        MENUS.register("raid") { ->
            IMenuTypeExtension.create(RaidMenu::fromNetwork)
        }

    val CHARGER: DeferredHolder<MenuType<*>, MenuType<ChargerMenu>> =
        MENUS.register("charger") { ->
            IMenuTypeExtension.create(ChargerMenu::fromNetwork)
        }

    val DISK_DRIVE: DeferredHolder<MenuType<*>, MenuType<DiskDriveMenu>> =
        MENUS.register("disk_drive") { ->
            IMenuTypeExtension.create(DiskDriveMenu::fromNetwork)
        }

    val PRINTER: DeferredHolder<MenuType<*>, MenuType<PrinterMenu>> =
        MENUS.register("printer") { ->
            IMenuTypeExtension.create(PrinterMenu::fromNetwork)
        }

    val ADAPTER: DeferredHolder<MenuType<*>, MenuType<AdapterMenu>> =
        MENUS.register("adapter") { ->
            IMenuTypeExtension.create(AdapterMenu::fromNetwork)
        }

    val RACK: DeferredHolder<MenuType<*>, MenuType<RackMenu>> =
        MENUS.register("rack") { ->
            IMenuTypeExtension.create(RackMenu::fromNetwork)
        }

    val DATABASE: DeferredHolder<MenuType<*>, MenuType<DatabaseMenu>> =
        MENUS.register("database") { ->
            IMenuTypeExtension.create(DatabaseMenu::fromNetwork)
        }

    val RELAY: DeferredHolder<MenuType<*>, MenuType<RelayMenu>> =
        MENUS.register("relay") { ->
            IMenuTypeExtension.create(RelayMenu::fromNetwork)
        }

    val ROBOT: DeferredHolder<MenuType<*>, MenuType<RobotMenu>> =
        MENUS.register("robot") { ->
            IMenuTypeExtension.create(RobotMenu::fromNetwork)
        }

    val DRONE: DeferredHolder<MenuType<*>, MenuType<DroneMenu>> =
        MENUS.register("drone") { ->
            IMenuTypeExtension.create(DroneMenu::fromNetwork)
        }

    val SERVER: DeferredHolder<MenuType<*>, MenuType<ServerMenu>> =
        MENUS.register("server") { ->
            IMenuTypeExtension.create(ServerMenu::fromNetwork)
        }

    val TABLET: DeferredHolder<MenuType<*>, MenuType<TabletMenu>> =
        MENUS.register("tablet") { ->
            IMenuTypeExtension.create(TabletMenu::fromNetwork)
        }

    val SWITCH: DeferredHolder<MenuType<*>, MenuType<SwitchMenu>> =
        MENUS.register("switch") { ->
            IMenuTypeExtension.create(SwitchMenu::fromNetwork)
        }

    fun register(eventBus: IEventBus) {
        MENUS.register(eventBus)
    }
}
