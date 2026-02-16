package eab.api;

import eab.api.data.AnvilRepairRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilAPIMod implements ModInitializer {
    public static final String MOD_ID = "anvil_api";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Anvil API Mod");
        
        // 注册数据包资源监听器
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            new AnvilRepairRecipe.ReloadListener()
        );
        
        LOGGER.info("Anvil API Mod initialized successfully");
    }
}