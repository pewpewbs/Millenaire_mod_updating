/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.EnumDyeColor
 */
package org.millenaire.common.block;

import net.minecraft.world.item.DyeColor;

public interface IPaintedBlock {
    public String getBlockType();

    public DyeColor getDyeColour();

    public void initModel();
}

