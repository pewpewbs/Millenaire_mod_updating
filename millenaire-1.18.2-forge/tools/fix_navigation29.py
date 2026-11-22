from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');p=r/'src/main/java/org/millenaire/common/entity/MillVillager.java'
b=r/'migration/before-navigation29';b.mkdir(exist_ok=False);shutil.copy2(p,b/p.name)
s=p.read_text();needle='    public static AttributeSupplier.Builder createVillagerAttributes() {'
assert s.count(needle)==1
s=s.replace(needle,'''    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new PathNavigateSimple(this, level);
    }

'''+needle)
p.write_text(s,encoding='utf-8')
print('Connected MillVillager to PathNavigateSimple')
