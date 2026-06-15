package eab.anvilapi.gui;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.recipe.RecipeLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PackListScreen extends Screen {
    private final Screen parent;
    
    public PackListScreen(Screen parent) {
        super(Text.translatable("anvil_api.title"));
        this.parent = parent;
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
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        
        int y = 35;
        var namespaces = AnvilApiMod.getRecipeLoader().getLoadedNamespaces();
        
        if (namespaces.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable("anvil_api.no_recipes").getString(), 20, y, 0xCCCCCC);
        } else {
            context.drawTextWithShadow(this.textRenderer, "§e已加载的数据包:", 20, y, 0xCCCCCC);
            y += 15;
            
            for (var namespace : namespaces) {
                var metadata = AnvilApiMod.getRecipeLoader().getPackMetadata(namespace);
                if (metadata != null) {
                    String packName = metadata.packName.isEmpty() ? namespace.getNamespace() : metadata.packName;
                    context.drawTextWithShadow(this.textRenderer, "§7- §f" + packName + " §7(v" + metadata.packVersion + ")", 25, y, 0xAAAAAA);
                    y += 12;
                }
            }
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
    }
}