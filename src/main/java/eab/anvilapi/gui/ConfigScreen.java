package eab.anvilapi.gui;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.recipe.AnvilRepairRecipe;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ConfigScreen {
    
    public static void open() {
        var recipes = AnvilApiMod.getRecipeLoader().getAllRecipes();
        
        ConfigBuilder builder = ConfigBuilder.create()
            .setTitle(Text.literal("Anvil API Configuration"))
            .setTransparentBackground(false)
            .setSavingRunnable(() -> {
                AnvilApiMod.getConfig().save();
                AnvilApiMod.LOGGER.info("Config saved");
            });
        
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Anvil Repair Recipes"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        for (AnvilRepairRecipe recipe : recipes) {
            String displayName = recipe.getId().toString() + " (" + recipe.getItemName() + " -> " + recipe.getMaterialName() + ")";
            
            category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal(displayName),
                recipe.isEnabled()
            ).setDefaultValue(true)
            .setTooltip(Text.literal(
                "Repair: " + recipe.getRepairAmount() + " | Cost: " + recipe.getMaterialCost() + " " + recipe.getMaterialName() + " | XP: " + recipe.getExperienceCost()
            ))
            .setSaveConsumer(recipe::setEnabled)
            .build());
        }
        
        category.addEntry(entryBuilder.startTextDescription(
            Text.literal("§eClick 'Save' to apply changes. Use /anvilapi reload to reload datapacks.")
        ).build());
        
        MinecraftClient.getInstance().setScreen(builder.build());
    }
}