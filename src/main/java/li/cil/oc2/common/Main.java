/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import dev.architectury.platform.forge.EventBuses;
import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.sedna.Sedna;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(API.MOD_ID)
public final class Main {
    public Main(FMLJavaModLoadingContext context) {
        EventBuses.registerModEventBus(API.MOD_ID, context.getModEventBus());
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();

        context.registerConfig(ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);

        RegistryUtils.begin();

        ItemTags.initialize();
        BlockTags.initialize();
        Blocks.initialize(context);
        Items.initialize(context);
        BlockEntities.initialize(context);
        Entities.initialize(context);
        Containers.initialize(context);
        RecipeSerializers.initialize(context);
        SoundEvents.initialize(context);

        ProviderRegistry.initialize(context);
        DeviceTypes.initialize(context);

        BlockDeviceDataRegistry.initialize(context);
        FirmwareRegistry.initialize(context);

        RegistryUtils.finish(context);

        context.getModEventBus().register(CommonSetup.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Manuals.initialize(context));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            context.getModEventBus().register(ClientSetup.class));

        ItemGroup.TAB_REGISTER.register(context.getModEventBus());

        NativeLoader.loadLibrary();
    }

    public static class NativeLoader {
        public static void loadLibrary() {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            String platform;
            String libName;
            String fileNameArch = getArchString(arch);

            if (os.contains("mac")) {
                platform = "macos";
                libName = "liboc2rnet-" + fileNameArch + ".dylib";
            } else if (os.contains("win")) {
                platform = "windows";
                libName = "oc2rnet-"+ fileNameArch + ".dll";
            } else if (os.contains("nux") || os.contains("nix")) {
                platform = "linux";
                libName = "liboc2rnet-linux-" + fileNameArch + ".so";
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            String resourcePath = "/natives/" + platform + "/" + libName;
            try {
                Path tempFile = extractToTemp(resourcePath);
                System.load(tempFile.toAbsolutePath().toString());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load native library: " + resourcePath, e);
            }
        }

        private static String getArchString(String arch) {
            if (arch.equals("amd64")) {
                return "x86_64";
            } else if (arch.equals("aarch64")) {
                return "arm64";
            } else {
                throw new UnsupportedOperationException("Unsupported architecture: " + arch);
            }
        }

        private static Path extractToTemp(String resourcePath) throws IOException {
            try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
                Path local = Path.of(System.getProperty("user.dir"), System.mapLibraryName("oc2rnet"));
                Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
                return local;
            }
        }
    }
}
