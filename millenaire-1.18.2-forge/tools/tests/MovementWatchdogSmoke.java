import org.millenaire.common.pathing.MovementWatchdog;

/**
 * Dependency-free regression smoke test for the physical-movement watchdog.
 *
 * Run from millenaire-1.18.2-forge:
 *   javac --release 17 -d build/watchdog-smoke src/main/java/org/millenaire/common/pathing/MovementWatchdog.java tools/tests/MovementWatchdogSmoke.java
 *   java -cp build/watchdog-smoke MovementWatchdogSmoke
 *
 * A full Forge compile and live Minecraft playtest are still required.
 */
public final class MovementWatchdogSmoke {
    private static void expect(MovementWatchdog.Action actual,
            MovementWatchdog.Action expected, String scenario) {
        if (actual != expected) {
            throw new AssertionError(scenario + ": expected " + expected + " but got " + actual);
        }
    }

    public static void main(String[] args) {
        MovementWatchdog watchdog = new MovementWatchdog();
        for (int tick = 0; tick <= 240; tick++) {
            double height = tick % 20 < 10
                    ? 64.1 + 0.7 * (tick % 10) / 9.0
                    : 64.8 - 0.7 * (tick % 10) / 9.0;
            MovementWatchdog.Action expected = tick == 80
                    ? MovementWatchdog.Action.REPATH
                    : tick == 160 ? MovementWatchdog.Action.DETOUR
                    : tick == 240 ? MovementWatchdog.Action.RECOVER
                    : MovementWatchdog.Action.NONE;
            expect(watchdog.sample(tick, "builder-stock", 0, height, 0, false),
                    expected, "stationary jumping at tick " + tick);
        }

        watchdog.reset();
        expect(watchdog.sample(0, "target", 0, 64, 0, false),
                MovementWatchdog.Action.NONE, "initial observation");
        expect(watchdog.sample(80, "target", 0.6, 64, 0, false),
                MovementWatchdog.Action.NONE, "horizontal movement resets timer");
        expect(watchdog.sample(150, "target", 0.6, 64, 0, false),
                MovementWatchdog.Action.NONE, "80-tick delay since progress");
        expect(watchdog.sample(160, "target", 0.6, 64, 0, false),
                MovementWatchdog.Action.REPATH, "repath only after new stall interval");

        watchdog.reset();
        watchdog.sample(0, "ladder", 0, 64, 0, true);
        expect(watchdog.sample(80, "ladder", 0, 65, 0, true),
                MovementWatchdog.Action.NONE, "real climb resets timer");
        expect(watchdog.sample(160, "ladder", 0, 66, 0, true),
                MovementWatchdog.Action.NONE, "continued climbing");

        watchdog.reset();
        watchdog.sample(0, "first-target", 0, 64, 0, false);
        expect(watchdog.sample(79, "new-target", 0, 64, 0, false),
                MovementWatchdog.Action.NONE, "new goal resets observation");
        System.out.println("PASS: jump loop, horizontal/climbing progress and retarget");
    }
}
