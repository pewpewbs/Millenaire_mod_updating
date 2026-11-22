from pathlib import Path
r=Path(__file__).resolve().parents[1];source=(r/'src/main/java/org/millenaire/common/quest/SpecialQuestActions.java').read_text(encoding='utf-8')
def extract(name):
 a=source.index('    private static void '+name);b=source.index('{',a);i=b+1;d=1
 while d:
  if source[i]=='{':d+=1
  elif source[i]=='}':d-=1
  i+=1
 return source[a:i]
methods='\n'.join(extract(n) for n in ['indianCQHandleBottomOfTheWorld','indianCQHandleTopOfTheWorld','indianCQHandleUnderwaterDive','indianCQHandleUnderwaterGlass'])
text='''import java.util.*;import net.minecraft.world.level.block.*;import net.minecraft.network.chat.*;import net.minecraft.ChatFormatting;import net.minecraft.resources.ResourceLocation;import org.millenaire.common.utilities.LegacyBiomeNames;
public class QuestBoundsCheck{
 static final String BOTTOMOFTHEWORLD="action_bottomoftheworld",TOPOFTHEWORLD="action_topoftheworld",UNDERWATER_DIVE="action_underwater_dive",UNDERWATER_GLASS="action_underwater_glass";
 static class Entity{double y;double getY(){return y;}}static class Player extends Entity{}
 static class Point{int y;Point(Entity e){y=(int)Math.floor(e.y);}Point(int y){this.y=y;}int getiY(){return y;}Point getAbove(){return new Point(y+1);}}
 static class Profile{Set<String> tags=new HashSet<>();boolean isTagSet(String t){return tags.contains(t);}void setTag(String t){tags.add(t);}void clearTag(String t){tags.remove(t);}}
 static class World{int min,max,reads;Map<Integer,Block> blocks=new HashMap<>();int getMinBuildHeight(){return min;}int getMaxBuildHeight(){return max;}}
 static class MillWorldData{World world=new World();Profile profile=new Profile();Profile getProfile(Player p){return profile;}}
 static class WorldUtilities{static Block getBlock(World w,Point p){if(++w.reads>600)throw new AssertionError("Unbounded scan");return w.blocks.getOrDefault(p.y,Blocks.AIR);}}
 static class BlockItemUtilities{static boolean isBlockOpaqueCube(Block b){return b==Blocks.STONE;}}
 static class ServerSender{static int sent;static void sendTranslatedSentence(Player p,char c,String key,String...args){sent++;}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}static MillWorldData world(int min,int max,String tag){var m=new MillWorldData();m.world.min=min;m.world.max=max;m.profile.setTag(tag);return m;}
'''+methods+'''
 public static void main(String[] args)throws Exception{net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var p=new Player();
 for(int[] heights:new int[][]{{-64,320},{0,256},{0,128}}){int min=heights[0],max=heights[1];var m=world(min,max,BOTTOMOFTHEWORLD);p.y=min+4;indianCQHandleBottomOfTheWorld(m,p);check(!m.profile.isTagSet(BOTTOMOFTHEWORLD+"_complete"),"Lower boundary exclusive");p.y=min+3;indianCQHandleBottomOfTheWorld(m,p);check(m.profile.isTagSet(BOTTOMOFTHEWORLD+"_complete"),"Actual world bottom");int sent=ServerSender.sent;indianCQHandleBottomOfTheWorld(m,p);check(ServerSender.sent==sent,"No duplicate completion");m=world(min,max,TOPOFTHEWORLD);p.y=max-6;indianCQHandleTopOfTheWorld(m,p);check(!m.profile.isTagSet(TOPOFTHEWORLD+"_complete"),"Upper boundary exclusive");p.y=max-5;indianCQHandleTopOfTheWorld(m,p);check(m.profile.isTagSet(TOPOFTHEWORLD+"_complete"),"Actual world top");}
 var m=world(-64,320,UNDERWATER_GLASS);p.y=-50;indianCQHandleUnderwaterGlass(m,p);check(m.world.reads<=372&&!m.profile.isTagSet(UNDERWATER_GLASS+"_complete"),"Open sky terminates without success");
 for(int depth:new int[]{12,13}){m=world(-64,320,UNDERWATER_DIVE);p.y=-40;for(int i=0;i<depth;i++)m.world.blocks.put(-40+i,Blocks.WATER);indianCQHandleUnderwaterDive(m,p);check(m.profile.isTagSet(UNDERWATER_DIVE+"_complete")== (depth>12),"Dive depth threshold below zero");}
 for(int depth:new int[]{15,16}){m=world(-64,320,UNDERWATER_GLASS);p.y=-40;m.world.blocks.put(-38,Blocks.GLASS);for(int i=0;i<depth;i++)m.world.blocks.put(-37+i,Blocks.WATER);indianCQHandleUnderwaterGlass(m,p);check(m.profile.isTagSet(UNDERWATER_GLASS+"_complete")== (depth>15),"Glass depth threshold below zero");}
 m=world(-64,320,UNDERWATER_GLASS);p.y=310;m.world.blocks.put(312,Blocks.STONE);m.world.blocks.put(313,Blocks.GLASS);indianCQHandleUnderwaterGlass(m,p);check(!m.profile.isTagSet(UNDERWATER_GLASS+"_complete"),"Opaque ceiling blocks search");
 check(LegacyBiomeNames.matches("Extreme Hills",new ResourceLocation("windswept_hills")),"Legacy mountain biome");check(LegacyBiomeNames.matches("forest",new ResourceLocation("forest"))&&!LegacyBiomeNames.matches("forest",new ResourceLocation("desert"))&&!LegacyBiomeNames.matches("forest",null),"Biome match and exclusion");var chat=new TextComponent("Test");chat.withStyle(ChatFormatting.GOLD);check(chat.getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.GOLD)),"Modern chat color applied");
 System.out.println("PASS: extracted production quest methods with world/player/profile fixtures: three dimension height ranges, exact thresholds, no duplicate completion, open-sky termination, negative-height water/glass depths and opaque ceilings; actual biome compatibility and chat style APIs. Live quests, borehole adaptation and network delivery untested.");}}
'''
(r/'tools/QuestBoundsCheck.java').write_text(text,encoding='utf-8');cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/quest-bounds-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/QuestBoundsCheck.java',r/'src/main/java/org/millenaire/common/utilities/LegacyBiomeNames.java'];(r/'build/quest-bounds-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/quest-bounds-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'QuestBoundsCheck'])),encoding='utf-8')
