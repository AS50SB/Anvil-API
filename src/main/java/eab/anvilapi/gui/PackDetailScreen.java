package eab.anvilapi.gui;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.recipe.RecipeLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PackDetailScreen extends Screen {
    private final Screen parent;
    private final Identifier namespace;
    private final RecipeLoader.PackMetadata metadata;
    
    public PackDetailScreen(Screen parent, Identifier namespace, RecipeLoader.PackMetadata metadata) {
        super(Text.literal("Pack: " + namespace.getNamespace()));
        this.parent = parent;
        this.namespace = namespace;
        this.metadata = metadata;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("anvil_api.back"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int y = 20;
        
        String packName = metadata.packName.isEmpty() ? namespace.getNamespace() : metadata.packName;
        context.drawCenteredTextWithShadow(this.textRenderer, "§l" + packName, this.width / 2, y, 0xFFFFFF);
        y += 25;
        
        context.drawTextWithShadow(this.textRenderer, "§7命名空间§f: " + namespace.getNamespace(), 20, y, 0xCCCCCC);
        y += 15;
        
        context.drawTextWithShadow(this.textRenderer, "§7包版本§f: " + metadata.packVersion, 20, y, 0xCCCCCC);
        y += 15;
        
        int currentFormat = 101;
        String formatStatus;
        if (metadata.anvilApiFormat < currentFormat) {
            formatStatus = "§c[过时]";
        } else if (metadata.anvilApiFormat > currentFormat) {
            formatStatus = "§e[新版]";
        } else {
            formatStatus = "§a[匹配]";
        }
        context.drawTextWithShadow(this.textRenderer, "§7API格式§f: " + metadata.anvilApiFormat + " " + formatStatus, 20, y, 0xCCCCCC);
        y += 15;
        
        if (!metadata.authors.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "§7作者§f: " + String.join(", ", metadata.authors), 20, y, 0xCCCCCC);
            y += 15;
        }
        
        if (!metadata.depends.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "§7依赖§f:", 20, y, 0xCCCCCC);
            y += 12;
            for (var entry : metadata.depends.entrySet()) {
                context.drawTextWithShadow(this.textRenderer, "  §7- " + entry.getKey() + " §f" + entry.getValue(), 20, y, 0xAAAAAA);
                y += 12;
            }
        }
        
        long packRecipeCount = AnvilApiMod.getRecipeLoader().getAllRecipes().stream()
            .filter(r -> r.getId().getNamespace().equals(namespace.getNamespace()))
            .count();
        context.drawTextWithShadow(this.textRenderer, "§7配方数量§f: " + packRecipeCount, 20, y, 0xCCCCCC);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
    }
}