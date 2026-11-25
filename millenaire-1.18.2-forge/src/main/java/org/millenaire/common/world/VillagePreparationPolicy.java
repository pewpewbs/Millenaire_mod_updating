package org.millenaire.common.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure bounds and backpressure policy; no world access. */
final class VillagePreparationPolicy {
    static final int MAX_PENDING = 8;
    static final int MAX_RADIUS = 10;
    record Cell(int x, int z) {}

    static int radius(int blockRadius) {
        return Math.max(2, blockRadius / 16 + 2);
    }

    static List<Cell> cells(int x, int z, int radius) {
        if (radius < 2 || radius > MAX_RADIUS) return List.of();
        List<Cell> cells = new ArrayList<>();
        for (int dx = -radius; dx <= radius + 1; dx++)
            for (int dz = -radius; dz <= radius + 1; dz++)
                cells.add(new Cell(x + dx, z + dz));
        cells.sort(Comparator.comparingInt(c -> Math.max(Math.abs(c.x() - x), Math.abs(c.z() - z))));
        return cells;
    }

    static boolean nearby(int x, int z, int otherX, int otherZ, int radius) {
        return Math.abs((long)x - otherX) <= radius && Math.abs((long)z - otherZ) <= radius;
    }

    static int interval(float mspt, long workNanos) {
        if (!Float.isFinite(mspt) || mspt >= 55 || workNanos >= 30_000_000L) return 0;
        return mspt >= 40 || workNanos >= 15_000_000L ? 10 : 2;
    }
}
