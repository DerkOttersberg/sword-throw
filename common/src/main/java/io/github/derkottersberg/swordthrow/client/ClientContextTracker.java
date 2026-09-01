package io.github.derkottersberg.swordthrow.client;

/**
 * Tracks client world/player identity by reference so a same-world respawn is
 * distinguishable from an ordinary tick.
 */
final class ClientContextTracker<L, P> {
    private L level;
    private P player;

    Change update(L nextLevel, P nextPlayer) {
        Change change;
        if (nextLevel != level) {
            change = Change.LEVEL_CHANGED;
        } else if (nextPlayer != player) {
            change = Change.PLAYER_CHANGED;
        } else {
            change = Change.NONE;
        }
        level = nextLevel;
        player = nextPlayer;
        return change;
    }

    P player() {
        return player;
    }

    enum Change {
        NONE,
        PLAYER_CHANGED,
        LEVEL_CHANGED
    }
}
