package net.defade.amestify.world.loaders.amethyst;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.Section;
import net.defade.amestify.world.chunk.palette.FlexiblePalette;
import net.defade.amestify.world.pos.ChunkPos;
import net.defade.amestify.world.viewer.MapViewerWorld;
import java.nio.ByteBuffer;
import java.util.Map;

public class AmethystChunk extends Chunk {
    public AmethystChunk(MapViewerWorld mapViewerWorld, ChunkPos chunkPos, int minY, int maxY, Map<Integer, Integer> biomeMap, ByteBuffer buffer) {
        super(mapViewerWorld, chunkPos, minY, maxY);

        byte sectionAmount = buffer.get();
        for (byte sectionIndex = 0; sectionIndex < sectionAmount; sectionIndex++) {
            Section section = sections.get(sectionIndex);
            byte blockPaletteType = buffer.get();

            if (blockPaletteType == 0) {
                section.getBlockPalette().fill(buffer.getShort());
            } else {
                byte blockBitsPerEntry = buffer.get();

                short blockMapSize = buffer.getShort();
                IntArrayList blockPaletteToValueList = new IntArrayList(blockMapSize);
                for (int i = 0; i < blockMapSize; i++) {
                    blockPaletteToValueList.add(buffer.getShort());
                }

                short blockCount = buffer.getShort();

                long[] blockValues = new long[buffer.getShort()];
                buffer.asLongBuffer().get(blockValues);
                buffer.position(buffer.position() + (blockValues.length * 8));

                FlexiblePalette flexiblePalette = new FlexiblePalette(section.getBlockPalette(),
                        blockBitsPerEntry, blockPaletteToValueList, blockCount, blockValues);
                section.getBlockPalette().setPalette(flexiblePalette);
            }

            byte biomePaletteType = buffer.get();

            if (biomePaletteType == 0) {
                section.getBiomePalette().fill(buffer.getShort());
            } else {
                byte biomeBitsPerEntry = buffer.get();

                short biomeMapSize = buffer.getShort();
                IntArrayList biomePaletteToValueList = new IntArrayList(biomeMapSize);
                for (int i = 0; i < biomeMapSize; i++) {
                    biomePaletteToValueList.add((int) biomeMap.get((int) buffer.getShort()));
                }

                short biomeCount = buffer.getShort();

                long[] biomeValues = new long[buffer.getShort()];
                buffer.asLongBuffer().get(biomeValues);
                buffer.position(buffer.position() + (biomeValues.length * 8));

                FlexiblePalette flexiblePalette = new FlexiblePalette(section.getBiomePalette(),
                        biomeBitsPerEntry, biomePaletteToValueList, biomeCount, biomeValues);
                section.getBiomePalette().setPalette(flexiblePalette);
            }

            byte[] blockLightData = new byte[buffer.getShort()];
            buffer.get(blockLightData);
            section.setBlockLight(blockLightData);

            byte[] skyLightData = new byte[buffer.getShort()];
            buffer.get(skyLightData);
            section.setSkyLight(skyLightData);
        }

        int blockEntitiesAmount = buffer.getInt();
        for (int i = 0; i < blockEntitiesAmount; i++) {
            buffer.position(buffer.position() + 6);
            if ((buffer.get() & 1) == 1) {
                buffer.position(buffer.getShort());
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxY - 1; y > minY; y--) {
                    if (getBlockState(x, y, z) != 0) {
                        heightMap[x + (z << 4)] = y - minY + 1;
                        break;
                    }
                }
            }
        }
    }
}
