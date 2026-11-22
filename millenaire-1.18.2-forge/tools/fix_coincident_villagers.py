from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java'
s=p.read_text(encoding='utf-8')
needle='    public void aiStep() {\n'
assert s.count(needle)==1
code='''    @Override
    public void push(Entity other) {
        // Vanilla skips separation when both horizontal centres are within 0.01 blocks.
        // Shared work destinations can put two villagers exactly at that position.
        if (other instanceof MillVillager && !this.level.isClientSide
                && !this.noPhysics && !other.noPhysics
                && this.isAlive() && other.isAlive()
                && !this.isPassenger() && !other.isPassenger()
                && !this.isVehicle() && !other.isVehicle()
                && !this.isVillagerSleeping() && !((MillVillager) other).isVillagerSleeping()
                && this.getBoundingBox().intersects(other.getBoundingBox())
                && Math.max(Math.abs(this.getX() - other.getX()),
                            Math.abs(this.getZ() - other.getZ())) < 0.01D) {
            // One impulse per pair; normal movement still resolves walls and doors.
            if (this.getId() < other.getId()) {
                this.push(-0.025D, 0.0D, 0.0D);
                other.push(0.025D, 0.0D, 0.0D);
            }
            return;
        }
        super.push(other);
    }

'''
backup=r/'migration/before-coincident-villagers';backup.mkdir(exist_ok=True)
assert not (backup/'MillVillager.java').exists()
shutil.copy2(p,backup/'MillVillager.java')
p.write_text(s.replace(needle,code+needle),encoding='utf-8')
print('Added server-side coincident-centre separation; backup:',backup)
