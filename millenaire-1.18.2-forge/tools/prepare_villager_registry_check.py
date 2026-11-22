from pathlib import Path
r=Path(__file__).resolve().parents[1];src=r/'src/main/java/org/millenaire';f=r/'tools/fixtures/villager-registry';f.mkdir(parents=True,exist_ok=True)
s=(src/'common/entity/MillVillager.java').read_text(encoding='utf-8');attrs=s[s.index('    public static AttributeSupplier.Builder createVillagerAttributes()'):s.index('    @Override\n    public Packet<ClientGamePacketListener>')]
constants='\n'.join(l for l in s.splitlines() if 'public static final ResourceLocation GENERIC_' in l)
body='''package org.millenaire.common.entity;
import net.minecraft.world.entity.*;import net.minecraft.world.entity.ai.attributes.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.level.Level;
public abstract class MillVillager extends PathfinderMob {
'''+constants+attrs+'''protected MillVillager(EntityType<? extends MillVillager> type,Level world){super(type,world);}
'''
for name in ['EntityGenericMale','EntityGenericSymmFemale','EntityGenericAsymmFemale']:body+='public static class '+name+' extends MillVillager { public '+name+'(EntityType<? extends MillVillager> type,Level world){super(type,world);} }\n'
(f/'MillVillager.java').write_text(body+'}\n',encoding='utf-8')
t=(r/'tools/QuestEntityRegistryCheck.java').read_text(encoding='utf-8').replace('QuestEntityRegistryCheck','VillagerEntityRegistryCheck').replace('MillQuestEntities','MillVillagerEntities').replace('"millblaze","millghast","millwitherskeleton"','"genericvillager","genericsimmfemale","genericasimmfemale"').replace('!=64','!=128')
t=t[:t.index('        if(!MillVillagerEntities.BLAZE')]+'''        for(var type:new EntityType<?>[]{MillVillagerEntities.MALE,MillVillagerEntities.FEMALE_SYM,MillVillagerEntities.FEMALE_ASYM}) {
            if(!type.fireImmune() || Math.abs(type.getWidth()-.6F)>.001 || Math.abs(type.getHeight()-1.8F)>.001)throw new AssertionError("Dimensions or immunity");
            if(attributes.get(type).getValue(Attributes.MAX_HEALTH)!=40 || attributes.get(type).getValue(Attributes.MOVEMENT_SPEED)!=.5)throw new AssertionError("Initial villager attributes");
        }
        System.out.println("PASS: three villager registry entries, copied production attribute builder, dimensions, fire immunity and 128-block tracking. Entity constructors, spawn data and gameplay use fixtures or remain untested.");
    }
}
'''
(r/'tools/VillagerEntityRegistryCheck.java').write_text(t,encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();o=r/'build/villager-registry-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,f/'MillVillager.java',src/'common/entity/MillVillagerEntities.java',r/'tools/VillagerEntityRegistryCheck.java']
(r/'build/villager-registry-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/villager-registry-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'net.minecraftforge.registries.VillagerEntityRegistryCheck'])),encoding='utf-8')
