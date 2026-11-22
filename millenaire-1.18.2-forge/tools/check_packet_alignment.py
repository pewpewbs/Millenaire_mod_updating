from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
p=r/'tools/PacketAlignmentCheck.java';p.write_text('''import org.millenaire.common.network.StreamReadWrite;import net.minecraft.network.FriendlyByteBuf;import io.netty.buffer.Unpooled;import net.minecraft.world.item.*;import net.minecraft.world.item.alchemy.*;import net.minecraft.nbt.*;
public class PacketAlignmentCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
 var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(23);sword.getOrCreateTag().putString("custom","тест");var tagged=new ItemStack(Items.STONE,32);tagged.getOrCreateTag().putInt("MillenaireVariant",2);var potion=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.AWKWARD);
 ItemStack[] values={null,ItemStack.EMPTY,new ItemStack(Items.COD,3),sword,tagged,potion};int cases=0;
 for(ItemStack left:values)for(ItemStack right:values){var buf=new FriendlyByteBuf(Unpooled.buffer());try{StreamReadWrite.writeNullableItemStack(left,buf);StreamReadWrite.writeNullableItemStack(right,buf);buf.writeInt(1);buf.writeInt(Item.getId(Items.STONE));buf.writeInt(0);buf.writeInt(64);buf.writeLong(0x123456789abcdefL);var a=StreamReadWrite.readNullableItemStack(buf);var b=StreamReadWrite.readNullableItemStack(buf);check(left==null?a==null:a!=null&&ItemStack.matches(left,a),"First stack");check(right==null?b==null:b!=null&&ItemStack.matches(right,b),"Second stack");check(buf.readInt()==1&&buf.readInt()==Item.getId(Items.STONE)&&buf.readInt()==0&&buf.readInt()==64&&buf.readLong()==0x123456789abcdefL&&buf.readableBytes()==0,"Inventory alignment and tail");cases++;}finally{buf.release();}}
 var write=StreamReadWrite.class.getDeclaredMethod("writeNBTTagCompound",CompoundTag.class,FriendlyByteBuf.class);write.setAccessible(true);var read=StreamReadWrite.class.getDeclaredMethod("readNBTTagCompound",FriendlyByteBuf.class);read.setAccessible(true);var tag=new CompoundTag();tag.putString("name","дані");for(CompoundTag nbt:new CompoundTag[]{null,new CompoundTag(),tag}){var buf=new FriendlyByteBuf(Unpooled.buffer());try{write.invoke(null,nbt,buf);buf.writeInt(54321);check(java.util.Objects.equals(nbt,read.invoke(null,buf))&&buf.readInt()==54321&&buf.readableBytes()==0,"NBT boundary");}finally{buf.release();}}
 System.out.println("PASS: actual StreamReadWrite codecs: "+cases+" held-item pairs followed by inventory fields and sentinel; null, empty, damaged, named/variant and potion tags; nullable NBT boundaries. Live delivery validated separately.");}}
''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+str(r/'src/main/resources')+';'+(r/'build/migration-classpath.txt').read_text().strip();out=r/'build/packet-alignment-check';out.mkdir(exist_ok=True)
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(p)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'PacketAlignmentCheck'],check=True)
