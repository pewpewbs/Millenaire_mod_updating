package org.millenaire.common.pathing;

/**
 * Immutable identity for an unfinished path-search request.
 *
 * Both endpoints and ALL movement options are part of the key. It is
 * intentionally independent of Minecraft classes so request coalescing can
 * be smoke-tested with plain Java 17. It does not license reusing a finished
 * path: that still requires live-world directed-edge validation.
 */
public record NavigationRequestKey(Endpoint start, Endpoint goal, Movement movement) {
    public record Endpoint(int x, int y, int z) {}

    public record Movement(
            boolean canUseDoors,
            boolean canTakeDiagonals,
            boolean allowDropping,
            boolean canSwim,
            boolean canClearLeaves,
            boolean tolerance,
            int toleranceHorizontal,
            int toleranceVertical) {}

    public NavigationRequestKey {
        java.util.Objects.requireNonNull(start, "start");
        java.util.Objects.requireNonNull(goal, "goal");
        java.util.Objects.requireNonNull(movement, "movement");
    }
}
