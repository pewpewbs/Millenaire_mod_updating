from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixture=root/'tools/fixtures/chest';fixture.mkdir(parents=True,exist_ok=True)
fixtures={
'common/entity/MillBlockEntities':'public class MillBlockEntities { public static net.minecraft.world.level.block.entity.BlockEntityType<TileEntityLockedChest> LOCKED_CHEST; }',
'common/forge/Mill':'public class Mill { public static org.millenaire.common.world.MillWorldData clientWorld; public static org.millenaire.common.world.MillWorldData getMillWorld(net.minecraft.world.level.Level world) { return null; } }',
'common/world/MillWorldData':'public class MillWorldData { public org.millenaire.common.village.Building getBuilding(org.millenaire.common.utilities.Point point) { return null; } }',
'common/village/Building':'public class Building { public boolean chestLocked; public int invalidations; public void invalidateInventoryCache() { invalidations++; } public String getNativeBuildingName(){return "test";} public boolean lockedForPlayer(net.minecraft.world.entity.player.Player player){return chestLocked;} }',
'common/config/MillConfigValues':'public class MillConfigValues { public static boolean DEV; }',
'common/utilities/Point':'''public class Point { private final net.minecraft.core.BlockPos pos; public Point(net.minecraft.core.BlockPos pos){this.pos=pos;} public org.millenaire.common.entity.TileEntityLockedChest getMillChest(net.minecraft.world.level.Level world){return null;} public void write(net.minecraft.nbt.CompoundTag tag,String key){tag.putLong(key,pos.asLong());} public static Point read(net.minecraft.nbt.CompoundTag tag,String key){return tag.contains(key)?new Point(net.minecraft.core.BlockPos.of(tag.getLong(key))):null;} }''',
'common/utilities/LanguageUtilities':'public class LanguageUtilities { public static String string(String key){return key;} public static net.minecraft.network.chat.Component textComponent(String key){return new net.minecraft.network.chat.TextComponent(key);} }',
'common/utilities/MillLog':'public class MillLog { public static void error(Object source,String text){} public static void printException(String text,Exception error){} }',
'common/network/StreamReadWrite':'public class StreamReadWrite { public static org.millenaire.common.utilities.Point readNullablePoint(net.minecraft.network.FriendlyByteBuf buffer){return null;} public static net.minecraft.world.item.ItemStack readNullableItemStack(net.minecraft.network.FriendlyByteBuf buffer) throws java.io.IOException {return net.minecraft.world.item.ItemStack.EMPTY;} }',
'common/network/ServerSender':'public class ServerSender { public static void sendLockedChestUpdatePacket(org.millenaire.common.entity.TileEntityLockedChest chest,net.minecraft.world.entity.player.Player player){} }',
'common/ui/MillMenus':'public class MillMenus { public static net.minecraft.world.inventory.MenuType<ContainerLockedChest> LOCKED_CHEST=new net.minecraft.world.inventory.MenuType<>((id,inventory)->null); }',
'common/ui/GuiActions':'public class GuiActions { public static void activateMillChest(net.minecraft.world.entity.player.Player player,org.millenaire.common.utilities.Point point){} }',
}
for path,body in fixtures.items():
 f=fixture/(path+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package org.millenaire.'+path.rsplit('/',1)[0].replace('/','.')+';\n'+body,encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text().strip();out=root/'build/chest-check';out.mkdir(exist_ok=True)
sources=['common/block/LegacyVariantBlock','common/block/BlockLockedChest','common/entity/TileEntityLockedChest','common/ui/ContainerLockedChest','client/render/TileEntityLockedChestRenderer']
q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+[src/(s+'.java') for s in sources]+list(fixture.rglob('*.java'))+[root/'tools/ChestStateCheck.java']
(root/'build/chest-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8')
(root/'build/chest-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.ChestStateCheck']),encoding='utf-8')
