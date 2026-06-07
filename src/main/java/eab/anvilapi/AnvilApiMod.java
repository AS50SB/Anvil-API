package eab.anvilapi;

import eab.anvilapi.command.AnvilConfigCommand;
import eab.anvilapi.config.ModConfig;
import eab.anvilapi.network.OpenGuiPacket;
import eab.anvilapi.recipe.RecipeLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilApiMod implements ModInitializer {
    public static final String MOD_ID = "anvil_api";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static RecipeLoader recipeLoader;
    private static ModConfig config;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Anvil API");
        
        // 注册网络包类型
        PayloadTypeRegistry.playS2C().register(OpenGuiPacket.ID, OpenGuiPacket.CODEC);
        
        config = ModConfig.createAndLoad();
        recipeLoader = new RecipeLoader();
        
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of(MOD_ID, "recipe_loader");
                }
                
                @Override
                public void reload(ResourceManager manager) {
                    recipeLoader.loadAllRecipes(manager);
                    LOGGER.info("Loaded {} anvil repair recipes", recipeLoader.getAllRecipes().size());
                }
            });
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AnvilConfigCommand.register(dispatcher);
        });
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            recipeLoader.loadAllRecipes(server.getResourceManager());
        });
    }
    
    public static RecipeLoader getRecipeLoader() {
        return recipeLoader;
    }
    
    public static ModConfig getConfig() {
        return config;
    }
}