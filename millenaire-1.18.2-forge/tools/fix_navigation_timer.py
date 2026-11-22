from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/entity/PathNavigateSimple.java'
s=p.read_text();needle='    protected boolean canUpdatePath() {'
assert s.count(needle)==1
s=s.replace(needle,'''    @Override
    public boolean moveTo(net.minecraft.world.level.pathfinder.Path requested, double speed) {
        if (requested != null && requested == this.path && !this.isDone()) {
            // Reusing an active route must not restart vanilla's stuck timer each tick.
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
    }

'''+needle)
p.write_text(s,encoding='utf-8')
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java';b=r/'migration/before-door-navigation-fix/MillVillager.java'
if not b.exists():shutil.copy2(p,b)
s=p.read_text();needle='''        if (this.pathEntity != null && this.pathEntity.getNodeCount() > 0 && !MillCommonUtilities.chanceOn(50)'''
assert s.count(needle)==1
s=s.replace(needle,'''        if (this.getNavigation().isStuck()) {
            this.pathEntity = null;
        }
'''+needle)
p.write_text(s,encoding='utf-8')
print('Repeated active route keeps its stuck timer; stuck villagers request a fresh route.')
