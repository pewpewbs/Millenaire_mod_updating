package org.millenaire.common.village;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.utilities.Point;

/** Conservative pen grouping. A clear low-level segment is evidence of a
 * connection; a blocked segment is not a full navigation proof. Never loads chunks. */
public final class AnimalPenAccess {
    private AnimalPenAccess(){}
    public static boolean connected(Level level,Entity context,Vec3 from,Vec3 to) {
        Vec3 delta=to.subtract(from);
        if(delta.lengthSqr()>32*32||Math.abs(delta.y)>1.0)return false;
        int steps=Math.max(1,(int)Math.ceil(delta.length()*2));
        for(int i=0;i<=steps;i++){
            Vec3 p=from.add(delta.scale(i/(double)steps));
            if(!level.hasChunkAt(new BlockPos(p)))return false;
        }
        return level.clip(new ClipContext(from.add(0,.45,0),to.add(0,.45,0),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,context)).getType()==HitResult.Type.MISS;
    }
    private static Vec3 foot(Point p){return new Vec3(p.getiX()+.5,p.getiY(),p.getiZ()+.5);}
    private static int root(int[] parent,int i){while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;}
    public static Point choose(Level level,Entity animal,List<Point> markers,List<Entity> herd){
        if(markers.isEmpty())return null;
        List<Point> valid=new ArrayList<>();
        for(Point p:markers)if(level.hasChunkAt(p.getBlockPos()))valid.add(p);
        if(valid.isEmpty())return null;
        int n=valid.size();
        int[] parent=new int[n], counts=new int[n], capacity=new int[n], markerCounts=new int[n];
        for(int i=0;i<n;i++)parent[i]=i;
        // Marker counts are small in village plans. Bound quadratic grouping on
        // anomalously large custom plots; fall back to individual marker balancing.
        if(n<=64)for(int i=0;i<n;i++)for(int j=0;j<i;j++)
            if(connected(level,animal,foot(valid.get(i)),foot(valid.get(j))))parent[root(parent,i)]=root(parent,j);
        for(int i=0;i<n;i++)capacity[root(parent,i)]++;
        for(Entity e:herd){
            if(!e.isAlive()||e.getType()!=animal.getType())continue;
            int nearest=-1;double distance=Double.MAX_VALUE;
            for(int i=0;i<n;i++){
                double ds=e.position().distanceToSqr(foot(valid.get(i)));
                if(ds<distance&&connected(level,e,e.position(),foot(valid.get(i)))){nearest=i;distance=ds;}
            }
            if(nearest>=0){counts[root(parent,nearest)]++;markerCounts[nearest]++;}
        }
        Point best=null;int bestScore=Integer.MIN_VALUE;
        for(int i=0;i<n;i++){
            Point p=valid.get(i);Vec3 pos=foot(p);
            var box=animal.getBoundingBox().move(pos.subtract(animal.position()));
            if(!level.noCollision(animal,box))continue;
            int group=root(parent,i), count=counts[group];
            // Finish an isolated pair before starting a new one; then spread
            // across available pens in proportion to registered marker capacity.
            int score=PenAllocationRules.score(count,capacity[group],markerCounts[i]);
            if(score>bestScore){best=p;bestScore=score;}
        }
        return best;
    }
}
