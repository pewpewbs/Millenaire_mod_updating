package org.millenaire.common.goal;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.common.entity.MillVillager;

/** On-demand observation of real AI decisions. No extra goal checks or random draws. */
public final class GoalDiagnostics {
    private GoalDiagnostics() {}
    private static final int MAX_ROWS = 96, MAX_NOTES = 4, MAX_NOTE_LENGTH = 180;
    private static final long WATCH_TICKS = 1200;
    private static final Map<MillVillager, Watch> WATCHES = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> REQUESTS = new WeakHashMap<>();
    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();
    private static final class Watch {
        long until;
        Snapshot snapshot;
        String destinationGoal, destinationResult;
        long destinationTick;
    }
    private record Row(String key, boolean possible, Integer priority, List<String> notes) {}
    private record Snapshot(long tick, String selected, List<Row> rows) {}
    private static final class Candidate {
        final String key;
        boolean possible;
        Integer priority;
        final List<String> notes = new ArrayList<>();
        Candidate(String key) { this.key = key; }
        Row freeze() { return new Row(key, possible, priority, List.copyOf(notes)); }
    }

    /** Request limit per connected operator. Called only on the logical server. */
    public static synchronized boolean allowRequest(ServerPlayer player) {
        if (player == null || !player.hasPermissions(2)) return false;
        long now = player.level.getGameTime();
        Long last = REQUESTS.get(player);
        if (last != null && now >= last && now - last < 10) return false;
        REQUESTS.put(player, now);
        return true;
    }
    /** Read the last observed selection; arm observation for subsequent natural decisions. */
    public static synchronized List<String> watchAndReport(ServerPlayer player, MillVillager villager) {
        if (player == null || !player.hasPermissions(2) || villager == null
                || villager.level != player.level || !villager.isAlive()
                || player.distanceToSqr(villager) > 256.0) return List.of("DENIED_OR_OUT_OF_RANGE");
        long now = villager.level.getGameTime();
        Watch watch = WATCHES.get(villager);
        if (watch == null || now > watch.until) {
            watch = new Watch();
            WATCHES.put(villager, watch);
        }
        watch.until = now + WATCH_TICKS;
        List<String> result = new ArrayList<>();
        result.add("NPC: " + villager.getName().getString());
        result.add("CURRENT: " + (villager.goalKey == null ? "-" : villager.goalKey));
        result.add("CURRENT_DESTINATION: " + (villager.getGoalDestPoint() == null ? "-" : villager.getGoalDestPoint().toString()));
        if (watch.destinationGoal != null && watch.destinationGoal.equals(villager.goalKey))
            result.add("LAST_DESTINATION: " + watch.destinationResult + " | age=" + Math.max(0, now-watch.destinationTick) + " ticks");
        Snapshot snapshot = watch.snapshot;
        if (snapshot == null) {
            result.add("WAITING_FOR_NEXT_NATURAL_GOAL_SELECTION");
            for (Goal goal : villager.getGoals()) {
                if (result.size() >= MAX_ROWS + 3) break;
                result.add(goal.key + " | NOT_EVALUATED");
            }
            return result;
        }
        result.add("LAST_SELECTION: " + snapshot.selected + " | age=" + Math.max(0, now-snapshot.tick) + " ticks");
        for (Row row : snapshot.rows) {
            result.add(row.key + " | " + (row.possible ? "AVAILABLE" : "BLOCKED")
                    + (row.priority != null ? " | last priority=" + row.priority : ""));
            if (!row.possible) for (String note : row.notes) result.add("  " + note);
        }
        return result;
    }

    /** Destination selection is a separate natural AI phase, not a reachability test. */
    public static synchronized void destination(MillVillager villager, Goal goal, Goal.GoalInformation info) {
        if (villager.level.isClientSide) return;
        Watch watch = WATCHES.get(villager);
        if (watch == null || villager.level.getGameTime() > watch.until) return;
        watch.destinationGoal = goal.key;
        watch.destinationTick = villager.level.getGameTime();
        if (info == null || (info.getDest() == null && info.getTargetEnt() == null)) {
            watch.destinationResult = goal instanceof org.millenaire.common.goal.generic.GoalGenericMining
                    ? "NO_REGISTERED_MATCHING_LIVE_SOURCE" : "NO_DESTINATION";
        } else {
            watch.destinationResult = info.getDest() != null ? info.getDest().toString() : "ENTITY_TARGET";
        }
    }

    public static synchronized Session begin(MillVillager villager) {
        if (villager.level.isClientSide) return null;
        Watch watch = WATCHES.get(villager);
        if (watch == null) return null;
        if (villager.level.getGameTime() > watch.until) { WATCHES.remove(villager); return null; }
        if (CURRENT.get() != null) return null;
        Session session = new Session(villager, watch);
        CURRENT.set(session);
        return session;
    }
    public static final class Session implements AutoCloseable {
        private final MillVillager villager;
        private final Watch watch;
        private final Map<Goal, Candidate> candidates = new LinkedHashMap<>();
        private Goal activeGoal;
        private Candidate active;
        private Session(MillVillager villager, Watch watch) { this.villager=villager; this.watch=watch; }
        public void candidate(Goal goal) {
            activeGoal = goal;
            active = candidates.get(goal);
            if (active == null && candidates.size() < MAX_ROWS) {
                active = new Candidate(goal.key);
                candidates.put(goal, active);
            }
        }
        public void possible(boolean possible) {
            if (active == null) return;
            active.possible = possible;
            if (!possible && active.notes.isEmpty()) active.notes.add("SPECIFIC_CONDITION_FALSE (not instrumented)");
        }
        public void priority(Goal goal, int value) {
            Candidate row = candidates.get(goal);
            if (row != null) row.priority = value;
        }
        public void finish(Goal selected) {
            List<Row> rows = candidates.values().stream().map(Candidate::freeze).toList();
            synchronized (GoalDiagnostics.class) {
                watch.snapshot = new Snapshot(villager.level.getGameTime(), selected == null ? "-" : selected.key, rows);
                watch.destinationGoal = null;
                watch.destinationResult = null;
            }
        }
        @Override public void close() { if (CURRENT.get() == this) CURRENT.remove(); }
    }
    public static boolean recording(MillVillager villager, Goal goal) {
        Session session = CURRENT.get();
        return session != null && session.villager == villager && session.activeGoal == goal && session.active != null;
    }
    public static void note(MillVillager villager, Goal goal, String reason) {
        if (!recording(villager, goal)) return;
        Candidate row = CURRENT.get().active;
        if (reason.length() > MAX_NOTE_LENGTH) reason = reason.substring(0, MAX_NOTE_LENGTH);
        if (row.notes.size() < MAX_NOTES && !row.notes.contains(reason)) row.notes.add(reason);
    }
    public static boolean reject(MillVillager villager, Goal goal, String reason) {
        note(villager, goal, reason);
        return false;
    }
}
