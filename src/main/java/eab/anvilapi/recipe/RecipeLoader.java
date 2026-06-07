package eab.anvilapi.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eab.anvilapi.AnvilApiMod;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.item.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String RECIPES_PATH = "anvil_repair_recipes";
    
    private final Map<Identifier, AnvilRepairRecipe> recipes = new ConcurrentHashMap<>();
    
    public void loadAllRecipes(ResourceManager resourceManager) {
        Map<Identifier, AnvilRepairRecipe> newRecipes = new ConcurrentHashMap<>();
        
        // 查找所有命名空间下的 anvil_repair_recipes 文件夹
        for (String namespace : resourceManager.getAllNamespaces()) {
            // 正确的方式：使用 findResources 直接查找路径
            String searchPath = RECIPES_PATH;
            
            try {
                var resources = resourceManager.findResources(searchPath, path -> path.toString().endsWith(".json"));
                
                for (var entry : resources.entrySet()) {
                    Identifier id = entry.getKey();
                    // 检查是否属于当前命名空间
                    if (!id.getNamespace().equals(namespace)) {
                        continue;
                    }
                    
                    try (InputStream stream = entry.getValue().getInputStream();
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        
                        String itemId = json.get("item").getAsString();
                        String materialId = json.get("repair_material").getAsString();
                        int repairAmount = json.get("repair_amount").getAsInt();
                        int materialCost = json.get("material_cost").getAsInt();
                        int experienceCost = json.get("experience_cost").getAsInt();
                        
                        Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
                        Item material = Registries.ITEM.get(Identifier.tryParse(materialId));
                        
                        if (item != null && material != null) {
                            String path = id.getPath();
                            String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                            Identifier recipeId = Identifier.of(namespace, fileName);
                            
                            AnvilRepairRecipe recipe = new AnvilRepairRecipe(recipeId, item, material, 
                                repairAmount, materialCost, experienceCost);
                            
                            boolean enabled = AnvilApiMod.getConfig().isRecipeEnabled(recipeId);
                            recipe.setEnabled(enabled);
                            
                            newRecipes.put(recipeId, recipe);
                            AnvilApiMod.LOGGER.info("Loaded recipe: {} -> {} + {}", 
                                recipeId, itemId, materialId);
                        } else {
                            AnvilApiMod.LOGGER.warn("Failed to load recipe: invalid item or material in {}", id);
                        }
                    } catch (Exception e) {
                        AnvilApiMod.LOGGER.error("Failed to load recipe from {}", id, e);
                    }
                }
            } catch (Exception e) {
                AnvilApiMod.LOGGER.error("Failed to find recipes in namespace {}", namespace, e);
            }
        }
        
        recipes.clear();
        recipes.putAll(newRecipes);
        AnvilApiMod.LOGGER.info("Total loaded recipes: {}", recipes.size());
    }
    
    public Optional<AnvilRepairRecipe> getRecipeFor(Item item, Item material) {
        return recipes.values().stream()
            .filter(recipe -> recipe.matches(item, material))
            .findFirst();
    }
    
    public Collection<AnvilRepairRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }
    
    public AnvilRepairRecipe getRecipe(Identifier id) {
        return recipes.get(id);
    }
    
    public void setRecipeEnabled(Identifier id, boolean enabled) {
        AnvilRepairRecipe recipe = recipes.get(id);
        if (recipe != null) {
            recipe.setEnabled(enabled);
            AnvilApiMod.getConfig().setRecipeEnabled(id, enabled);
        }
    }
    
    public void toggleRecipe(Identifier id) {
        AnvilRepairRecipe recipe = recipes.get(id);
        if (recipe != null) {
            boolean newState = !recipe.isEnabled();
            recipe.setEnabled(newState);
            AnvilApiMod.getConfig().setRecipeEnabled(id, newState);
        }
    }
}