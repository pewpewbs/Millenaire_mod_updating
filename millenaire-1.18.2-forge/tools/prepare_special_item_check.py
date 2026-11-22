from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixture=root/'tools/fixtures/special-items';fixture.mkdir(parents=True,exist_ok=True)
fixtures={
'MillBlocks':'package org.millenaire.common.block; public class MillBlocks { public static final net.minecraft.world.item.CreativeModeTab tabMillenaire=net.minecraft.world.item.CreativeModeTab.TAB_MISC; }',
'MillItems':'package org.millenaire.common.item; public class MillItems { public static net.minecraft.world.item.Item DENIER,DENIER_ARGENT,DENIER_OR; static final net.minecraft.world.item.ArmorMaterial ARMOUR_mayan_quest_crown=MillMaterials.armor("mayanQuestCrown","millenaire:mayan_quest_crown",0,new int[]{3,6,8,3},10,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_DIAMOND,2); }',
'InvItem':'package org.millenaire.common.item; public class InvItem { public interface IItemInitialEnchantmens { void applyEnchantments(net.minecraft.world.item.ItemStack stack); } }',
'MillCommonUtilities':'package org.millenaire.common.utilities; public class MillCommonUtilities { public static int putItemsInChest(net.minecraft.world.Container inv,net.minecraft.world.item.Item item,int count){throw new UnsupportedOperationException("Not part of this fixture");} }',
'WorldUtilities':'package org.millenaire.common.utilities; public class WorldUtilities { public static int getItemsFromChest(net.minecraft.world.Container inv,net.minecraft.world.item.Item item,int meta,int count){throw new UnsupportedOperationException("Not part of this fixture");} }'}
for name,text in fixtures.items():(fixture/(name+'.java')).write_text(text,encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/special-item-check';out.mkdir(exist_ok=True)
q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+[src/'common/item'/(f+'.java') for f in ['MillMaterials','ItemMill','ItemMillenaireBow','ItemMayanQuestCrown','ItemPurse']]+list(fixture.glob('*.java'))+[root/'tools/SpecialItemCheck.java']
(root/'build/special-item-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8')
(root/'build/special-item-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.SpecialItemCheck']),encoding='utf-8')
