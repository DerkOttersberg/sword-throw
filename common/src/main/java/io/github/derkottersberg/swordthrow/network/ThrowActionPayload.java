package io.github.derkottersberg.swordthrow.network;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

/** A charge lifecycle message. The server never treats the client tick count as authoritative. */
public record ThrowActionPayload(Action action, int clientChargeTicks) implements CustomPacketPayload {
    public static final Type<ThrowActionPayload> ID = new Type<>(SwordThrow.id("throw_action"));
    public static final LegacyCodec STREAM_CODEC = new LegacyCodec();
    public static final class LegacyCodec implements StreamCodec<FriendlyByteBuf, ThrowActionPayload> {
        public void encode(FriendlyByteBuf buffer, ThrowActionPayload value) {
            buffer.writeByte(value.action.ordinal());
            buffer.writeVarInt(value.clientChargeTicks);
        }
        public ThrowActionPayload decode(FriendlyByteBuf buffer) {
            return new ThrowActionPayload(Action.byOrdinal(buffer.readUnsignedByte()), buffer.readVarInt());
        }
    }

    @Override
    public Type<ThrowActionPayload> type() { return ID; }

    public ThrowActionPayload {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
    }

    public static ThrowActionPayload start() {
        return new ThrowActionPayload(Action.START, 0);
    }

    public static ThrowActionPayload cancel() {
        return new ThrowActionPayload(Action.CANCEL, 0);
    }

    public static ThrowActionPayload release(int clientChargeTicks) {
        return new ThrowActionPayload(Action.RELEASE, clientChargeTicks);
    }


    public enum Action {
        START,
        CANCEL,
        RELEASE;

        static Action byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : CANCEL;
        }
    }
}
