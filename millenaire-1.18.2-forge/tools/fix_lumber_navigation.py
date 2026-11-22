from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');b=r/'migration/before-lumber-navigation';b.mkdir(exist_ok=False)
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java';s=p.read_text();shutil.copy2(p,b/p.name)
s=s.replace('private boolean pathFailedSincelastTick = false;', 'private volatile boolean pathFailedSincelastTick = false;\n    private long nextVanillaPathAttempt = 0L;\n    private long nextPathFailureDiagnostic = 0L;')
s=s.replace('private List<AStarNode> pathCalculatedSinceLastTick = null;', 'private volatile List<AStarNode> pathCalculatedSinceLastTick = null;')
s=s.replace('goal != Goal.construction && this.pathEntity', 'goal != Goal.construction && !(goal instanceof org.millenaire.common.goal.GoalLumbermanChopTrees) && this.pathEntity')
s=s.replace('this.goalKey != null && Goal.goals.get(this.goalKey) == Goal.construction)', 'this.goalKey != null && (Goal.goals.get(this.goalKey) == Goal.construction\n                        || Goal.goals.get(this.goalKey) instanceof org.millenaire.common.goal.GoalLumbermanChopTrees))')
a=s.index('    private void pathFailedSinceLastTick() {');z=s.index('    public boolean performNightAction()',a)
s=s[:a]+'''    private void pathFailedSinceLastTick() {
        this.pathFailedSincelastTick = false;
        Point dest = this.getPathDestPoint();
        if (dest == null || this.stopMoving) return;
        // A failed background refresh must not interrupt a still usable route.
        if (this.pathEntity != null && !this.pathEntity.isDone()
                && !this.navigation.isDone() && !this.navigation.isStuck()) return;
        long now = this.level.getGameTime();
        if (now >= this.nextVanillaPathAttempt) {
            this.nextVanillaPathAttempt = now + 40L;
            this.navigation.stop();
            Goal goal = Goal.goals.get(this.goalKey);
            int range = goal == null ? 1 : Math.max(1, goal.range(this));
            Path fallback = this.navigation.createPath(dest.getBlockPos(), Math.min(2, range - 1));
            if (fallback != null && fallback.canReach() && fallback.getNodeCount() > 0
                    && new Point(fallback.getEndNode()).horizontalDistanceTo(dest) < range) {
                Node[] nodes = new Node[fallback.getNodeCount()];
                for (int i = 0; i < nodes.length; i++) nodes[i] = fallback.getNode(i);
                AS_PathEntity adapted = new AS_PathEntity(nodes);
                if (this.navigation.moveTo(adapted, 0.5)) {
                    this.pathEntity = adapted;
                    this.pathFailureRecovery.reset();
                    return;
                }
            }
        }
        if (now >= this.nextPathFailureDiagnostic) {
            this.nextPathFailureDiagnostic = now + 200L;
            MillLog.major(this, "Navigation recovery: goal=" + this.goalKey
                    + " position=" + this.getPos() + " target=" + dest
                    + " distance=" + dest.horizontalDistanceTo((Entity)this));
        }
        if (this.pathFailureRecovery.failed(now)
            && (!this.vtype.noTeleport || this.getRecord() != null && this.getRecord().raidingVillage)) {
            this.jumpToDest();
        }
    }

'''+s[z:]
p.write_text(s)
p=r/'src/main/resources/todeploy/millenaire/languages/uk/strings.txt';s=p.read_text(encoding='utf-8');shutil.copy2(p,b/'uk-strings.txt')
assert 'goal.sleep_travelling=Пойдіть спати.' in s
s=s.replace('goal.sleep_travelling=Пойдіть спати.','goal.sleep_travelling=Йде спати').replace('goal.sleep=Спати\n','goal.sleep=Спить\n');p.write_text(s,encoding='utf-8')
(r/'src/main/resources/todeploy/millenaire/localization-version.txt').write_text('uk-ru-2026-11-17-1\n',encoding='utf-8')
print('Lumber approach, bounded vanilla recovery, failure diagnostics, sleep status translation updated.')
