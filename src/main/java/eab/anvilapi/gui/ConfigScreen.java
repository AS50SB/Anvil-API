package eab.anvilapi.gui;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.recipe.AnvilRepairRecipe;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ConfigScreen {
    
    public static void open() {
        var recipes = AnvilApiMod.getRecipeLoader().getAllRecipes();
        
        ConfigBuilder builder = ConfigBuilder.create()
            .setTitle(Text.translatable("anvil_api.title"))
            .setTransparentBackground(false)
            .setSavingRunnable(() -> {
                AnvilApiMod.getConfig().save();
            });
        
        // 添加包列表按钮
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);
        
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Anvil Repair Recipes"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // 添加查看数据包按钮
        category.addEntry(entryBuilder.startTextDescription(
            Text.literal("§e点击下方按钮查看所有数据包信息")
        ).build());
        
        category.addEntry(entryBuilder.startTextDescription(
            Text.literal("§7[查看数据包] §f点击打开数据包列表")
        ).setTooltip(Text.literal("双击数据包查看详细信息"))
        .build());
        
        // 这里可以添加一个按钮来打开包列表，但由于 Cloth Config 的限制，我们通过命令访问
        
        for (AnvilRepairRecipe recipe : recipes) {
            String displayName = recipe.getId().toString();
            String tooltipText = String.format(
                "Item: %s | Material: %s | Repair: %s | Cost: %d | XP: %d",
                recipe.getItemName(),
                recipe.getMaterialName(),
                recipe.getRepairAmountRaw(),
                recipe.getMaterialCost(),
                recipe.getExperienceCost()
            );
            
            category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal(displayName),
                recipe.isEnabled()
            ).setDefaultValue(true)
            .setTooltip(Text.literal(tooltipText))
            .setSaveConsumer(recipe::setEnabled)
            .build());
        }
        
        MinecraftClient.getInstance().setScreen(builder.build());
    }
}