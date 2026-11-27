package org.millenaire.common.pathing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.village.Building;

/** A village's plot bounds, detached from the live town before searching. */
public final class VillageTransit {
    public static final VillageTransit NONE = new VillageTransit(new Plot[0]);
    private static final Map<Building, Layout> LAYOUTS = new WeakHashMap<>();
    private record Layout(long tick, List<Plot> plots) {}
    public record Plot(int x0, int x1, int y0, int y1, int z0, int z1, boolean compactRoom) {
        private boolean contains(AStarNode n) {
            return n.x >= x0 - 1 && n.x <= x1 + 1 && n.z >= z0 - 1 && n.z <= z1 + 1
                    && n.y >= y0 - 1 && n.y <= y1 + 1;
        }
    }
    private final Plot[] plots;
    private VillageTransit(Plot[] plots) { this.plots = plots; }

    public static VillageTransit between(Building hall, long tick, AStarNode start, AStarNode goal) {
        if (hall == null || Math.abs(start.x - goal.x) + Math.abs(start.z - goal.z) < 6) return NONE;
        Layout layout;
        synchronized (LAYOUTS) {
            layout = LAYOUTS.get(hall);
            if (layout == null || tick < layout.tick || tick - layout.tick >= 100) {
                var plots = new ArrayList<Plot>();
                for (var location : hall.getLocations()) {
                    if (location.level < 0) continue;
                    // Keep large compounds, courtyards, farms and bridges traversable.
                    // For these, only walking through an actual doorway carries a cost.
                    var plan = location.getIBuildingPlan();
                    boolean occupied = plan != null && (!plan.getFemaleResident().isEmpty()
                            || !plan.getMaleResident().isEmpty());
                    boolean compact = location.maxx - location.minx <= 14
                            && location.maxz - location.minz <= 14
                            && (location.craftingPos != null || occupied);
                    plots.add(new Plot(location.minx, location.maxx, location.miny,
                            location.maxy, location.minz, location.maxz, compact));
                }
                layout = new Layout(tick, List.copyOf(plots));
                LAYOUTS.put(hall, layout);
            }
        }
        return between(layout.plots, start, goal);
    }

    public static VillageTransit between(List<Plot> plots, AStarNode start, AStarNode goal) {
        int minX = Math.min(start.x, goal.x) - 6, maxX = Math.max(start.x, goal.x) + 6;
        int minZ = Math.min(start.z, goal.z) - 6, maxZ = Math.max(start.z, goal.z) + 6;
        var near = new ArrayList<Plot>();
        for (Plot p : plots) {
            if (p.x0 > maxX || p.x1 < minX || p.z0 > maxZ || p.z1 < minZ
                    || p.contains(start) || p.contains(goal)) continue;
            near.add(p);
            if (near.size() == 12) break;
        }
        return near.isEmpty() ? NONE : new VillageTransit(near.toArray(Plot[]::new));
    }

    public int cost(BlockGetter view, int x, int y, int z) {
        for (Plot p : plots) {
            if (x < p.x0 || x > p.x1 || z < p.z0 || z > p.z1 || y < p.y0 || y > p.y1) continue;
            // Stay away from the central floor of small houses. The perimeter
            // remains available as a footpath. A passage is always legal.
            if (p.compactRoom && x >= p.x0 + 2 && x <= p.x1 - 2
                    && z >= p.z0 + 2 && z <= p.z1 - 2) return 8;
            BlockPos pos = new BlockPos(x, y, z);
            if (view instanceof net.minecraft.world.level.Level level && !level.hasChunkAt(pos)) continue;
            if (view.getBlockState(pos).getBlock() instanceof DoorBlock) return 12;
        }
        return 0;
    }
}
