package eab.anvilapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eab.anvilapi.AnvilApiMod;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "anvil_api.json";
    
    private Map<String, Boolean> recipeStates = new ConcurrentHashMap<>();
    
    private static ModConfig instance;
    private Path configPath;
    
    private ModConfig() {}
    
    public static ModConfig createAndLoad() {
        instance = new ModConfig();
        instance.configPath = Path.of("config", CONFIG_FILE);
        instance.load();
        return instance;
    }
    
    public static ModConfig getInstance() {
        return instance;
    }
    
    public void load() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null && data.recipeStates != null) {
                    recipeStates.clear();
                    recipeStates.putAll(data.recipeStates);
                }
                AnvilApiMod.LOGGER.info("Loaded config from {}", configPath);
            } catch (IOException e) {
                AnvilApiMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            save();
        }
    }
    
    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                ConfigData data = new ConfigData();
                data.recipeStates = new ConcurrentHashMap<>(recipeStates);
                GSON.toJson(data, writer);
            }
            AnvilApiMod.LOGGER.info("Saved config to {}", configPath);
        } catch (IOException e) {
            AnvilApiMod.LOGGER.error("Failed to save config", e);
        }
    }
    
    public boolean isRecipeEnabled(Identifier recipeId) {
        String key = recipeId.toString();
        return recipeStates.getOrDefault(key, true);
    }
    
    public void setRecipeEnabled(Identifier recipeId, boolean enabled) {
        String key = recipeId.toString();
        if (enabled) {
            recipeStates.remove(key);
        } else {
            recipeStates.put(key, false);
        }
        save();
    }
    
    public void toggleRecipe(Identifier recipeId) {
        setRecipeEnabled(recipeId, !isRecipeEnabled(recipeId));
    }
    
    public Map<String, Boolean> getRecipeStates() {
        return new ConcurrentHashMap<>(recipeStates);
    }
    
    private static class ConfigData {
        Map<String, Boolean> recipeStates = new ConcurrentHashMap<>();
    }
}