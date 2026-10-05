package io.github.derkottersberg.swordthrow.network;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

/** Server-authoritative pose state broadcast to the thrower and tracking clients. */
public record ThrowStatePayload(int playerEntityId, Phase phase, int chargeTicks) implements CustomPacketPayload {
    public static final Type<ThrowStatePayload> ID = new Type<>(SwordThrow.id("throw_state"));
    public static final LegacyCodec STREAM_CODEC = new LegacyCodec();
    public static final class LegacyCodec implements StreamCodec<FriendlyByteBuf, ThrowStatePayload> {
        public void encode(FriendlyByteBuf buffer, ThrowStatePayload value) {
            buffer.writeVarInt(value.playerEntityId);
            buffer.writeByte(value.phase.ordinal());
            buffer.writeVarInt(value.chargeTicks);
        }
        public ThrowStatePayload decode(FriendlyByteBuf buffer) {
            return new ThrowStatePayload(buffer.readVarInt(), Phase.byOrdinal(buffer.readUnsignedByte()), buffer.readVarInt());
        }
    }

    @Override
    public Type<ThrowStatePayload> type() { return ID; }

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


    public enum Phase {
        CHARGING,
        RELEASE,
        CANCEL;

        static Phase byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : CANCEL;
        }
    }
}
