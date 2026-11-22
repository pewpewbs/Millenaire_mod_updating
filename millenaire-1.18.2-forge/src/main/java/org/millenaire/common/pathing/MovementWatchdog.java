package org.millenaire.common.pathing;

/** Tracks physical movement, independently of replacement path objects. */
public final class MovementWatchdog {
    public enum Action { NONE, REPATH, RECOVER }
    private Object target;
    private double x, y, z;
    private long since, nextRetry;
    public void reset() { target = null; }
    public Action sample(long now, Object destination, double px, double py, double pz) {
        double dx=px-x, dy=py-y, dz=pz-z;
        if (target == null || !target.equals(destination) || now < since
                || dx*dx+dy*dy+dz*dz >= 0.25D) {
            target=destination; x=px; y=py; z=pz; since=now; nextRetry=now+80;
            return Action.NONE;
        }
        if (now-since >= 240) {
            since=now; nextRetry=now+80;
            return Action.RECOVER;
        }
        if (now >= nextRetry) { nextRetry=now+80; return Action.REPATH; }
        return Action.NONE;
    }
}
