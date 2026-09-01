package io.github.derkottersberg.swordthrow.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import org.junit.jupiter.api.Test;

class RemoteThrowPoseTrackerTest {
    @Test
    void keepsTwoRemotePlayersIndependentThroughReleaseAndCancel() {
        RemoteThrowPoseTracker tracker = new RemoteThrowPoseTracker();
        tracker.apply(ThrowStatePayload.charging(101, 30), 0L);
        tracker.apply(ThrowStatePayload.charging(202, 5), 0L);

        for (long tick = 1L; tick <= 4L; tick++) {
            tracker.tick(tick, ignored -> true);
        }

        ThrowPoseState first = tracker.poseFor(101);
        ThrowPoseState second = tracker.poseFor(202);
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(
            first.getChargeIndicatorProgress(1.0F) > second.getChargeIndicatorProgress(1.0F),
            "one player's charge must not overwrite another player's pose"
        );

        tracker.apply(ThrowStatePayload.release(101, 30), 4L);
        tracker.apply(ThrowStatePayload.cancel(202), 4L);
        assertNotNull(tracker.poseFor(101));
        assertNull(tracker.poseFor(202));

        for (long tick = 5L; tick <= 10L; tick++) {
            tracker.tick(tick, ignored -> true);
        }
        assertEquals(0, tracker.size(), "the completed release pose must expire");
    }

    @Test
    void acceptsLateTrackingHeartbeatAndPrunesStaleOrMissingPlayers() {
        RemoteThrowPoseTracker lateTracker = new RemoteThrowPoseTracker();
        lateTracker.apply(ThrowStatePayload.charging(303, 20), 100L);
        lateTracker.tick(101L, ignored -> true);
        assertNotNull(lateTracker.poseFor(303), "a late tracker must animate from the heartbeat snapshot");

        for (long tick = 102L; tick <= 141L; tick++) {
            lateTracker.tick(tick, ignored -> true);
        }
        assertNull(lateTracker.poseFor(303), "a charge without another server heartbeat must expire");

        RemoteThrowPoseTracker missingTracker = new RemoteThrowPoseTracker();
        missingTracker.apply(ThrowStatePayload.charging(404, 10), 0L);
        for (long tick = 1L; tick <= RemoteThrowPoseTracker.MISSING_ENTITY_GRACE_TICKS + 1L; tick++) {
            missingTracker.tick(tick, ignored -> false);
        }
        assertNull(missingTracker.poseFor(404), "an unloaded player must not leak a remote pose");
    }
}
