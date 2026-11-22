import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.phys.AABB;import net.minecraft.world.entity.item.PrimedTnt;
public class BoreholeCheck{
 static final String BOREHOLE="action_borehole",BOREHOLETNT="action_boreholetnt",BOREHOLETNTLIT="action_boreholetntlit",THEVOID="action_thevoid";
 static class Entity{double x,y,z;double getX(){return x;}double getY(){return y;}double getZ(){return z;}BlockPos blockPosition(){return new BlockPos(x,y,z);}}static class Player extends Entity{}
 static class Point{double x,y,z;Point(Entity e){x=e.x;y=e.y;z=e.z;}Point(String s){String[] a=s.split(",");x=Double.parseDouble(a[0]);y=Double.parseDouble(a[1]);z=Double.parseDouble(a[2]);}int getiX(){return (int)Math.floor(x);}int getiY(){return (int)Math.floor(y);}int getiZ(){return (int)Math.floor(z);}String getIntString(){return getiX()+","+getiY()+","+getiZ();}double distanceToSquared(Entity e){return (x-e.x)*(x-e.x)+(y-e.y)*(y-e.y)+(z-e.z)*(z-e.z);}}
 static class Profile{Set<String> tags=new HashSet<>();Map<String,String> data=new HashMap<>();boolean isTagSet(String t){return tags.contains(t);}void setTag(String t){tags.add(t);}void clearTag(String t){tags.remove(t);}String getActionData(String k){return data.get(k);}void setActionData(String k,String v){data.put(k,v);}void clearActionData(String k){data.remove(k);}}
 static class World{int min,max,reads;Map<BlockPos,Block> blocks=new HashMap<>();List<double[]> tnt=new ArrayList<>();int getMinBuildHeight(){return min;}int getMaxBuildHeight(){return max;}BlockState getBlockState(BlockPos p){if(p.getY()<min||p.getY()>=max)throw new AssertionError("Out of build bounds: "+p);if(++reads>25000)throw new AssertionError("Unbounded scan");return blocks.getOrDefault(p,p.getY()<=min+1?Blocks.BEDROCK:Blocks.CAVE_AIR).defaultBlockState();}List<Object> getEntitiesOfClass(Class c,AABB box){return tnt.stream().filter(p->box.contains(p[0],p[1],p[2])).map(p->(Object)p).toList();}}
 static class MillWorldData{World world=new World();Profile profile=new Profile();Profile getProfile(Player p){return profile;}}
 static class WorldUtilities{static Block getBlock(World w,int x,int y,int z){return w.getBlockState(new BlockPos(x,y,z)).getBlock();}}
 static class ServerSender{static int sent;static void sendTranslatedSentence(Player p,char c,String s,String...args){sent++;}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}static MillWorldData world(int min,int max,String tag){var w=new MillWorldData();w.world.min=min;w.world.max=max;w.profile.setTag(tag);return w;}
    private static void normanCQHandleBorehole(MillWorldData mw, Player player) {
        if (!mw.getProfile(player).isTagSet(BOREHOLE) || mw.getProfile(player).isTagSet("action_borehole_complete")) {
            return;
        }
        if (player.getY() > mw.world.getMinBuildHeight() + 10.0) {
            return;
        }
        int nbok = 0;
        for (int x = player.blockPosition().getX() - 2; x < player.blockPosition().getX() + 3; ++x) {
            for (int z = player.blockPosition().getZ() - 2; z < player.blockPosition().getZ() + 3; ++z) {
                boolean ok = true;
                boolean stop = false;
                for (int y = mw.world.getMaxBuildHeight() - 1; y > mw.world.getMinBuildHeight() && !stop; --y) {
                    Block block = WorldUtilities.getBlock(mw.world, x, y, z);
                    if (block == Blocks.BEDROCK && y <= mw.world.getMinBuildHeight() + 5) {
                        stop = true;
                        continue;
                    }
                    if (mw.world.getBlockState(new BlockPos(x, y, z)).isAir()) continue;
                    stop = true;
                    ok = false;
                }
                if (!ok) continue;
                ++nbok;
            }
        }
        if (nbok >= 25) {
            ServerSender.sendTranslatedSentence(player, '7', "actions.borehole_success", new String[0]);
            mw.getProfile(player).clearTag(BOREHOLE);
            mw.getProfile(player).setTag("action_borehole_complete");
            mw.getProfile(player).setActionData("action_borehole_pos", new Point((Entity)player).getIntString());
            return;
        }
        String maxKnownStr = mw.getProfile(player).getActionData("action_borehole_max");
        int maxKnown = 0;
        if (maxKnownStr != null) {
            maxKnown = Integer.parseInt(maxKnownStr);
        }
        if (nbok > maxKnown) {
            ServerSender.sendTranslatedSentence(player, '7', "actions.borehole_nblineok", "" + nbok);
            mw.getProfile(player).setActionData("action_borehole_max", "" + nbok);
        }
    }
    private static void normanCQHandleBoreholeTNT(MillWorldData mw, Player player) {
        if (!mw.getProfile(player).isTagSet(BOREHOLETNT) || mw.getProfile(player).isTagSet("action_boreholetnt_complete")) {
            return;
        }
        String pStr = mw.getProfile(player).getActionData("action_borehole_pos");
        if (pStr == null) {
            return;
        }
        Point p = new Point(pStr);
        if (p.distanceToSquared((Entity)player) > 25.0) {
            return;
        }
        int nbTNT = 0;
        for (int x = p.getiX() - 2; x < p.getiX() + 3; ++x) {
            for (int z = p.getiZ() - 2; z < p.getiZ() + 3; ++z) {
                boolean obsidian = false;
                for (int y = Math.min(mw.world.getMinBuildHeight() + 6, mw.world.getMaxBuildHeight() - 1); y > mw.world.getMinBuildHeight(); --y) {
                    Block block = WorldUtilities.getBlock(mw.world, x, y, z);
                    if (block == Blocks.OBSIDIAN) {
                        obsidian = true;
                        continue;
                    }
                    if (!obsidian || block != Blocks.TNT) continue;
                    ++nbTNT;
                }
            }
        }
        if (nbTNT >= 20) {
            ServerSender.sendTranslatedSentence(player, '7', "actions.boreholetnt_success", new String[0]);
            mw.getProfile(player).clearTag(BOREHOLETNT);
            mw.getProfile(player).setTag("action_boreholetnt_complete");
            mw.getProfile(player).setTag(BOREHOLETNTLIT);
            mw.getProfile(player).clearActionData("action_boreholetnt_max");
            return;
        }
        if (nbTNT == 0) {
            return;
        }
        String maxKnownStr = mw.getProfile(player).getActionData("action_boreholetnt_max");
        int maxKnown = 0;
        if (maxKnownStr != null) {
            maxKnown = Integer.parseInt(maxKnownStr);
        }
        if (nbTNT > maxKnown) {
            ServerSender.sendTranslatedSentence(player, '7', "actions.boreholetnt_nbtnt", "" + nbTNT);
            mw.getProfile(player).setActionData("action_boreholetnt_max", "" + nbTNT);
        }
    }
    private static void normanCQHandleBoreholeTNTLit(MillWorldData mw, Player player) {
        if (!mw.getProfile(player).isTagSet(BOREHOLETNTLIT) || mw.getProfile(player).isTagSet("action_boreholetntlit_complete")) {
            return;
        }
        String savedPosition = mw.getProfile(player).getActionData("action_borehole_pos");
        if (savedPosition == null) return;
        Point p = new Point(savedPosition);
        int nbtnt = mw.world.getEntitiesOfClass(PrimedTnt.class, new AABB(p.x, p.y, p.z, p.x + 1.0, p.y + 1.0, p.z + 1.0).inflate(8.0, 4.0, 8.0)).size();
        if (nbtnt > 0) {
            ServerSender.sendTranslatedSentence(player, '7', "actions.boreholetntlit_success", new String[0]);
            mw.getProfile(player).clearTag(BOREHOLETNTLIT);
            mw.getProfile(player).setTag("action_boreholetntlit_complete");
            return;
        }
    }
    private static void normanCQHandleTheVoid(MillWorldData mw, Player player) {
        if (!mw.getProfile(player).isTagSet(THEVOID) || mw.getProfile(player).isTagSet("action_thevoid_complete")) {
            return;
        }
        if (player.getY() > mw.world.getMinBuildHeight() + 30.0) {
            return;
        }
        for (int i = -5; i < 5; ++i) {
            for (int j = -5; j < 5; ++j) {
                BlockPos floor = new BlockPos(player.blockPosition().getX() + i, mw.world.getMinBuildHeight(), player.blockPosition().getZ() + j);
                if (!mw.world.getBlockState(floor).isAir()) continue;
                ServerSender.sendTranslatedSentence(player, '7', "actions.thevoid_success", new String[0]);
                mw.getProfile(player).clearTag(THEVOID);
                mw.getProfile(player).setTag("action_thevoid_complete");
                return;
            }
        }
    }
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var p=new Player();p.x=-0.5;p.z=-0.5;
 for(int[] h:new int[][]{{-64,320},{0,256},{0,128}}){var m=world(h[0],h[1],BOREHOLE);p.y=h[0]+3;normanCQHandleBorehole(m,p);check(m.profile.isTagSet(BOREHOLE+"_complete"),"Full 5x5 cave-air shaft "+h[0]);check(m.profile.getActionData("action_borehole_pos").startsWith("-1,"),"Negative anchor rounded down");int sent=ServerSender.sent;normanCQHandleBorehole(m,p);check(ServerSender.sent==sent,"No repeated completion");}
 var m=world(-64,320,BOREHOLE);p.y=-61;m.world.blocks.put(new BlockPos(-3,200,-3),Blocks.STONE);normanCQHandleBorehole(m,p);check(!m.profile.isTagSet(BOREHOLE+"_complete")&&"24".equals(m.profile.getActionData("action_borehole_max")),"High obstruction and negative edge included");m=world(-64,320,BOREHOLE);m.world.blocks.put(new BlockPos(-1,319,-1),Blocks.BEDROCK);normanCQHandleBorehole(m,p);check(!m.profile.isTagSet(BOREHOLE+"_complete"),"Bedrock roof is not bottom");m=world(-64,320,BOREHOLE);p.y=3;normanCQHandleBorehole(m,p);check(m.world.reads==0,"Old zero depth rejected");p.y=-61;
 for(int count:new int[]{19,20}){m=world(-64,320,BOREHOLETNT);m.profile.setActionData("action_borehole_pos","-1,-61,-1");int n=0;for(int x=-3;x<2;x++)for(int z=-3;z<2;z++)if(n++<count){m.world.blocks.put(new BlockPos(x,-58,z),Blocks.OBSIDIAN);m.world.blocks.put(new BlockPos(x,-59,z),Blocks.TNT);}normanCQHandleBoreholeTNT(m,p);check(m.profile.isTagSet(BOREHOLETNT+"_complete")== (count==20),"TNT count threshold");check(m.profile.isTagSet(BOREHOLETNTLIT)== (count==20),"Next quest enabled only on success");}
 m=world(-64,320,BOREHOLETNT);m.profile.setActionData("action_borehole_pos","-1,-61,-1");for(int x=-3;x<2;x++)for(int z=-3;z<2;z++)m.world.blocks.put(new BlockPos(x,-59,z),Blocks.TNT);normanCQHandleBoreholeTNT(m,p);check(!m.profile.isTagSet(BOREHOLETNT+"_complete"),"Obsidian required");
 for(double offset:new double[]{-7,7}){m=world(-64,320,BOREHOLETNTLIT);m.profile.setActionData("action_borehole_pos","-1,-61,-1");m.world.tnt.add(new double[]{-1+offset,-61,-1});normanCQHandleBoreholeTNTLit(m,p);check(m.profile.isTagSet(BOREHOLETNTLIT+"_complete"),"TNT detection both directions");}m=world(-64,320,BOREHOLETNTLIT);normanCQHandleBoreholeTNTLit(m,p);check(!m.profile.isTagSet(BOREHOLETNTLIT+"_complete"),"Missing saved position safe");
 m=world(-64,320,THEVOID);normanCQHandleTheVoid(m,p);check(!m.profile.isTagSet(THEVOID+"_complete"),"Intact bottom does not pass");m.world.blocks.put(new BlockPos(-6,-64,-6),Blocks.VOID_AIR);normanCQHandleTheVoid(m,p);check(m.profile.isTagSet(THEVOID+"_complete"),"Void air at negative bottom counts");m=world(-64,320,THEVOID);p.y=0;normanCQHandleTheVoid(m,p);check(m.world.reads==0,"Void quest requires actual depth");
 System.out.println("PASS: extracted four borehole/TNT/void quest methods with world/profile/player fixtures: dimension bounds, 5x5 negative coordinates, cave air, high obstruction, bedrock roof, TNT thresholds/cover, symmetric ignition detection, missing position and bottom void. Live world loading, explosions and quest text untested.");}}
