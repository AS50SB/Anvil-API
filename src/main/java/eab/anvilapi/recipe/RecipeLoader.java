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
    private final Map<Identifier, PackMetadata> packMetadata = new ConcurrentHashMap<>();
    
    public static class PackMetadata {
        public String packName;
        public String packVersion;
        public int anvilApiFormat;
        public List<String> authors;
        public Map<String, String> depends;
        
        public PackMetadata() {
            this.packName = "";
            this.packVersion = "1.0.0";
            this.anvilApiFormat = 101;
            this.authors = new ArrayList<>();
            this.depends = new HashMap<>();
        }
    }
    
    public void loadAllRecipes(ResourceManager resourceManager) {
        Map<Identifier, AnvilRepairRecipe> newRecipes = new ConcurrentHashMap<>();
        Map<Identifier, PackMetadata> newMetadata = new ConcurrentHashMap<>();
        
        for (String namespace : resourceManager.getAllNamespaces()) {
            try {
                // 加载元数据文件
                Identifier metadataId = Identifier.of(namespace, "anvilapi.mcmeta");
                if (resourceManager.getResource(metadataId).isPresent()) {
                    var resource = resourceManager.getResource(metadataId).get();
                    try (InputStream stream = resource.getInputStream();
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        PackMetadata meta = new PackMetadata();
                        
                        if (json.has("anvil_recipes_pack")) {
                            JsonObject pack = json.getAsJsonObject("anvil_recipes_pack");
                            if (pack.has("pack_version")) {
                                meta.packVersion = pack.get("pack_version").getAsString();
                            }
                            if (pack.has("anvil_api_format")) {
                                meta.anvilApiFormat = pack.get("anvil_api_format").getAsInt();
                            }
                            if (pack.has("authors")) {
                                pack.getAsJsonArray("authors").forEach(author -> 
                                    meta.authors.add(author.getAsString()));
                            }
                            if (pack.has("pack_name")) {
                                meta.packName = pack.get("pack_name").getAsString();
                            }
                        }
                        if (json.has("depends")) {
                            JsonObject depends = json.getAsJsonObject("depends");
                            depends.entrySet().forEach(entry -> 
                                meta.depends.put(entry.getKey(), entry.getValue().getAsString()));
                        }
                        
                        newMetadata.put(Identifier.of(namespace, "pack"), meta);
                    } catch (Exception e) {
                        AnvilApiMod.LOGGER.warn("Failed to load anvilapi.mcmeta for namespace {}", namespace, e);
                    }
                }
                
                // 加载配方
                var resources = resourceManager.findResources(RECIPES_PATH, path -> path.toString().endsWith(".json"));
                
                for (var entry : resources.entrySet()) {
                    Identifier id = entry.getKey();
                    if (!id.getNamespace().equals(namespace)) {
                        continue;
                    }
                    
                    try (InputStream stream = entry.getValue().getInputStream();
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        
                        String itemId = json.get("item").getAsString();
                        String materialId = json.get("repair_material").getAsString();
                        String repairAmount = json.get("repair_amount").getAsString();
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
                            AnvilApiMod.LOGGER.info("Loaded recipe: {} -> {} + {} (repair: {})", 
                                recipeId, itemId, materialId, repairAmount);
                        } else {
                            AnvilApiMod.LOGGER.warn("Failed to load recipe: invalid item or material in {}", id);
                        }
                    } catch (Exception e) {
                        AnvilApiMod.LOGGER.error("Failed to load recipe from {}", id, e);
                    }
                }
            } catch (Exception e) {
                AnvilApiMod.LOGGER.error("Failed to process namespace {}", namespace, e);
            }
        }
        
        recipes.clear();
        recipes.putAll(newRecipes);
        packMetadata.clear();
        packMetadata.putAll(newMetadata);
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
    
    public PackMetadata getPackMetadata(Identifier namespace) {
        return packMetadata.getOrDefault(namespace, null);
    }
    
    public Set<Identifier> getLoadedNamespaces() {
        return packMetadata.keySet();
    }
}