import java.util.ArrayList;
import java.util.List;
import org.millenaire.common.pathing.FailedRouteBackoff;
import org.millenaire.common.pathing.NavigationRequestKey;

/**
 * Standalone Java 17 diagnostic-only regression for NAV-OPT-17.
 *
 * From millenaire-1.18.2-forge:
 * javac --release 17 -d build/nav-failure-events-smoke
 *   src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java
 *   src/main/java/org/millenaire/common/pathing/FailedRouteBackoff.java
 *   tools/tests/FailedRouteBackoffEventsSmoke.java
 * java -cp build/nav-failure-events-smoke FailedRouteBackoffEventsSmoke
 */
public final class FailedRouteBackoffEventsSmoke {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }

    private static NavigationRequestKey key(int start, int goal, boolean drop) {
        return new NavigationRequestKey(
                new NavigationRequestKey.Endpoint(start, 70, 20),
                new NavigationRequestKey.Endpoint(goal, 69, 25),
                new NavigationRequestKey.Movement(true, false, drop,
                        false, true, false, 0, 0));
    }

    public static void main(String[] args) {
        List<FailedRouteBackoff.Event> observed = new ArrayList<>();
        var backoff = new FailedRouteBackoff(observed::add);
        var original = key(10, 40, false);

        backoff.onRequest(original);
        backoff.onSuccess();
        check(observed.isEmpty(), "never count reset without a failed path");

        backoff.onFailed(original, 100, 1, 0);
        check(backoff.shouldDefer(original, 119, 1), "original backoff still applies");
        check(!backoff.shouldDefer(original, 120, 1), "expiry allows retry");
        check(!backoff.shouldDefer(original, 121, 1), "second expiry check also allows retry");
        check(observed.equals(List.of(FailedRouteBackoff.Event.RETRY_EXPIRED)),
                "one expiry event per failed search, not every game tick");
        check(backoff.level() == 1, "timer expiry never clears the failure streak");
        check(backoff.onFailed(original, 121, 1, 0) == 2,
                "second failure escalates despite prior retry expiry");
        check(backoff.retryTick() == 161, "second level retains 40-tick delay");

        backoff.onRequest(key(11, 40, false));
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_START_CHANGED,
                "changed start categorized separately");
        backoff.onFailed(original, 200, 1, 0);
        backoff.onRequest(key(10, 41, false));
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_GOAL_CHANGED,
                "changed goal categorized separately");
        backoff.onFailed(original, 300, 1, 0);
        backoff.onRequest(key(10, 40, true));
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_MOVEMENT_CHANGED,
                "changed movement policy categorized separately");

        backoff.onFailed(original, 400, 10, 0);
        check(!backoff.shouldDefer(original, 401, 11), "known edit wakes path");
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_WORLD_MUTATED,
                "world mutation is separately counted");
        check(backoff.level() == 0, "world edit removes old streak");
        backoff.onFailed(original, 500, 10, 0);
        check(backoff.onFailed(original, 501, 11, 0) == 1,
                "mutation during worker search starts a fresh streak");
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_WORLD_MUTATED,
                "mutation during in-flight search is not silently ignored");

        backoff.onFailed(original, 600, 12, 0);
        check(!backoff.shouldDefer(original, 599, 12), "tick rewind wakes path");
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_TICK_REWIND,
                "tick rewind gets its own diagnostic");

        backoff.onFailed(original, 700, 12, 0);
        backoff.onSuccess();
        check(observed.get(observed.size()-1) == FailedRouteBackoff.Event.RESET_SUCCESS,
                "success is counted only after a failed path");
        int before = observed.size();
        backoff.onSuccess();
        backoff.clear();
        check(observed.size() == before, "successful idle calls and manual clear are silent");
        check(backoff.onFailed(original, 800, 12, 0) == 1,
                "diagnostic instrumentation does not modify base delay");

        System.out.println("PASS: reason-specific resets, one-shot retry expiry, unchanged backoff levels");
    }
}
