package com.eerussianguy.blazemap.util;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Minecraft doesn't have an easy way to tell if a block will render with transparency or not
 * based on the block data itself. There seems to be _no_ way to do this on the server side based
 * on the information Minecraft gives us alone. Thus, it's up to us to determine which blocks
 * should be considered "transparent" for mapping purposes.
 * 
 * The following 6 sets allow us, plus other mods via API, to decide which blocks should be
 * considered transparent by BM, and which should not. This can be determined based on a block's
 * class (including superclasses), its block tags, or its fluid tags. Add the class or tag to the
 * appropriate list for it to count.
 * 
 * The default transparency is quite translucent, blocking most of what's underneath but still
 * giving hints of what's below. This is for blocks like ice which aren't entirely see-through but
 * still transmit some light. For the minority of blocks that are properly clear like stained glass,
 * you'll want to also add them to the "quite transparent" lists. This will make them show a lot
 * more of what's underneath including contour shadows.
 * 
 * In practice, a block is expected to pass the `isTransparent` check if it's also expected to pass
 * the `isQuiteTransparent` check. However, each check doesn't need to pass for the same reason.
 * For example: Stained glass passes the "isTransparent" check because it subclasses `HalfTransparentBlock`.
 * But it passes the "isQuiteTransparent" check because it subclasses `AbstractGlassBlock`, which
 * is a subclass of `HalfTransparentBlock`. There is no reason to check it's a `HalfTransparentBlock`
 * again if we've already checked that it's an `AbstractGlassBlock`.
 */
public class Transparency {
    public static final float OPACITY_LOW = 0.1875f; // 3/16ths
    public static final float OPACITY_HIGH = 0.85f;

    private static final Set<Class<?>> transparentClasses = initialiseTransparentClassSet(false);
    private static final Set<Class<?>> quiteTransparentClasses = initialiseTransparentClassSet(true);

    private static final Set<TagKey<Fluid>> transparentFluidTags = initialiseTransparentFluidSet(false);
    private static final Set<TagKey<Fluid>> quiteTransparentFluidTags = initialiseTransparentFluidSet(true);

    private static final Set<TagKey<Block>> transparentBlockTags = initialiseTransparentBlockSet(false);
    private static final Set<TagKey<Block>> quiteTransparentBlockTags = initialiseTransparentBlockSet(true);

    public enum TransparencyState {
        AIR(0f),
        QUITE_TRANSPARENT(OPACITY_LOW),
        SEMI_TRANSPARENT(OPACITY_HIGH),
        OPAQUE(1f),
        ;

        public final float opacity;

        private TransparencyState(float opacity) {
            this.opacity = opacity;
        }

        public static TransparencyState max(TransparencyState t1, TransparencyState t2) {
            if (t1.opacity > t2.opacity) return t1;
            return t2;
        }

        public static TransparencyState min(TransparencyState t1, TransparencyState t2) {
            if (t1.opacity < t2.opacity) return t1;
            return t2;
        }
    }

    public enum ColorMixState {
        BLOCK,
        FLUIDLOGGED,
        FLUID,
        AIR,
    }

    private static final Set<Class<?>> initialiseTransparentClassSet (boolean isQuiteTransparent) {
        Set<Class<?>> newTransparencyList = new HashSet<Class<?>>();

        if (isQuiteTransparent) {
            newTransparencyList.add(AbstractGlassBlock.class); // Glass of all colours and types
        } else { // is only semi-transparent
            newTransparencyList.add(HalfTransparentBlock.class); // Slime, honey, ice, glass (is parent of AbstractGlassBlock)
        }
        // Both
        // N/A for now

        return newTransparencyList;
    }

    private static final Set<TagKey<Fluid>> initialiseTransparentFluidSet (boolean isQuiteTransparent) {
        Set<TagKey<Fluid>> newTransparencyList = new HashSet<TagKey<Fluid>>();

        // if (isQuiteTransparent) {
        //     // N/A for now
        // } else { // is only semi-transparent
        //     // N/A for now
        // }

        // Both
        newTransparencyList.add(FluidTags.WATER); // The wet stuff we all know and love

        return newTransparencyList;
    }

    private static final Set<TagKey<Block>> initialiseTransparentBlockSet (boolean isQuiteTransparent) {
        Set<TagKey<Block>> newTransparencyList = new HashSet<TagKey<Block>>();

        // if (isQuiteTransparent) {
        //     // N/A for now
        // } else { // is only semi-transparent
        //     // N/A for now
        // }

        // Both
        // N/A for now

        return newTransparencyList;
    }


    // /**
    //  * Eg: Water, slime, honey, ice, glass
    //  */
    // protected static boolean isTransparent(BlockState testBlockState) {
    //     Block testBlock = testBlockState.getBlock();
    //     FluidState testFluid = testBlockState.getFluidState();

