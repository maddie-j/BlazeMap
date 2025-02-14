package com.eerussianguy.blazemap.feature.mapping;

import java.util.function.IntFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
                do {
                    colorPOS.set(x + minX, y, z + minZ);
                    final BlockState state = level.getBlockState(colorPOS);

                    // Skip air
                    if (state.isAir()) continue;

                    // Get color from texture
                    if(state.is(BlockTags.FLOWERS)) {
                        color = getBestTexturePixel(level, state, null, this::avoidGreen);
                    } else {
                        color = getAverageTextureColor(level, state, Direction.UP);
                    }

                    if((color & Colors.ALPHA) == TINTED_FLAG) {
                        color = Colors.multiplyRGB(color, blockColors.getColor(state, level, colorPOS, 0));
                    }

                    // Fallback 1: get block tint
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
                } while (color == 0 && --y > level.getMinBuildHeight());

                if(color != 0 && color != -1) {
                    colors[x][z] = color;
                }
            }
        }
        return new BlockColorMD(colors);
    }

    private int getAverageTextureColor(Level level, BlockState state, Direction direction) {
        return colors.computeIfAbsent(state, $ -> {
            var mc = Minecraft.getInstance();
            var model = mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            var quads = model.getQuads(state, direction, level.getRandom(), EmptyModelData.INSTANCE);

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

    private int getBestTexturePixel(Level level, BlockState state, Direction direction, IntFunction<Integer> fitness) {
        return colors.computeIfAbsent(state, $ -> {
            var mc = Minecraft.getInstance();
            var model = mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            var quads = model.getQuads(state, direction, level.getRandom(), EmptyModelData.INSTANCE);
            int pixel = 0, best = Integer.MIN_VALUE;

            for(var quad : quads) {
                var texture = quad.getSprite();
                int w = texture.getWidth(), h = texture.getHeight();
                for(int x = 0; x < w; x++) {
                    for(int y = 0; y < h; y++) {
                        int color = Colors.abgr(texture.getPixelRGBA(0, x, y));
                        int score = fitness.apply(color);
                        if(score > best) {
                            best = score;
                            pixel = color;
                        }
                    }
                }
            }

            return pixel;
        });
    }

    private int avoidGreen(int pixel) {
        var channels = Colors.decomposeRGBA(pixel);
        float a = channels[0];
        int r = (int) (channels[1] * 1000);
        int g = (int) (channels[2] * 1000);
        int b = (int) (channels[3] * 1000);
        return (int) (a * (r + 1000-g + b));
    }
}
