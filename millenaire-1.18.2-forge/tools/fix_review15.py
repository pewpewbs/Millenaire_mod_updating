from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');b=r/'migration/before-review15';b.mkdir(exist_ok=False)
base=r/'src/main/java/org/millenaire';res=r/'src/main/resources/todeploy/millenaire'
def edit(p,fn):
 d=b/p.relative_to(r);d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,d);p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def keys(s,changes):
 found=set();lines=[]
 for line in s.splitlines():
  k=line.split('=',1)[0]
  if k in changes:line=k+'='+changes[k];found.add(k)
  lines.append(line)
 assert found==set(changes),(set(changes)-found)
 return '\n'.join(lines)+'\n'
edit(res/'languages/uk/strings.txt',lambda s:keys(s,{
 'ui.possiblehousing':'Доступні житлові будівлі',
 'ui.cropnotenoughmoney':'На навчання вирощуванню культури «<0>» бракує <1>.',
 'ui.control_noreputation':'Вам поки бракує репутації, щоб очолити поселення цієї культури (<0>). Підвищуйте репутацію або виконуйте особливі завдання, щоб здобути цей статус.',
 'ui.panelwallnowalls':'У цьому селі немає стін.',
 'panels.offense':'Напад: <0>','panels.offenselong':'Напад: <0>','panels.defenselong':'Захист: <0>',
 'goal.construction':'Будує','goal.childobserve':'Спостерігає за дорослими',
 'other.villagerattackinglabel':'Атакує. Ціль: <0>'}))
edit(res/'languages/uk/japanese_strings.txt',lambda s:keys(s,{'villager.japanese_farmer':'Фермер','villager.japanese_samuraigeneral':'Володар','villager.japanese_bravewoman':'Воїтелька','shop.fishery':'Готові страви'}))
edit(res/'languages/uk/japanese_sentences.txt',lambda s:s.replace('Зберігайте зброю!','До зброї!'))
edit(res/'languages/uk/japanese_buildings.txt',lambda s:s.replace('=Гай\n','=Лісова ділянка\n'))
edit(res/'localization-version.txt',lambda s:'uk-ru-2026-11-17-4\n')
# Keep Minecraft's terrain heights, obstacle tests and door handling instead of legacy shortcuts.
edit(base/'common/entity/PathNavigateSimple.java',lambda s:'''package org.millenaire.common.entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
public class PathNavigateSimple extends GroundPathNavigation {
    public PathNavigateSimple(Mob mob, Level level) {
        super(mob, level);
        setCanOpenDoors(true);
        setCanPassDoors(true);
        setCanFloat(true);
    }
    @Override public boolean moveTo(Path requested, double speed) {
        if (requested != null && requested == this.path && !isDone()) {
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
    }
}
''')
for n in ['BlockPath.java','BlockPathSlab.java']:
 def block(s):
  at='    @Override public boolean useShapeForLightOcclusion'
  assert at in s
  return s.replace(at,'''    @Override public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.level.pathfinder.PathComputationType type) { return false; }
'''+at)
 edit(base/'common/block'/n,block)
def villager(s):
 a=s.index('        if (now >= this.nextVanillaPathAttempt) {',s.index('    private void pathFailedSinceLastTick()'))
 z=s.index('        if (now >= this.nextPathFailureDiagnostic)',a)
 body=s[a:z].replace('                    return;','                    return true;')
 helper='''    private boolean tryGroundRoute(Point dest) {
        long now = this.level.getGameTime();
'''+body+'''        return false;
    }

'''
 s=s[:a]+'        if (this.tryGroundRoute(dest)) return;\n'+s[z:]
 s=s.replace('    private void pathFailedSinceLastTick() {',helper+'    private void pathFailedSinceLastTick() {')
 old='''                if (this.pathPlannerJPS.isBusy()) {
                    this.pathPlannerJPS.stopPathSearch(true);
                }'''
 assert s.count(old)==1;s=s.replace(old,'''                if (this.tryGroundRoute(dest)) return null;
'''+old)
 s=s.replace(' && !MillCommonUtilities.chanceOn(50) && this.pathEntity.getCurrentTargetPathPoint() != null',' && this.pathEntity.getCurrentTargetPathPoint() != null')
 return s
edit(base/'common/entity/MillVillager.java',villager)
edit(base/'common/goal/GoalSleep.java',lambda s:s.replace('return this.key + "_travelling";', 'return villager != null && villager.isVillagerSleeping() ? this.key : this.key + "_travelling";'))
# Both slow survival mining and deliberate creative-mode removal.
edit(base/'common/block/BlockPanel.java',lambda s:s.replace('.strength(1.0F)', '.strength(20.0F, 1200.0F)'))
def event(s):
 marker='public class MillEventController {'
 return s.replace(marker,marker+'''
    @SubscribeEvent
    public void protectPanelClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().getBlockState(event.getPos()).getBlock() instanceof org.millenaire.common.block.BlockPanel
                && !event.getPlayer().isShiftKeyDown()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void protectPanelBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof org.millenaire.common.block.BlockPanel
                && !event.getPlayer().isShiftKeyDown()) event.setCanceled(true);
    }
''')
edit(base/'common/forge/MillEventController.java',event)
# Occupation is not the current job; display both instead of relabelling the resident.
def mapui(s):
 marker='''            boolean gameString;
            int stringlength = this.font.width(villagerHover.func_70005_c_());'''
 new='''            boolean gameString;
            org.millenaire.common.goal.Goal activeGoal = org.millenaire.common.goal.Goal.goals.get(villagerHover.goalKey);
            String taskLabel = activeGoal == null ? "" : activeGoal.gameName(villagerHover);
            int stringlength = Math.max(this.font.width(taskLabel), this.font.width(villagerHover.func_70005_c_()));'''
 assert s.count(marker)==1;s=s.replace(marker,new)
 a=s.index('        if (villagerHover != null) {');z=s.index('        } else if (playerHover != null)',a)
 part=s[a:z].replace('j + 10 + 33 - yStart','j + 10 + (taskLabel.isEmpty() ? 33 : 44) - yStart').replace('j + 10 + 22 - yStart','j + 10 + (taskLabel.isEmpty() ? 22 : 33) - yStart')
 part+='''            if (!taskLabel.isEmpty()) this.font.draw(framePose, taskLabel, i + 10 - xStart,
                    j + 10 - yStart + (gameString ? 33 : 22), 0xB0B060);
'''
 return s[:a]+part+s[z:]
edit(base/'client/gui/text/GuiPanelParchment.java',mapui)
print('Updated translations, terrain navigation, sleeping label, map task and protected panels.')
