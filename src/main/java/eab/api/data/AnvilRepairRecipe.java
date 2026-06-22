package eab.api.data;

import com.google.gson.*;
import eab.api.AnvilAPIMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class AnvilRepairRecipe {
    private final Identifier id;
    private final Item item;
    private final Item repairMaterial;
    private final int repairAmount;
    private final int materialCost;
    private final int experienceCost;
    
    private static final Map<Identifier, AnvilRepairRecipe> RECIPES = new HashMap<>();
    private static final Map<Item, Map<Item, AnvilRepairRecipe>> ITEM_REPAIR_MAP = new HashMap<>();
    
    public AnvilRepairRecipe(Identifier id, Item item, Item repairMaterial, 
                             int repairAmount, int materialCost, int experienceCost) {
        this.id = id;
        this.item = item;
        this.repairMaterial = repairMaterial;
        this.repairAmount = repairAmount;
        this.materialCost = materialCost;
        this.experienceCost = experienceCost;
    }
    
    public Item getItem() {
        return item;
    }
    
    public Item getRepairMaterial() {
        return repairMaterial;
    }
    
    public int getRepairAmount() {
        return repairAmount;
    }
    
    public int getMaterialCost() {
        return materialCost;
    }
    
    public int getExperienceCost() {
        return experienceCost;
    }
    
    public Identifier getId() {
        return id;
    }
    
    public static AnvilRepairRecipe getRecipe(Item item, Item repairMaterial) {
        Map<Item, AnvilRepairRecipe> materialMap = ITEM_REPAIR_MAP.get(item);
        if (materialMap != null) {
            return materialMap.get(repairMaterial);
        }
        return null;
    }
    
    public static Map<Item, Map<Item, AnvilRepairRecipe>> getAllRecipes() {
        return ITEM_REPAIR_MAP;
    }
    
    public static void clearRecipes() {
        RECIPES.clear();
        ITEM_REPAIR_MAP.clear();
    }
    
    public static void registerRecipe(AnvilRepairRecipe recipe) {
        RECIPES.put(recipe.getId(), recipe);
        
        ITEM_REPAIR_MAP.computeIfAbsent(recipe.getItem(), k -> new HashMap<>())
                       .put(recipe.getRepairMaterial(), recipe);
    }
    
    public static class ReloadListener implements SimpleSynchronousResourceReloadListener {
        private static final Gson GSON = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        
        @Override
        public Identifier getFabricId() {
            return new Identifier(AnvilAPIMod.MOD_ID, "anvil_repair_recipes");
        }
        
        @Override
        public void reload(ResourceManager manager) {
            // 清除现有配方
            clearRecipes();
            
            // 加载所有配方
            String path = "anvil_repair_recipes";
            manager.findResources(path, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                try (Reader reader = resource.getReader()) {
                    JsonObject json = JsonHelper.deserialize(GSON, reader, JsonObject.class);
                    
                    String itemId = JsonHelper.getString(json, "item");
                    String repairMaterialId = JsonHelper.getString(json, "repair_material");
                    int repairAmount = JsonHelper.getInt(json, "repair_amount", 0);
                    int materialCost = JsonHelper.getInt(json, "material_cost", 1);
                    int experienceCost = JsonHelper.getInt(json, "experience_cost", 1);
                    
                    Item item = Registries.ITEM.get(new Identifier(itemId));
                    Item repairMaterial = Registries.ITEM.get(new Identifier(repairMaterialId));
                    
                    if (item != null && repairMaterial != null) {
                        AnvilRepairRecipe recipe = new AnvilRepairRecipe(
                            id, item, repairMaterial, repairAmount, materialCost, experienceCost
                        );
                        registerRecipe(recipe);
                        AnvilAPIMod.LOGGER.info("Loaded anvil repair recipe: {}", id);
                    } else {
                        AnvilAPIMod.LOGGER.warn("Failed to load anvil repair recipe: {} - item or material not found", id);
                    }
                } catch (IOException | JsonParseException e) {
                    AnvilAPIMod.LOGGER.error("Error loading anvil repair recipe: {}", id, e);
                }
            });
            
            AnvilAPIMod.LOGGER.info("Loaded {} anvil repair recipes", RECIPES.size());
        }
    }
}