    //     for (Class<?> transparentClass : transparentClasses) {
    //         if (transparentClass.isAssignableFrom(testBlock.getClass())) {
    //             return true;
    //         }
    //     }

    //     for (TagKey<Fluid> transparentFluid: transparentFluidTags) {
    //         if (testFluid.is(transparentFluid)) {
    //             return true;
    //         }
    //     }

    //     for (TagKey<Block> transparentBlock: transparentBlockTags) {
    //         if (testBlockState.is(transparentBlock)) {
    //             return true;
    //         }
    //     }

    //     // Else, hasn't matched anything above
    //     return false;

    //     // return (testBlockState.getBlock() instanceof HalfTransparentBlock) || testBlockState.getFluidState().is(FluidTags.WATER);
    // }

    /**
     * Eg: Slime, honey, ice, glass
     */
    public static boolean isTransparentBlock(BlockState testBlockState) {
        Block testBlock = testBlockState.getBlock();

        for (Class<?> transparentClass : transparentClasses) {
            if (transparentClass.isAssignableFrom(testBlock.getClass())) {
                return true;
            }
        }

        for (TagKey<Block> transparentBlock: transparentBlockTags) {
            if (testBlockState.is(transparentBlock)) {
                return true;
            }
        }

        // Else, hasn't matched anything above
        return false;
    }

    /**
     * Eg: Water
     */
    public static boolean isTransparentFluid(BlockState testBlockState) {
        FluidState testFluid = testBlockState.getFluidState();

        for (TagKey<Fluid> transparentFluid: transparentFluidTags) {
            if (testFluid.is(transparentFluid)) {
                return true;
            }
        }

        // Else, hasn't matched anything above
        return false;
    }

    // /**
    //  * Pretty much only glass and water by default
    //  */
    // public static boolean isQuiteTransparent(BlockState testBlockState) {
    //     Block testBlock = testBlockState.getBlock();
    //     FluidState testFluid = testBlockState.getFluidState();

    //     for (Class<?> transparentClass : quiteTransparentClasses) {
    //         if (transparentClass.isAssignableFrom(testBlock.getClass())) {
    //             return true;
    //         }
    //     }

    //     for (TagKey<Fluid> transparentFluid: quiteTransparentFluidTags) {
    //         if (testFluid.is(transparentFluid)) {
    //             return true;
    //         }
    //     }

    //     for (TagKey<Block> transparentBlock: quiteTransparentBlockTags) {
    //         if (testBlockState.is(transparentBlock)) {
    //             return true;
    //         }
    //     }

    //     // Else, hasn't matched anything above
    //     return false;

    //     // return (state.getBlock() instanceof AbstractGlassBlock) || state.getFluidState().is(FluidTags.WATER);
    // }

    /**
     * Eg: Glass
     */
    public static boolean isQuiteTransparentBlock(BlockState testBlockState) {
        Block testBlock = testBlockState.getBlock();

        for (Class<?> transparentClass : quiteTransparentClasses) {
            if (transparentClass.isAssignableFrom(testBlock.getClass())) {
                return true;
            }
        }

        for (TagKey<Block> transparentBlock: quiteTransparentBlockTags) {
            if (testBlockState.is(transparentBlock)) {
                return true;
            }
        }

        // Else, hasn't matched anything above
        return false;
    }
    /**
     * Eg: Water
     */
    public static boolean isQuiteTransparentFluid(BlockState testBlockState) {
        FluidState testFluid = testBlockState.getFluidState();

        for (TagKey<Fluid> transparentFluid: quiteTransparentFluidTags) {
            if (testFluid.is(transparentFluid)) {
                return true;
            }
        }

        // Else, hasn't matched anything above
        return false;
    }

    // External API for other mods to mark their own blocks as transparent.
    // TODO: Should probably make a custom blocktag at some point specifically for folks to apply to their
    // blocks rather than having block identifiers added to our list, but that's a future task for when people
    // outside the BME project care enough to actually proactively make their mods work better with BME

    /** Add a new block class to mark it as transparent (transmits some light) */
    public static void addTransparentBlockClass(Class<?> block) { transparentClasses.add(block); };
    /** Add a new block class to mark it as quite transparent (very low opacity, like glass). Block must also be marked separately as transparent */
    public static void addQuiteTransparentBlockClass(Class<?> block) { quiteTransparentClasses.add(block); };

    /** Add a new fluid tag to mark it as transparent (transmits some light) */
    public static void addTransparentFluidTag(TagKey<Fluid> fluid) { transparentFluidTags.add(fluid); };
    /** Add a new fluid tag to mark it as quite transparent (very low opacity, like water). Block must also be marked separately as transparent */
    public static void addQuiteTransparentFluidTag(TagKey<Fluid> fluid) { quiteTransparentFluidTags.add(fluid); };

