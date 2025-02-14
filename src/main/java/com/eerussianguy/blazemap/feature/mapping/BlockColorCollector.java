package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MaterialColor;

import net.minecraftforge.client.model.data.EmptyModelData;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;
import com.eerussianguy.blazemap.lib.Colors;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

public class BlockColorCollector extends ClientOnlyCollector<BlockColorMD> {
    private static final int TINTED_FLAG = 0xFA000000;
    private static final Object2IntArrayMap<BlockState> colors = new Object2IntArrayMap<>();

    public BlockColorCollector() {
        super(
            BlazeMapReferences.Collectors.BLOCK_COLOR,
            BlazeMapReferences.MasterData.BLOCK_COLOR
        );
    }

    @Override
    public BlockColorMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] colors = new int[16][16];
        final BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        final MutableBlockPos colorPOS = new MutableBlockPos();

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, minX + x, minZ + z);

                int color = 0;
                while(color == 0 && y > level.getMinBuildHeight()) {
                    colorPOS.set(x + minX, y, z + minZ);
                    final BlockState state = level.getBlockState(colorPOS);

                    // Skip air
                    if (state.isAir()) {
                        y--;
                        continue;
                    }

                    // Get color from top texture
                    color = getTopFaceColor(level, state);
                    if((color & TINTED_FLAG) == TINTED_FLAG) {
                        color = Colors.multiplyRGB(color, blockColors.getColor(state, level, colorPOS, 0));
                    }

                    // Fallback 1: get block color
                    if(color == 0) {
                        color = blockColors.getColor(state, level, colorPOS, 0);
                    }

                    // Fallback 2: get block map color
                    if(color <= 0) {
                        MaterialColor mapColor = state.getMapColor(level, colorPOS);
                        if(mapColor != MaterialColor.NONE) {
                            color = mapColor.col;
                        }
                    }

                    y--;
                }

                if(color != 0 && color != -1) {
                    colors[x][z] = color;
                }
            }
        }
        return new BlockColorMD(colors);
    }

    private int getTopFaceColor(Level level, BlockState state) {
        return colors.computeIfAbsent(state, $ -> {
            var mc = Minecraft.getInstance();
            var model = mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            var quads = model.getQuads(state, Direction.UP, level.getRandom(), EmptyModelData.INSTANCE);

            int flag = 0;
            int r = 0, g = 0, b = 0, total = 0;

            for(var quad : quads) {
                if(quad.isTinted()) {
                    flag = TINTED_FLAG;
                }
                var texture = quad.getSprite();
                int w = texture.getWidth(), h = texture.getHeight();
                for(int x = 0; x < w; x++) {
                    for(int y = 0; y < h; y++) {
                        var pixel = Colors.decomposeRGBA(texture.getPixelRGBA(0, x, y));
                        float alpha = pixel[0];
                        if(alpha < 0.05F) continue;
                        r += (255 * pixel[3] * alpha);
                        g += (255 * pixel[2] * alpha);
                        b += (255 * pixel[1] * alpha);
                        total++;
                    }
                }
            }

            // prevent dumb math and division by zero early
            if(total == 0) return 0;

            r /= total;
            g /= total;
            b /= total;

            return flag | (r << 16) | (g << 8) | b;
        });
    }
}
