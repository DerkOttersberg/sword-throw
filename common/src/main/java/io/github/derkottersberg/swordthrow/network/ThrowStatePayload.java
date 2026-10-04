package io.github.derkottersberg.swordthrow.network;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative pose state broadcast to the thrower and tracking clients. */
public record ThrowStatePayload(int playerEntityId, Phase phase, int chargeTicks) {
    public static final ResourceLocation ID = SwordThrow.id("throw_state");
    public static final LegacyCodec STREAM_CODEC = new LegacyCodec();
    public static final class LegacyCodec {
        public void encode(FriendlyByteBuf buffer, ThrowStatePayload value) {
            buffer.writeVarInt(value.playerEntityId);
            buffer.writeByte(value.phase.ordinal());
            buffer.writeVarInt(value.chargeTicks);
        }
        public ThrowStatePayload decode(FriendlyByteBuf buffer) {
            return new ThrowStatePayload(buffer.readVarInt(), Phase.byOrdinal(buffer.readUnsignedByte()), buffer.readVarInt());
        }
    }

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
