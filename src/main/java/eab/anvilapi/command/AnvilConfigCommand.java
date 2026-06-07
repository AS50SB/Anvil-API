package eab.anvilapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.network.OpenGuiPacket;
import eab.anvilapi.recipe.AnvilRepairRecipe;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AnvilConfigCommand {
    
    private static final SuggestionProvider<ServerCommandSource> RECIPE_SUGGESTIONS = (context, builder) -> {
        Collection<AnvilRepairRecipe> recipes = AnvilApiMod.getRecipeLoader().getAllRecipes();
        for (AnvilRepairRecipe recipe : recipes) {
            builder.suggest(recipe.getId().toString());
        }
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("anvilapi")
            .requires(source -> source.hasPermissionLevel(2))
            .then(literal("gui")
                .executes(AnvilConfigCommand::openGui)
            )
            .then(literal("list")
                .executes(AnvilConfigCommand::listRecipes)
            )
            .then(literal("enable")
                .then(argument("recipe_id", StringArgumentType.string())
                    .suggests(RECIPE_SUGGESTIONS)
                    .executes(ctx -> setRecipeState(ctx, true))
                )
            )
            .then(literal("disable")
                .then(argument("recipe_id", StringArgumentType.string())
                    .suggests(RECIPE_SUGGESTIONS)
                    .executes(ctx -> setRecipeState(ctx, false))
                )
            )
            .then(literal("toggle")
                .then(argument("recipe_id", StringArgumentType.string())
                    .suggests(RECIPE_SUGGESTIONS)
                    .executes(AnvilConfigCommand::toggleRecipe)
                )
            )
            .then(literal("reload")
                .executes(AnvilConfigCommand::reloadRecipes)
            )
        );
    }
    
    private static int openGui(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        if (player != null) {
            // 发送网络包到客户端，让客户端打开 GUI
            ServerPlayNetworking.send(player, new OpenGuiPacket());
            context.getSource().sendFeedback(() -> Text.literal("§a正在打开配置界面..."), false);
        } else {
            context.getSource().sendError(Text.literal("§c此命令只能由玩家执行"));
        }
        
        return 1;
    }
    
    private static int listRecipes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        var recipes = AnvilApiMod.getRecipeLoader().getAllRecipes();
    
        if (recipes.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§c没有加载任何配方！请确保数据包正确放置并执行 /reload"), false);
            return 0;
        }
    
        source.sendFeedback(() -> Text.literal("§6=== Anvil API Recipes ==="), false);
    
        for (AnvilRepairRecipe recipe : recipes) {
            String status = recipe.isEnabled() ? "§a[✔]" : "§c[✘]";
            // 确保 getId() 返回正确的 Identifier
            source.sendFeedback(() -> Text.literal(String.format("§7%s §f%s §7-> §f%s §7(repair: %d, cost: %d, xp: %d)",
                status,
                recipe.getId().toString(),
                recipe.getItemName(),
                recipe.getMaterialName(),
                recipe.getRepairAmount(),
                recipe.getMaterialCost(),
                recipe.getExperienceCost()
            )), false);
        }
    
        source.sendFeedback(() -> Text.literal("§6========================="), false);
        source.sendFeedback(() -> Text.literal("§7Use §e/anvilapi enable/disable/toggle <recipe_id>"), false);
        
        return recipes.size();
    }
    
    private static int setRecipeState(CommandContext<ServerCommandSource> context, boolean enabled) {
        String recipeIdStr = StringArgumentType.getString(context, "recipe_id");
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        
        if (recipeId == null) {
            context.getSource().sendError(Text.literal("§cInvalid recipe ID: " + recipeIdStr));
            return 0;
        }
        
        var recipe = AnvilApiMod.getRecipeLoader().getRecipe(recipeId);
        if (recipe == null) {
            context.getSource().sendError(Text.literal("§cRecipe not found: " + recipeIdStr));
            return 0;
        }
        
        AnvilApiMod.getRecipeLoader().setRecipeEnabled(recipeId, enabled);
        String state = enabled ? "enabled" : "disabled";
        context.getSource().sendFeedback(() -> Text.literal("§aRecipe " + recipeIdStr + " " + state), true);
        
        return 1;
    }
    
    private static int toggleRecipe(CommandContext<ServerCommandSource> context) {
        String recipeIdStr = StringArgumentType.getString(context, "recipe_id");
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        
        if (recipeId == null) {
            context.getSource().sendError(Text.literal("§cInvalid recipe ID: " + recipeIdStr));
            return 0;
        }
        
        var recipe = AnvilApiMod.getRecipeLoader().getRecipe(recipeId);
        if (recipe == null) {
            context.getSource().sendError(Text.literal("§cRecipe not found: " + recipeIdStr));
            return 0;
        }
        
        AnvilApiMod.getRecipeLoader().toggleRecipe(recipeId);
        String newState = recipe.isEnabled() ? "enabled" : "disabled";
        context.getSource().sendFeedback(() -> Text.literal("§aRecipe " + recipeIdStr + " is now " + newState), true);
        
        return 1;
    }
    
    private static int reloadRecipes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            AnvilApiMod.getRecipeLoader().loadAllRecipes(source.getServer().getResourceManager());
            int count = AnvilApiMod.getRecipeLoader().getAllRecipes().size();
            source.sendFeedback(() -> Text.literal("§aReloaded " + count + " anvil repair recipes"), true);
            AnvilApiMod.LOGGER.info("Reloaded {} recipes", count);
        } catch (Exception e) {
            source.sendError(Text.literal("§cFailed to reload recipes: " + e.getMessage()));
            AnvilApiMod.LOGGER.error("Failed to reload recipes", e);
        }
        
        return 1;
    }
}