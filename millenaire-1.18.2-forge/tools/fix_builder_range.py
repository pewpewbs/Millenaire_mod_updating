from pathlib import Path
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java';s=p.read_text()
old='''            target = this.getPathDestPoint();
            if (this.pathEntity != null && this.pathEntity.getNodeCount() > 0) {'''
new='''            target = this.getPathDestPoint();
            // Construction reach is measured to the block, not an alternate access node.
            if (goal != Goal.construction && this.pathEntity != null && this.pathEntity.getNodeCount() > 0) {'''
assert s.count(old)==1;s=s.replace(old,new)
old='(double)(goal = Goal.goals.get(this.goalKey)).range(this) >= this.getPos().horizontalDistanceTo(this.getPathDestPoint())'
new='(double)(goal = Goal.goals.get(this.goalKey)).range(this) > this.getPos().horizontalDistanceTo(this.getPathDestPoint())'
assert s.count(old)==1;s=s.replace(old,new);p.write_text(s)
print('Aligned construction action target and strict pathfinding reach boundary')
