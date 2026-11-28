package org.millenaire.common.pathing;

import java.util.Objects;

/**
 * Diagnostic-only detector of moving-target path-search bursts for one planner.
 *
 * A burst means >=5 destination changes among submitted searches within
 * 20 game ticks, while the start, movement policy and semantic goal remain
 * identical. This never postpones a search or changes route priority.
 *
 * One tiny state object per villager avoids process-wide maps and GC churn.
 */
public final class NavigationGoalDriftWindow {
    private static final long WINDOW_TICKS = 20L;
    private static final int BURST_THRESHOLD = 5;

    public record Sample(boolean goalChanged, boolean burstStarted, int changes) {
        public static final Sample NONE = new Sample(false, false, 0);
    }

    private NavigationRequestKey.Endpoint start;
    private NavigationRequestKey.Endpoint firstGoal;
    private NavigationRequestKey.Endpoint lastGoal;
    private NavigationRequestKey.Movement movement;
    private String semanticGoal;
    private long startedTick;
    private int changes;
    private boolean reported;

    public Sample onSearch(long tick, NavigationRequestKey request, String goal) {
        Objects.requireNonNull(request, "request");
        String normalizedGoal = goal == null ? "unknown" : goal;
        if (start == null || tick < startedTick || tick - startedTick >= WINDOW_TICKS
                || !start.equals(request.start())
                || !movement.equals(request.movement())
                || !semanticGoal.equals(normalizedGoal)) {
            start = request.start();
            firstGoal = request.goal();
            lastGoal = request.goal();
            movement = request.movement();
            semanticGoal = normalizedGoal;
            startedTick = tick;
            changes = 0;
            reported = false;
            return Sample.NONE;
        }
        if (lastGoal.equals(request.goal())) return Sample.NONE;
        lastGoal = request.goal();
        changes++;
        boolean firstBurst = changes >= BURST_THRESHOLD && !reported;
        if (firstBurst) reported = true;
        return new Sample(true, firstBurst, changes);
    }

    public NavigationRequestKey.Endpoint firstGoal() { return firstGoal; }
    public NavigationRequestKey.Endpoint lastGoal() { return lastGoal; }
    public int changes() { return changes; }
    public long startedTick() { return startedTick; }
}
