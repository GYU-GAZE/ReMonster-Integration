package com.gyugaze.remonsterintegration.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only accessor used to place Re:Monster chrome around, never over, native menus. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int remonster$getLeftPos();

    @Accessor("topPos")
    int remonster$getTopPos();

    @Accessor("imageWidth")
    int remonster$getImageWidth();

    @Accessor("imageHeight")
    int remonster$getImageHeight();
}
