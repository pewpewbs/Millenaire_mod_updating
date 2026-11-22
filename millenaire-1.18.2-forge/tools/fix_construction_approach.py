from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java'
s=p.read_text(); backup=r/'migration/before-construction-approach';backup.mkdir(exist_ok=False);shutil.copy2(p,backup/p.name)
old='''                AStarNode destNode = null;
                AStarNode[] possibles = AStarStatic.getAccessNodesSorted(this.level, this.doubleToInt(this.getX()), this.doubleToInt(this.getY()), this.doubleToInt(this.getZ()), this.getPathDestPoint().getiX(), this.getPathDestPoint().getiY(), this.getPathDestPoint().getiZ(), this.getPathingConfig());
                if (possibles.length != 0) {
                    destNode = possibles[0];
                }'''
new='''                AStarNode destNode = null;
                if (this.goalKey != null && Goal.goals.get(this.goalKey) == Goal.construction) {
                    // Building search already accepts a walkable node near the target,
                    // including ground below a canopy. Do not offset that tolerance
                    // around a second access point outside the builder's working reach.
                    destNode = new AStarNode(dest.getiX(), dest.getiY(), dest.getiZ());
                } else {
                    AStarNode[] possibles = AStarStatic.getAccessNodesSorted(this.level, this.doubleToInt(this.getX()), this.doubleToInt(this.getY()), this.doubleToInt(this.getZ()), this.getPathDestPoint().getiX(), this.getPathDestPoint().getiY(), this.getPathDestPoint().getiZ(), this.getPathingConfig());
                    if (possibles.length != 0) {
                        destNode = possibles[0];
                    }
                }'''
assert s.count(old)==1;s=s.replace(old,new);p.write_text(s)
print('Construction paths now use the block as the tolerance centre.')
