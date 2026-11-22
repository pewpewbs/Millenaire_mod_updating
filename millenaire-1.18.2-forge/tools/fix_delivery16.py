from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';b=r/'migration/before-delivery16';b.mkdir(exist_ok=False)
def edit(p,fn):
 q=b/p.name;shutil.copy2(p,q);p.write_text(fn(p.read_text()),encoding='utf-8')
def adapter(s):
 old='''    public AS_PathEntity(Node[] points) {
        super(new ArrayList<>(Arrays.asList(points.clone())),
                points.length == 0 ? BlockPos.ZERO : points[points.length - 1].asBlockPos(), points.length > 0);
        pointsCopy = points.clone();
    }'''
 new='''    public AS_PathEntity(Node[] points) {
        this(points, points.length == 0 ? BlockPos.ZERO : points[points.length - 1].asBlockPos(), points.length > 0);
    }
    public AS_PathEntity(Node[] points, BlockPos target, boolean reachesTarget) {
        super(new ArrayList<>(Arrays.asList(points.clone())), target, reachesTarget && points.length > 0);
        pointsCopy = points.clone();
    }'''
 assert old in s;return s.replace(old,new)
edit(base/'pathing/atomicstryker/AS_PathEntity.java',adapter)
def villager(s):
 old='if (goal != Goal.construction && !(goal instanceof org.millenaire.common.goal.GoalLumbermanChopTrees) && this.pathEntity != null && this.pathEntity.getNodeCount() > 0) {'
 new='''if (goal != Goal.construction && !(goal instanceof org.millenaire.common.goal.GoalLumbermanChopTrees)
                    && !(goal instanceof org.millenaire.common.goal.GoalBringBackResourcesHome)
                    && this.pathEntity != null && this.pathEntity.canReach() && this.pathEntity.getNodeCount() > 0) {'''
 assert old in s;s=s.replace(old,new)
 old='AS_PathEntity adapted = new AS_PathEntity(nodes);';assert s.count(old)==1
 return s.replace(old,'AS_PathEntity adapted = new AS_PathEntity(nodes, fallback.getTarget(), fallback.canReach());')
edit(base/'entity/MillVillager.java',villager)
def delivery(s):
 old='''    public boolean performAction(MillVillager villager) {
        for (InvItem key : villager.getInventoryKeys()) {'''
 new='''    public boolean performAction(MillVillager villager) {
        // Never deposit remotely at an intermediate route endpoint.
        org.millenaire.common.utilities.Point destination = villager.getGoalDestPoint();
        if (destination == null || destination.distanceTo(villager) >= this.range(villager)) return false;
        for (InvItem key : villager.getInventoryKeys()) {'''
 assert old in s;return s.replace(old,new)
edit(base/'goal/GoalBringBackResourcesHome.java',delivery)
print('Preserved partial-route target/reachability and guarded home delivery at actual destination.')
