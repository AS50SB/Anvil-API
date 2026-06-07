package eab.anvilapi.client;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.gui.ConfigScreen;
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
        
        // 注册网络包接收器，用于打开 GUI
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                ConfigScreen.open();
            });
        });
    }
}