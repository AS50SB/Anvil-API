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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AnvilConfigCommand {
    
    private static boolean firstTimeUse = true;
    
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
            .then(literal("info")
                .executes(AnvilConfigCommand::showInfo)
            )
            .then(literal("gui")
                .executes(AnvilConfigCommand::openGui)
            )
            .then(literal("list")
                .executes(AnvilConfigCommand::listRecipes)
            )
            .then(literal("create")
                .executes(AnvilConfigCommand::createExamplePack)
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
    
    private static void checkFirstTime(ServerCommandSource source) {
        if (firstTimeUse) {
            source.sendFeedback(() -> Text.literal("§7[铁砧API] §f首次使用命令，输入 §e/anvilapi info §f查看帮助信息"), false);
        }
    }
    
    private static int showInfo(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player != null) {
            ServerPlayNetworking.send(player, new OpenGuiPacket("info"));
            context.getSource().sendFeedback(() -> Text.literal("§a正在打开信息界面..."), false);
        } else {
            context.getSource().sendError(Text.literal("§c此命令只能由玩家执行"));
        }
        firstTimeUse = false;
        return 1;
    }
    
    private static int openGui(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player != null) {
            ServerPlayNetworking.send(player, new OpenGuiPacket("config"));
            context.getSource().sendFeedback(() -> Text.literal("§a正在打开配置界面..."), false);
        } else {
            context.getSource().sendError(Text.literal("§c此命令只能由玩家执行"));
        }
        firstTimeUse = false;
        return 1;
    }
    
    private static int listRecipes(CommandContext<ServerCommandSource> context) {
        checkFirstTime(context.getSource());
        ServerCommandSource source = context.getSource();
        var recipes = AnvilApiMod.getRecipeLoader().getAllRecipes();
        
        if (recipes.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§c没有加载任何配方！请确保数据包正确放置并执行 /reload"), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("§6=== 铁砧API配方列表 ==="), false);
        
        for (AnvilRepairRecipe recipe : recipes) {
            String status = recipe.isEnabled() ? "§a[✔]" : "§c[✘]";
            String displayAmount = recipe.isPercentage() ? recipe.getRepairAmountRaw() : String.valueOf(recipe.getRepairAmountFixed());
            source.sendFeedback(() -> Text.literal(String.format("§7%s §f%s §7-> §f%s §7(修复: %s, 消耗: %d, 经验: %d)",
                status,
                recipe.getId().toString(),
                recipe.getMaterialName(),
                displayAmount,
                recipe.getMaterialCost(),
                recipe.getExperienceCost()
            )), false);
        }
        
        source.sendFeedback(() -> Text.literal("§6========================="), false);
        source.sendFeedback(() -> Text.literal("§7使用 §e/anvilapi enable/disable/toggle <配方ID>"), false);
        
        firstTimeUse = false;
        return recipes.size();
    }
    
    private static int createExamplePack(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String packName = "example_anvil_pack";
    
        try {
            // getRunDirectory() 直接返回 Path
            Path serverDir = source.getServer().getRunDirectory();
            Path savesDir = serverDir.resolve("saves");
        
            // 尝试获取当前世界名称
            var server = source.getServer();
            String levelName = server.getSaveProperties().getLevelName();
            Path worldDir = savesDir.resolve(levelName);
        
            // 如果找不到世界目录，使用当前目录
            if (!Files.exists(worldDir)) {
                worldDir = serverDir;
            }
        
            Path datapacksDir = worldDir.resolve("datapacks").resolve(packName);
        
            if (Files.exists(datapacksDir)) {
                source.sendError(Text.literal("§c数据包已存在: " + packName));
                return 0;
            }
        
            // 创建目录结构
            Files.createDirectories(datapacksDir.resolve("data/example/anvil_repair_recipes"));
        
            // pack.mcmeta
            String packMcmeta = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"Example Anvil API datapack\"\n  }\n}";
            Files.writeString(datapacksDir.resolve("pack.mcmeta"), packMcmeta);
        
            // anvilapi.mcmeta
            String anvilapiMcmeta = "{\n  \"anvil_recipes_pack\": {\n    \"pack_name\": \"Example Anvil Pack\",\n    \"pack_version\": \"1.0.0\",\n    \"anvil_api_format\": 101,\n    \"authors\": [\"Your Name\"]\n  },\n  \"depends\": {\n    \"anvil_api\": \"*\"\n  }\n}";
            Files.writeString(datapacksDir.resolve("anvilapi.mcmeta"), anvilapiMcmeta);
        
            // 示例配方
            String exampleRecipe = "{\n  \"item\": \"minecraft:diamond_sword\",\n  \"repair_material\": \"minecraft:diamond\",\n  \"repair_amount\": \"50%\",\n  \"material_cost\": 1,\n  \"experience_cost\": 2\n}";
            Files.writeString(datapacksDir.resolve("data/example/anvil_repair_recipes/example.json"), exampleRecipe);
        
            source.sendFeedback(() -> Text.literal("§a示例数据包已创建于: " + datapacksDir.toString()), true);
            source.sendFeedback(() -> Text.literal("§7请执行 §e/reload §7和 §e/anvilapi reload §7加载数据包"), false);
        
        } catch (IOException e) {
            source.sendError(Text.literal("§c创建示例数据包失败: " + e.getMessage()));
            AnvilApiMod.LOGGER.error("Failed to create example datapack", e);
        }
    
        return 1;
    }
    
    private static int setRecipeState(CommandContext<ServerCommandSource> context, boolean enabled) {
        checkFirstTime(context.getSource());
        String recipeIdStr = StringArgumentType.getString(context, "recipe_id");
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        
        if (recipeId == null) {
            context.getSource().sendError(Text.literal("§c无效的配方ID: " + recipeIdStr));
            return 0;
        }
        
        var recipe = AnvilApiMod.getRecipeLoader().getRecipe(recipeId);
        if (recipe == null) {
            context.getSource().sendError(Text.literal("§c未找到配方: " + recipeIdStr));
            return 0;
        }
        
        AnvilApiMod.getRecipeLoader().setRecipeEnabled(recipeId, enabled);
        String state = enabled ? "启用" : "禁用";
        context.getSource().sendFeedback(() -> Text.literal("§a配方 " + recipeIdStr + " 已" + state), true);
        
        firstTimeUse = false;
        return 1;
    }
    
    private static int toggleRecipe(CommandContext<ServerCommandSource> context) {
        checkFirstTime(context.getSource());
        String recipeIdStr = StringArgumentType.getString(context, "recipe_id");
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        
        if (recipeId == null) {
            context.getSource().sendError(Text.literal("§c无效的配方ID: " + recipeIdStr));
            return 0;
        }
        
        var recipe = AnvilApiMod.getRecipeLoader().getRecipe(recipeId);
        if (recipe == null) {
            context.getSource().sendError(Text.literal("§c未找到配方: " + recipeIdStr));
            return 0;
        }
        
        AnvilApiMod.getRecipeLoader().toggleRecipe(recipeId);
        String newState = recipe.isEnabled() ? "启用" : "禁用";
        context.getSource().sendFeedback(() -> Text.literal("§a配方 " + recipeIdStr + " 现在为" + newState), true);
        
        firstTimeUse = false;
        return 1;
    }
    
    private static int reloadRecipes(CommandContext<ServerCommandSource> context) {
        checkFirstTime(context.getSource());
        ServerCommandSource source = context.getSource();
        
        try {
            AnvilApiMod.getRecipeLoader().loadAllRecipes(source.getServer().getResourceManager());
            int count = AnvilApiMod.getRecipeLoader().getAllRecipes().size();
            source.sendFeedback(() -> Text.literal("§a已重新加载 " + count + " 个铁砧修复配方"), true);
            AnvilApiMod.LOGGER.info("Reloaded {} recipes", count);
        } catch (Exception e) {
            source.sendError(Text.literal("§c重新加载配方失败: " + e.getMessage()));
            AnvilApiMod.LOGGER.error("Failed to reload recipes", e);
        }
        
        firstTimeUse = false;
        return 1;
    }
}