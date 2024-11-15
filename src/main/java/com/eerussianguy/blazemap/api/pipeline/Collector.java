package com.eerussianguy.blazemap.api.pipeline;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.BlazeRegistry.RegistryEntry;

/**
 * Collectors collect MasterData from chunks that need updating to be processed later.
 * This operation is executed synchronously in the main game thread.
 *
 * MasterData is consumed by Layers and Processors asynchronously in the data crunching threads.
 *
 * @author LordFokas
 */
public abstract class Collector<T extends MasterDatum> implements RegistryEntry, PipelineComponent, Producer {
    protected static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    protected final Key<Collector<MasterDatum>> id;
    protected final Key<DataType<MasterDatum>> output;

    public Collector(Key<Collector<MasterDatum>> id, Key<DataType<MasterDatum>> output) {
        this.id = id;
        this.output = output;
    }

    public Key<Collector<MasterDatum>> getID() {
        return id;
    }

    public abstract T collect(Level level, int minX, int minZ, int maxX, int maxZ);

    @Override
    public Key<DataType<MasterDatum>> getOutputID() {
        return output;
    }

    protected static boolean isWater(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return state.getFluidState().is(FluidTags.WATER);
    }

    protected static boolean isLeavesOrReplaceable(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return state.is(BlockTags.LEAVES) || state.isAir() || state.canBeReplaced();
    }

    // == Transparency ==
    // TODO: The below should probably be moved somewhere else eventually

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

    private static final Set<Class<?>> transparentClasses = initialiseTransparentClassSet(false);
    private static final Set<Class<?>> quiteTransparentClasses = initialiseTransparentClassSet(true);

    private static final Set<TagKey<Fluid>> transparentFluidTags = initialiseTransparentFluidSet(false);
    private static final Set<TagKey<Fluid>> quiteTransparentFluidTags = initialiseTransparentFluidSet(true);

    private static final Set<TagKey<Block>> transparentBlockTags = initialiseTransparentBlockSet(false);
    private static final Set<TagKey<Block>> quiteTransparentBlockTags = initialiseTransparentBlockSet(true);


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


    /**
     * Eg: Water, slime, honey, ice, glass
     */
    protected static boolean isTransparent(BlockState testBlockState) {
        Block testBlock = testBlockState.getBlock();
        FluidState testFluid = testBlockState.getFluidState();

        for (Class<?> transparentClass : transparentClasses) {
            if (transparentClass.isAssignableFrom(testBlock.getClass())) {
                return true;
            }
        }

        for (TagKey<Fluid> transparentFluid: transparentFluidTags) {
            if (testFluid.is(transparentFluid)) {
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

        // return (testBlockState.getBlock() instanceof HalfTransparentBlock) || testBlockState.getFluidState().is(FluidTags.WATER);
    }

    /**
     * Pretty much only glass and water by default
     */
    protected static boolean isQuiteTransparent(BlockState testBlockState) {
        Block testBlock = testBlockState.getBlock();
        FluidState testFluid = testBlockState.getFluidState();

        for (Class<?> transparentClass : quiteTransparentClasses) {
            if (transparentClass.isAssignableFrom(testBlock.getClass())) {
                return true;
            }
        }

        for (TagKey<Fluid> transparentFluid: quiteTransparentFluidTags) {
            if (testFluid.is(transparentFluid)) {
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

        // return (state.getBlock() instanceof AbstractGlassBlock) || state.getFluidState().is(FluidTags.WATER);
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

}