    /** Add a new block tag to mark it as transparent (transmits some light) */
    public static void addTransparentBlockTag(TagKey<Block> block) { transparentBlockTags.add(block); };
    /** Add a new block tag to mark it as quite transparent (very low opacity, like glass). Block must also be marked separately as transparent */
    public static void addQuiteTransparentBlockTag(TagKey<Block> block) { quiteTransparentBlockTags.add(block); };

    public static class TransparentBlock {
        protected final TransparencyState totalTransparencyLevel;
        protected final TransparencyState blockTransparencyLevel;
        protected final TransparencyState fluidTransparencyLevel;
        protected final ColorMixState colorMix;

        public TransparentBlock(BlockState state, Level level, BlockPos pos) {
            // Short circuit on air block. 
            if (state.isAir()) {
                throw new IllegalArgumentException(
                    "Air blocks should be ignored. Don't waste time and memory on storing the transparency of air blocks."
                );
                // this.totalTransparencyLevel = TransparencyState.AIR;
                // this.colorMix = ColorMixState.AIR;
                // this.blockTransparencyLevel = TransparencyState.AIR;
                // this.fluidTransparencyLevel = TransparencyState.AIR;

                // return;
            }

            // TODO: For debugger reasons:
            Block thisBlockType = state.getBlock();
            String thisBlockName = state.getBlock().getDescriptionId();

            // Check if top or bottom face of block is "full"
            VoxelShape blockShape = state.getShape(level, pos);
            boolean isEmpty = blockShape.isEmpty();
            boolean isTopFull = Block.isFaceFull(blockShape, Direction.UP);
            boolean isBottomFull = Block.isFaceFull(blockShape, Direction.DOWN);

            // Set base transparency levels
            if (isEmpty) {
                this.blockTransparencyLevel = TransparencyState.AIR;
            } else {
                if (isTransparentBlock(state)) {
                    if (isQuiteTransparentBlock(state)) {
                        this.blockTransparencyLevel = TransparencyState.QUITE_TRANSPARENT;
                    } else {
                        this.blockTransparencyLevel = TransparencyState.SEMI_TRANSPARENT;
                    }
                } else {
                    this.blockTransparencyLevel = TransparencyState.OPAQUE;
                }
            }

            if (state.getFluidState().isEmpty()) {
                this.fluidTransparencyLevel = TransparencyState.AIR;
            } else {
                if (isTransparentFluid(state)) {
                    if (isQuiteTransparentFluid(state)) {
                        this.fluidTransparencyLevel = TransparencyState.QUITE_TRANSPARENT;
                    } else {
                        this.fluidTransparencyLevel = TransparencyState.SEMI_TRANSPARENT;
                    }
                } else {
                    this.fluidTransparencyLevel = TransparencyState.OPAQUE;
                }
            }

            // Find overall transparency state based on block shape
            if (isTopFull) {
                // Normal block transparency rules
                this.colorMix = ColorMixState.BLOCK;
                this.totalTransparencyLevel = blockTransparencyLevel;
    
            } else if (isBottomFull) {
                if (fluidTransparencyLevel == TransparencyState.AIR) {
                    // Normal block transparency rules
                    this.colorMix = ColorMixState.BLOCK;
                    this.totalTransparencyLevel = blockTransparencyLevel;

                } else {
                    // Filter block color through fluid colour based on fluid transparency rules.
                    this.colorMix = ColorMixState.FLUIDLOGGED;
    
                    // Total opacity based on highest opacity between fluid and solid.
                    this.totalTransparencyLevel = TransparencyState.max(blockTransparencyLevel, fluidTransparencyLevel);
                }

            } else if (isEmpty) {
                // Block contains only fluid
                this.colorMix = ColorMixState.FLUID;
                this.totalTransparencyLevel = fluidTransparencyLevel;

            } else { 
                // Not a solid block

                if (fluidTransparencyLevel == TransparencyState.AIR) {
                    // Normal block transparency rules, but can be at most semi-transparent due to
                    // light traveling through the gaps
                    this.colorMix = ColorMixState.BLOCK;
                    this.totalTransparencyLevel = TransparencyState.min(blockTransparencyLevel, TransparencyState.SEMI_TRANSPARENT);

                } else {
                    // Filter block color through fluid colour based on fluid transparency rules.
                    this.colorMix = ColorMixState.FLUIDLOGGED;
        
                    // Total opacity based on fluid opacity.
                    // Can only cross "quite transparent" threshold if block also quite transparent
                    // (otherwise partial block is considering "blocking too much of the light")
                    this.totalTransparencyLevel = TransparencyState.max(
                        fluidTransparencyLevel, 
                        TransparencyState.min(blockTransparencyLevel, TransparencyState.SEMI_TRANSPARENT)
                    );
                }
            }
        }

        public TransparencyState getTransparencyState() { return totalTransparencyLevel; }
        public ColorMixState getColorMixState() { return colorMix; }
    }

}
