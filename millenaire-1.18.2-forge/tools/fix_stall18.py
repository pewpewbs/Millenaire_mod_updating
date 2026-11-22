from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
base=r/'src/main/java/org/millenaire'
p=base/'common/pathing/MovementWatchdog.java'
assert not p.exists()
p.write_text('''package org.millenaire.common.pathing;

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
''',encoding='utf-8')
p=base/'common/entity/MillVillager.java'
b=r/'migration/before-stall18'/p.relative_to(r);b.parent.mkdir(parents=True,exist_ok=True);assert not b.exists();shutil.copy2(p,b)
s=p.read_text(encoding='utf-8')
anchor='    private long nextVanillaPathAttempt = 0L;'
assert s.count(anchor)==1
s=s.replace(anchor,anchor+'\n    private final org.millenaire.common.pathing.MovementWatchdog movementWatchdog = new org.millenaire.common.pathing.MovementWatchdog();')
anchor='''                if (this.getPathDestPoint() != null && !this.stopMoving) {
                    this.updatePathIfNeeded(this.getPathDestPoint());
                }'''
assert s.count(anchor)==1
s=s.replace(anchor,'''                if (this.getPathDestPoint() != null && !this.stopMoving) {
                    this.recoverStationaryMovement();
                    this.updatePathIfNeeded(this.getPathDestPoint());
                } else {
                    this.movementWatchdog.reset();
                }''')
anchor='    private boolean tryGroundRoute(Point dest) {'
assert s.count(anchor)==1
s=s.replace(anchor,'''    private void recoverStationaryMovement() {
        Goal goal = Goal.goals.get(this.goalKey);
        Point dest = this.getPathDestPoint();
        // Working, sleeping and fighting are not failed travel.
        if (this.getTarget() != null || this.isVillagerSleeping()
                || (goal != null && goal.isWithinActionRange(this, dest))) {
            this.movementWatchdog.reset();
            return;
        }
        var action = this.movementWatchdog.sample(this.level.getGameTime(),
                dest.getBlockPos(), this.getX(), this.getY(), this.getZ());
        if (action == org.millenaire.common.pathing.MovementWatchdog.Action.NONE) return;
        this.navigation.stop();
        this.pathEntity = null;
        this.nextVanillaPathAttempt = 0L;
        if (action == org.millenaire.common.pathing.MovementWatchdog.Action.RECOVER
                && this.vtype != null && (!this.vtype.noTeleport
                    || this.getRecord() != null && this.getRecord().raidingVillage)) {
            this.jumpToDest();
        }
    }

'''+anchor)
p.write_text(s,encoding='utf-8')
print('Physical movement watchdog added: retry 80 ticks, existing safe recovery after 240')
