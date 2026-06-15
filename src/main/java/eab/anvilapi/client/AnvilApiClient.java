package eab.anvilapi.client;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.gui.ConfigScreen;
import eab.anvilapi.gui.InfoScreen;
import eab.anvilapi.gui.PackListScreen;
import eab.anvilapi.network.OpenGuiPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class AnvilApiClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AnvilApiMod.LOGGER.info("Initializing Anvil API Client");
        
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                switch (payload.guiType()) {
                    case "config":
                        ConfigScreen.open();
                        break;
                    case "info":
                        context.client().setScreen(new InfoScreen(context.client().currentScreen));
                        break;
                    case "packlist":
                        context.client().setScreen(new PackListScreen(context.client().currentScreen));
                        break;
                }
            });
        });
    }
}