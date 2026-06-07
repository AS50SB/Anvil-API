package eab.anvilapi.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenGuiPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenGuiPacket> ID = new CustomPayload.Id<>(Identifier.of("anvil_api", "open_gui"));
    public static final PacketCodec<PacketByteBuf, OpenGuiPacket> CODEC = PacketCodec.of(OpenGuiPacket::write, OpenGuiPacket::new);
    
    private OpenGuiPacket(PacketByteBuf buf) {
        this();
    }
    
    private void write(PacketByteBuf buf) {
        // 无需写入数据
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}