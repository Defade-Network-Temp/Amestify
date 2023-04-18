package net.defade.amestify.loaders;

import com.github.luben.zstd.ZstdOutputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.defade.amestify.world.World;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.biome.BiomeParser;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.Section;
import net.defade.amestify.world.palette.AdaptivePalette;
import net.defade.amestify.world.palette.FlexiblePalette;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
public class AmethystSaver {
    /*
    * TODO: Chunks are not kept in memory anymore, so we need
    *  to re-load the map, apply the biome changes on the chunks,
    *  and then save the map.
    private final String config = ""; // TODO

    private final DataOutputStream dataOutputStream;
    private final World world;

    public AmethystSaver(OutputStream outputStream, World world) throws IOException {
        this.dataOutputStream = new DataOutputStream(new ZstdOutputStream(outputStream));
        this.world = world;
    }

    public CompletableFuture<Void> save() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            final Collection<Biome> biomes = world.unmodifiableBiomeCollection();
            final Collection<Chunk> chunks = world.getChunks();

            try {
                dataOutputStream.writeUTF(config);
                dataOutputStream.writeInt(biomes.size());

                for (Biome biome : biomes) {
                    dataOutputStream.writeInt(biome.id());
                    byte[] encodedBiome = BiomeParser.encode(biome);
                    dataOutputStream.writeInt(encodedBiome.length);
                    dataOutputStream.write(encodedBiome);
                }

                dataOutputStream.writeInt(chunks.size());

                int totalConvertedChunks = 0;

                for (Chunk chunk : chunks) {
                    byte[] serializedChunk = encodeChunk(chunk);

                    dataOutputStream.writeLong(chunk.getChunkPos().getChunkIndex());
                    dataOutputStream.writeInt(serializedChunk.length);
                    dataOutputStream.write(serializedChunk);
                }

                dataOutputStream.flush();
                dataOutputStream.close();

                completableFuture.complete(null);
            } catch (Exception exception) {
                completableFuture.completeExceptionally(exception);
            }
        });

        return completableFuture;
    }

    private byte[] encodeChunk(Chunk chunk) throws IOException {
        int arraySize = Byte.BYTES + 64; // Pre-calculating the array size allows us to define the original byte array size, avoiding useless and costly resizes.
        for (int i = 0; i < chunk.getSections().size(); i++) {
            Section section = chunk.getSections().get(i);
            AdaptivePalette blockPalette = section.getBlockPalette();
            AdaptivePalette biomePalette = section.getBiomePalette();

            arraySize = arraySize + Byte.BYTES;
            if (blockPalette.isFilledPalette()) {
                arraySize = arraySize + Short.BYTES;
            } else {
                FlexiblePalette flexiblePalette = blockPalette.getAsFlexiblePalette();
                arraySize = arraySize + Byte.BYTES + Short.BYTES + (flexiblePalette.getPaletteToValueList().size() * Short.BYTES) + Short.BYTES + Short.BYTES + (flexiblePalette.values().length * Long.BYTES);
            }

            if (biomePalette.isFilledPalette()) {
                arraySize = arraySize + Short.BYTES;
            } else {
                FlexiblePalette flexiblePalette = biomePalette.getAsFlexiblePalette();
                arraySize = arraySize + Byte.BYTES + Short.BYTES + (flexiblePalette.getPaletteToValueList().size() * Short.BYTES) + Short.BYTES + Short.BYTES + (flexiblePalette.values().length * Long.BYTES);
            }
        }

        arraySize = arraySize + Integer.BYTES;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(arraySize);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.write((byte) chunk.getSections().size());

        for (Section section : chunk.getSections()) {
            savePalette(section.getBlockPalette(), dataOutputStream);
            savePalette(section.getBiomePalette(), dataOutputStream);

            byte[] blockLightData = section.getBlockLight();
            dataOutputStream.writeShort(blockLightData.length);
            dataOutputStream.write(blockLightData);

            byte[] skyLightData = section.getSkyLight();
            dataOutputStream.writeShort(skyLightData.length);
            dataOutputStream.write(skyLightData);
        }

        dataOutputStream.writeInt(0); //TODO: Save block entities

        return byteArrayOutputStream.toByteArray();
    }

    private static void savePalette(AdaptivePalette palette, DataOutputStream dataOutputStream) throws IOException {
        if (palette.isFilledPalette()) {
            dataOutputStream.write(0);
            dataOutputStream.writeShort((short) palette.get(0, 0, 0));
        } else {
            FlexiblePalette flexiblePalette = palette.getAsFlexiblePalette();
            dataOutputStream.write(1);
            dataOutputStream.write((byte) flexiblePalette.bitsPerEntry());

            IntArrayList biomePaletteToValueList = flexiblePalette.getPaletteToValueList();
            dataOutputStream.writeShort((short) biomePaletteToValueList.size());
            for (int value : biomePaletteToValueList) {
                dataOutputStream.writeShort((short) value);
            }

            dataOutputStream.writeShort((short) flexiblePalette.count());

            long[] biomeValues = flexiblePalette.values();
            dataOutputStream.writeShort((short) biomeValues.length);
            for (long biomeValue : biomeValues) {
                dataOutputStream.writeLong(biomeValue);
            }
        }
    }
     */
}
