import org.millenaire.common.pathing.FailedRouteBackoff;
import org.millenaire.common.pathing.NavigationRequestKey;

/**
 * Standalone Java 17 regression for NAV-OPT-16 without Forge dependencies:
 *
 * javac --release 17 -d build/nav-failure-smoke
 *   src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java
 *   src/main/java/org/millenaire/common/pathing/FailedRouteBackoff.java
 *   tools/tests/FailedRouteBackoffSmoke.java
 * java -cp build/nav-failure-smoke FailedRouteBackoffSmoke
 */
public final class FailedRouteBackoffSmoke {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static NavigationRequestKey key(int x, int y, int z,
            int goalX, int goalY, int goalZ, boolean allowDrop, int tolerance) {
        return new NavigationRequestKey(
                new NavigationRequestKey.Endpoint(x, y, z),
                new NavigationRequestKey.Endpoint(goalX, goalY, goalZ),
                new NavigationRequestKey.Movement(true, false, allowDrop,
                        false, true, false, tolerance, 0));
    }

    public static void main(String[] args) {
        var backoff = new FailedRouteBackoff();
        var stuck = key(26, 71, 647, 28, 66, 660, false, 0);
        var another = key(26, 71, 647, 29, 66, 660, false, 0);
        require(!backoff.shouldDefer(stuck, 100, 1), "no failure means immediate search");
        require(backoff.onFailed(stuck, 100, 1, 3) == 1, "first failure level 1");
        require(backoff.retryTick() == 123, "first retry 20+jitter ticks later");
        require(backoff.shouldDefer(stuck, 122, 1), "unchanged failure deferred");
        require(!backoff.shouldDefer(stuck, 123, 1), "boundary retry allowed");
        require(backoff.onFailed(stuck, 123, 1, 3) == 2, "second failure level 2");
        require(backoff.retryTick() == 166, "second retry 40+jitter ticks later");
        require(backoff.onFailed(stuck, 166, 1, 3) == 3, "third failure level 3");
        require(backoff.retryTick() == 249, "third retry 80+jitter ticks later");
        require(backoff.onFailed(stuck, 249, 1, 3) == 3, "backoff is capped");
        require(backoff.retryTick() == 332, "capped retry interval is 83 ticks");
        require(!backoff.shouldDefer(another, 250, 1), "different goal bypasses old failure");
        backoff.onRequest(another);
        require(backoff.level() == 0, "changing request resets streak");

        backoff.onFailed(stuck, 400, 8, 0);
        require(!backoff.shouldDefer(key(25, 71, 647, 28, 66, 660, false, 0), 401, 8),
                "changed start must bypass");
        require(!backoff.shouldDefer(key(26, 71, 647, 28, 66, 660, true, 0), 401, 8),
                "changed movement permission must bypass");
        require(!backoff.shouldDefer(key(26, 71, 647, 28, 66, 660, false, 1), 401, 8),
                "changed path tolerance must bypass");
        require(backoff.shouldDefer(stuck, 401, 8), "same full route deferred");

        require(!backoff.shouldDefer(stuck, 402, 9),
                "known block mutation must wake blocked route immediately");
        require(backoff.level() == 0, "known edit resets failure streak");
        require(backoff.onFailed(stuck, 403, 9, 0) == 1,
                "new failure after mutation starts at base delay");
        require(!backoff.shouldDefer(stuck, 100, 9),
                "world time rewind must not lock NPC in a future cooldown");

        require(backoff.onFailed(stuck, 500, 10, 6) == 1,
                "new world mutation epoch restarts from base delay");
        require(backoff.retryTick() == 526, "maximum allowed jitter");
        backoff.clear();
        require(!backoff.shouldDefer(stuck, 501, 10),
                "successful route clears any failure suppression");
        require(backoff.level() == 0, "success resets backoff level");

        boolean invalidJitter = false;
        try { backoff.onFailed(stuck, 600, 10, 7); }
        catch (IllegalArgumentException expected) { invalidJitter = true; }
        require(invalidJitter, "invalid jitter rejected");
        System.out.println("PASS: bounded no-path retry, exact identity, mutation wakeup and success reset");
    }
}
