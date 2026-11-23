/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.common.pathing.atomicstryker;

import java.util.List;
import org.millenaire.common.pathing.atomicstryker.AStarNode;

public interface IAStarPathedEntity {
    public void onFoundPath(List<AStarNode> var1);

    public void onNoPathAvailable();

    /**
     * The request's actor metadata is captured on the server thread when a
     * search is issued. Default methods preserve other, non-villager callers.
     */
    default long navigationActorId() { return -1L; }
    default String navigationActorName() { return getClass().getSimpleName(); }
    default String navigationBehaviorKey() { return "unknown"; }
}

