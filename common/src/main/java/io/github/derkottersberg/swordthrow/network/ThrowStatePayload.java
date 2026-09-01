package io.github.derkottersberg.swordthrow.network;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-authoritative pose state broadcast to the thrower and tracking clients. */
public record ThrowStatePayload(int playerEntityId, Phase phase, int chargeTicks)
    implements CustomPacketPayload {
    public static final Type<ThrowStatePayload> TYPE = new Type<>(SwordThrow.id("throw_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowStatePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeVarInt(value.playerEntityId);
            buffer.writeByte(value.phase.ordinal());
            buffer.writeVarInt(value.chargeTicks);
        },
        buffer -> new ThrowStatePayload(
            buffer.readVarInt(),
            Phase.byOrdinal(buffer.readUnsignedByte()),
            buffer.readVarInt()
        )
    );

    public ThrowStatePayload {
        if (phase == null) {
            throw new IllegalArgumentException("phase cannot be null");
        }
        chargeTicks = Math.max(0, chargeTicks);
    }

    public static ThrowStatePayload charging(int playerEntityId, int chargeTicks) {
        return new ThrowStatePayload(playerEntityId, Phase.CHARGING, chargeTicks);
    }

    public static ThrowStatePayload release(int playerEntityId, int chargeTicks) {
        return new ThrowStatePayload(playerEntityId, Phase.RELEASE, chargeTicks);
    }

    public static ThrowStatePayload cancel(int playerEntityId) {
        return new ThrowStatePayload(playerEntityId, Phase.CANCEL, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Phase {
        CHARGING,
        RELEASE,
        CANCEL;

        static Phase byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : CANCEL;
        }
    }
}
