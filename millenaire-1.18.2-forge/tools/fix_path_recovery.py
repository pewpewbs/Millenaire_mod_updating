from pathlib import Path
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/entity/PathFailureRecovery.java'
p.write_text('''package org.millenaire.common.entity;
/** Short-lived route failures should be retried before using emergency relocation. */
public final class PathFailureRecovery {
    private long firstFailure = -1;
    private int failures;
    public boolean failed(long tick) {
        if (firstFailure < 0 || tick < firstFailure) { firstFailure = tick; failures = 0; }
        failures++;
        if (failures >= 3 && tick - firstFailure >= 200) { reset(); return true; }
        return false;
    }
    public void reset() { firstFailure = -1; failures = 0; }
}
''',encoding='utf-8')
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java';s=p.read_text()
s=s.replace('private boolean pathFailedSincelastTick = false;', 'private boolean pathFailedSincelastTick = false;\n    private final PathFailureRecovery pathFailureRecovery = new PathFailureRecovery();')
old='''                            if (this.localStuck > 100) {
                                this.setPos((double)nextPoint.x + 0.5, (double)nextPoint.y + 0.5, (double)nextPoint.z + 0.5);
                                this.localStuck = 0;
                            }'''
assert old in s;s=s.replace(old,'')
old='''                            if (this.localStuck > 30) {
                                this.navigation.stop();
                                this.pathEntity = null;
                            }'''
assert old in s;s=s.replace(old,old.replace('this.pathEntity = null;', 'this.pathEntity = null;\n                                this.localStuck = 0;'))
old='''    private void pathFailedSinceLastTick() {
        if (!this.vtype.noTeleport || this.getRecord() != null && this.getRecord().raidingVillage) {'''
assert old in s;s=s.replace(old,'''    private void pathFailedSinceLastTick() {
        if (this.pathFailureRecovery.failed(this.level.getGameTime())
            && (!this.vtype.noTeleport || this.getRecord() != null && this.getRecord().raidingVillage)) {''')
old='''        } else {
            try {
                this.navigation.moveTo((Path)path, 0.5);'''
assert old in s;s=s.replace(old,'''        } else {
            this.pathFailureRecovery.reset();
            try {
                this.navigation.moveTo((Path)path, 0.5);''')
start=s.index('    public void setPathDestPoint(');end=s.index('    public void setTexture(',start)
part=s[start:end].replace('this.navigation.stop();','this.pathFailureRecovery.reset();\n            this.navigation.stop();');s=s[:start]+part+s[end:]
old='''        if (jumpTo != null && jumpTo.distanceTo(this.getPathDestPoint()) < 4.0) {'''
assert old in s;s=s.replace(old,'''        if (jumpTo != null && jumpTo.distanceTo(this.getPathDestPoint()) < 4.0
            && this.level.noCollision(this, this.getBoundingBox().move(
                jumpTo.getiX() + 0.5 - this.getX(), jumpTo.getiY() - this.getY(), jumpTo.getiZ() + 0.5 - this.getZ()))) {''')
s=s.replace('this.setPos((double)jumpTo.getiX() + 0.5, (double)jumpTo.getiY() + 0.5, (double)jumpTo.getiZ() + 0.5);','this.setPos((double)jumpTo.getiX() + 0.5, (double)jumpTo.getiY(), (double)jumpTo.getiZ() + 0.5);')
p.write_text(s,encoding='utf-8')
print('Immediate/short stuck teleports removed; repeated-failure grace and collision check added.')
