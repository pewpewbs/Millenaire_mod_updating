from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common'
p=base/'goal/Goal.java';s=p.read_text();shutil.copy2(p,r/'migration/before-delivery16/Goal.java')
needle='    public int range(MillVillager villager) {'
assert needle in s;s=s.replace(needle,'''    public boolean isWithinActionRange(MillVillager villager, Point destination) {
        return destination != null && destination.horizontalDistanceTo(villager) < this.range(villager);
    }

'''+needle);p.write_text(s)
p=base/'goal/GoalBringBackResourcesHome.java';s=p.read_text();needle='    @Override\n    public boolean performAction(MillVillager villager) {'
s=s.replace(needle,'''    @Override
    public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point destination) {
        return destination != null && destination.distanceTo(villager) < this.range(villager);
    }

'''+needle).replace('destination == null || destination.distanceTo(villager) >= this.range(villager)', '!this.isWithinActionRange(villager, destination)');p.write_text(s)
p=base/'entity/MillVillager.java';s=p.read_text()
old='target != null && target.horizontalDistanceTo((Entity)this) < (double)goal.range(this)) {\n            if (this.actionStart'
new='goal.isWithinActionRange(this, target)) {\n            if (this.actionStart'
assert old in s;s=s.replace(old,new)
old='(double)(goal = Goal.goals.get(this.goalKey)).range(this) > this.getPos().horizontalDistanceTo(this.getPathDestPoint())'
assert old in s;s=s.replace(old,'(goal = Goal.goals.get(this.goalKey)).isWithinActionRange(this, this.getPathDestPoint())');p.write_text(s)
print('Delivery approach, work trigger and deposit now share the same 3D reach check.')
