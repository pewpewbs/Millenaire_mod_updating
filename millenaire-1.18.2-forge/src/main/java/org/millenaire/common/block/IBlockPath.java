/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.properties.PropertyBool
 */
package org.millenaire.common.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.millenaire.common.block.BlockPath;
import org.millenaire.common.block.BlockPathSlab;

public interface IBlockPath {
    public static final BooleanProperty STABLE = BooleanProperty.create((String)"stable");

    public BlockPath getDoubleSlab();

    public BlockPathSlab getSingleSlab();

    public boolean isFullPath();
}

