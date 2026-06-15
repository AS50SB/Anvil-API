package eab.anvilapi.gui;

import eab.anvilapi.AnvilApiMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class InfoScreen extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("textures/block/bookshelf.png");
    private final Screen parent;
    private List<Text> contentLines;
    
    public InfoScreen(Screen parent) {
        super(Text.translatable("anvil_api.info_title"));
        this.parent = parent;
        this.contentLines = new ArrayList<>();
        loadContent();
    }
    
    private void loadContent() {
        String language = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        String langCode = language.split("_")[0];
        
        String path = "docs/info/" + langCode + ".txt";
        try (InputStream is = InfoScreen.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                path = "docs/info/en_us.txt";
                try (InputStream fallback = InfoScreen.class.getClassLoader().getResourceAsStream(path)) {
                    if (fallback != null) {
                        readLines(fallback);
                    }
                }
            } else {
                readLines(is);
            }
        } catch (Exception e) {
            contentLines.add(Text.literal("§cFailed to load info content"));
            AnvilApiMod.LOGGER.error("Failed to load info content", e);
        }
    }
    
    private void readLines(InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace('&', '§');
                contentLines.add(Text.literal(line));
            }
        }
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
        
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        
        int y = 35;
        for (Text line : contentLines) {
            context.drawTextWithShadow(this.textRenderer, line, 15, y, 0xCCCCCC);
            y += 12;
            if (y >= this.height - 40) break;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}