package io.github.derkottersberg.swordthrow.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ClientContextTrackerTest {
    @Test
    void detectsSameLevelPlayerReplacementAsRespawnContextChange() {
        ClientContextTracker<Object, Object> tracker = new ClientContextTracker<>();
        Object level = new Object();
        Object firstPlayer = new Object();
        Object replacementPlayer = new Object();

        assertEquals(ClientContextTracker.Change.LEVEL_CHANGED, tracker.update(level, firstPlayer));
        assertEquals(ClientContextTracker.Change.NONE, tracker.update(level, firstPlayer));
        assertSame(firstPlayer, tracker.player());
        assertEquals(ClientContextTracker.Change.PLAYER_CHANGED, tracker.update(level, replacementPlayer));
        assertSame(replacementPlayer, tracker.player());
    }

    @Test
    void levelReplacementTakesPrecedenceAndDisconnectIsObserved() {
        ClientContextTracker<Object, Object> tracker = new ClientContextTracker<>();
        Object firstLevel = new Object();
        Object secondLevel = new Object();
        Object player = new Object();

        tracker.update(firstLevel, player);
        assertEquals(ClientContextTracker.Change.LEVEL_CHANGED, tracker.update(secondLevel, player));
        assertEquals(ClientContextTracker.Change.LEVEL_CHANGED, tracker.update(null, null));
        assertEquals(ClientContextTracker.Change.NONE, tracker.update(null, null));
    }
}
