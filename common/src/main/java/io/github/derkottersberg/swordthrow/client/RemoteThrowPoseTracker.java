package io.github.derkottersberg.swordthrow.client;

import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntPredicate;

/** Independent pose timelines for every remotely rendered player. */
final class RemoteThrowPoseTracker {
    static final int HEARTBEAT_TIMEOUT_TICKS = 40;
    static final int MISSING_ENTITY_GRACE_TICKS = 20;

    private final Map<Integer, RemotePoseState> poses = new HashMap<>();

    void apply(ThrowStatePayload payload, long clientTick) {
        switch (payload.phase()) {
            case CHARGING -> {
                RemotePoseState remote = poses.computeIfAbsent(
                    payload.playerEntityId(),
                    ignored -> new RemotePoseState()
                );
                if (!remote.charging) {
                    remote.pose.beginCharge();
                }
                remote.charging = true;
                remote.chargeTicks = ChargeMath.clampCharge(payload.chargeTicks());
                remote.lastUpdateTick = clientTick;
                remote.missingEntityTicks = 0;
                remote.pose.setChargeProgress(ChargeMath.progress(remote.chargeTicks));
            }
            case RELEASE -> {
                RemotePoseState remote = poses.computeIfAbsent(
                    payload.playerEntityId(),
                    ignored -> new RemotePoseState()
                );
                remote.charging = false;
                remote.chargeTicks = ChargeMath.clampCharge(payload.chargeTicks());
                remote.lastUpdateTick = clientTick;
                remote.missingEntityTicks = 0;
                remote.pose.releaseForward(ChargeMath.progress(remote.chargeTicks));
            }
            case CANCEL -> poses.remove(payload.playerEntityId());
        }
    }

    void tick(long clientTick, IntPredicate entityPresent) {
        Iterator<Map.Entry<Integer, RemotePoseState>> iterator = poses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RemotePoseState> entry = iterator.next();
            RemotePoseState remote = entry.getValue();

            if (entityPresent.test(entry.getKey())) {
                remote.missingEntityTicks = 0;
            } else {
                remote.missingEntityTicks++;
            }

            if (remote.charging) {
                remote.chargeTicks = Math.min(remote.chargeTicks + 1, ChargeMath.MAX_CHARGE_TICKS);
                remote.pose.setChargeProgress(ChargeMath.progress(remote.chargeTicks));
            }
            remote.pose.tick();

            boolean heartbeatExpired = remote.charging
                && clientTick - remote.lastUpdateTick > HEARTBEAT_TIMEOUT_TICKS;
            if (heartbeatExpired || remote.missingEntityTicks > MISSING_ENTITY_GRACE_TICKS) {
                iterator.remove();
            } else if (!remote.charging && remote.pose.isIdle()) {
                iterator.remove();
            }
        }
    }

    ThrowPoseState poseFor(int entityId) {
        RemotePoseState remote = poses.get(entityId);
        return remote == null || remote.pose.isIdle() ? null : remote.pose;
    }

    int size() {
        return poses.size();
    }

    void remove(int entityId) {
        poses.remove(entityId);
    }

    void clear() {
        poses.clear();
    }

    private static final class RemotePoseState {
        private final ThrowPoseState pose = new ThrowPoseState();
        private boolean charging;
        private int chargeTicks;
        private long lastUpdateTick;
        private int missingEntityTicks;
    }
}
