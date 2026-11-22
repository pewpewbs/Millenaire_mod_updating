import java.nio.file.*;import java.util.*;import net.minecraft.util.datafix.fixes.BlockStateData;
public class DumpLegacyBlockIds {
 public static void main(String[] args)throws Exception {
  var field=BlockStateData.class.getDeclaredField("ID_BY_OLD_NAME");field.setAccessible(true);
  Map<String,Integer> values=(Map<String,Integer>)field.get(null);TreeMap<String,Integer> sorted=new TreeMap<>();
  values.forEach((key,id)->sorted.put(key,id >> 4));
  StringBuilder out=new StringBuilder("# Generated from Minecraft 1.18.2 BlockStateData; original vanilla block IDs only.\n");
  sorted.forEach((key,id)->out.append(key).append('=').append(id).append('\n'));
  var statesField=BlockStateData.class.getDeclaredField("ID_BY_OLD");statesField.setAccessible(true);
  Map<com.mojang.serialization.Dynamic<?>,Integer> states=(Map<com.mojang.serialization.Dynamic<?>,Integer>)statesField.get(null);
  TreeSet<String> lines=new TreeSet<>();states.forEach((state,id)->lines.add(id+"\t"+state.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue().toString()));
  Files.write(Path.of(args[0]).resolveSibling("legacy-block-properties.txt"),lines);
  Files.writeString(Path.of(args[0]),out);System.out.println("Legacy block names: "+sorted.size());
 }
}