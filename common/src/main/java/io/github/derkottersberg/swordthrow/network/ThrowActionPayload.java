package io.github.derkottersberg.swordthrow.network;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A charge lifecycle message. The server never treats the client tick count as authoritative. */
public record ThrowActionPayload(Action action, int clientChargeTicks) implements CustomPacketPayload {
    public static final Type<ThrowActionPayload> TYPE = new Type<>(SwordThrow.id("throw_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowActionPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeByte(value.action.ordinal());
            buffer.writeVarInt(value.clientChargeTicks);
        },
        buffer -> new ThrowActionPayload(Action.byOrdinal(buffer.readUnsignedByte()), buffer.readVarInt())
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
