import org.millenaire.common.pathing.NavigationRequestKey;
import org.millenaire.common.pathing.NavigationGoalDriftWindow;

/** Standalone Java 17 regression for the NAV-OPT-18 diagnostic window.
 * From millenaire-1.18.2-forge:
 * javac --release 17 -d build/nav-goal-drift-smoke
 *   src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java
 *   src/main/java/org/millenaire/common/pathing/NavigationGoalDriftWindow.java
 *   tools/tests/NavigationGoalDriftWindowSmoke.java
 * java -cp build/nav-goal-drift-smoke NavigationGoalDriftWindowSmoke
 */
public final class NavigationGoalDriftWindowSmoke {
    private static void check(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }

    private static NavigationRequestKey request(int x, int target, boolean drop) {
        return new NavigationRequestKey(
                new NavigationRequestKey.Endpoint(x, 66, 608),
                new NavigationRequestKey.Endpoint(target, 66, 600),
                new NavigationRequestKey.Movement(true, false, drop,
                        false, true, false, 0, 0));
    }

    public static void main(String[] args) {
        var drift = new NavigationGoalDriftWindow();
        check(!drift.onSearch(100, request(48, 45, false), "gosocialise").goalChanged(),
                "first submitted route starts the window");
        check(!drift.onSearch(101, request(48, 45, false), "gosocialise").goalChanged(),
                "same target must not count");
        for (int i = 1; i <= 4; i++) {
            var result = drift.onSearch(101 + i, request(48, 45 - i, false), "gosocialise");
            check(result.goalChanged() && result.changes() == i, "distinct target change " + i);
            check(!result.burstStarted(), "do not signal a burst before the threshold");
        }
        var fifth = drift.onSearch(106, request(48, 40, false), "gosocialise");
        check(fifth.burstStarted() && fifth.changes() == 5,
                "fifth distinct target change triggers one burst");
        check(!drift.onSearch(107, request(48, 39, false), "gosocialise").burstStarted(),
                "burst notification must not repeat in the window");
        check(drift.firstGoal().x() == 45 && drift.lastGoal().x() == 39,
                "window remembers first and most recent requested destinations");
        check(!drift.onSearch(108, request(48, 38, false), "construction").goalChanged(),
                "new semantic task must reset the detector");
        check(!drift.onSearch(109, request(49, 37, false), "construction").goalChanged(),
                "changed start must reset the detector");
        check(!drift.onSearch(110, request(49, 36, true), "construction").goalChanged(),
                "changed movement policy must reset the detector");
        check(!drift.onSearch(130, request(49, 35, true), "construction").goalChanged(),
                "20 tick interval expiry must reset");
        check(!drift.onSearch(10, request(49, 34, true), "construction").goalChanged(),
                "world tick rewind must reset");
        check(!drift.onSearch(11, request(49, 34, true), "construction").goalChanged(),
                "unchanged destination after rewind must not be counted");

        // This detector never decides whether a route may be computed.
        // It only returns diagnostic Sample values with no rejection API.
        System.out.println("PASS: moving-goal burst threshold, task isolation and time-window reset");
    }
}
