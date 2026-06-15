package eab.anvilapi.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenGuiPacket(String guiType) implements CustomPayload {
    public static final CustomPayload.Id<OpenGuiPacket> ID = new CustomPayload.Id<>(Identifier.of("anvil_api", "open_gui"));
    public static final PacketCodec<PacketByteBuf, OpenGuiPacket> CODEC = PacketCodec.of(OpenGuiPacket::write, OpenGuiPacket::new);
    
    public OpenGuiPacket(PacketByteBuf buf) {
        this(buf.readString());
    }
    
    private void write(PacketByteBuf buf) {
        buf.writeString(guiType);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}