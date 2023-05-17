package net.defade.amestify.world.savers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.defade.amestify.graphics.gui.viewer.MapViewerRegion;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.MapViewerWorld;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.biome.BiomeParser;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.Section;
import net.defade.amestify.world.loaders.RegionFile;
import net.defade.amestify.world.loaders.anvil.AnvilMapLoader;
import net.defade.amestify.world.palette.AdaptivePalette;
import net.defade.amestify.world.palette.FlexiblePalette;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class AmethystSaver {
    private final ReentrantLock fileLock = new ReentrantLock();
    private Path tempFile;
    private RandomAccessFile outputFile;

    private MapViewerWorld mapViewerWorld;

    private final AtomicInteger totalChunks = new AtomicInteger(0);
    private long chunksAmountSaveOffset;

    public CompletableFuture<Path> saveToTempFile(String worldName, String config, MapViewerWorld mapViewerWorld, ProgressTracker progressTracker) {
        if(tempFile != null) throw new IllegalStateException("Already saving a world.");

        CompletableFuture<Path> saveFuture = new CompletableFuture<>();

        this.totalChunks.set(0);
        this.mapViewerWorld = mapViewerWorld;
        progressTracker.reset(mapViewerWorld.getRegions().size() * 32 * 32);

        try {
            this.tempFile = Utils.createAmethystTempFile(worldName);
            this.outputFile = new RandomAccessFile(tempFile.toFile(), "rw");

            writeFileHeader(config);
        } catch (IOException exception) {
            saveFuture.completeExceptionally(exception);
        }

        AnvilMapLoader anvilMapLoader = new AnvilMapLoader(mapViewerWorld.getAnvilWorldPath(), -64, 320); // TODO
        anvilMapLoader.loadRegions(null, mapViewerWorld, regionFile -> {
            try {
                writeRegion(regionFile, progressTracker);
            } catch (IOException exception) {
                saveFuture.completeExceptionally(exception);
            }
        }).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                saveFuture.completeExceptionally(throwable);
                return;
            }

            try {
                outputFile.seek(chunksAmountSaveOffset);
                outputFile.writeInt(totalChunks.get());
                outputFile.close();
            } catch (IOException exception) {
                saveFuture.completeExceptionally(exception);
                return;
            }

            saveFuture.complete(tempFile);
            tempFile = null;
        });

        return saveFuture;
    }

    private void writeFileHeader(String config) throws IOException {
        System.out.println("Config: " + config);
        Collection<Biome> biomes = mapViewerWorld.unmodifiableBiomeCollection();

        outputFile.writeUTF(config);
        outputFile.writeInt(biomes.size());

        for (Biome biome : biomes) {
            outputFile.writeInt(biome.id());
            byte[] encodedBiome = BiomeParser.encode(biome);
            outputFile.writeInt(encodedBiome.length);
            outputFile.write(encodedBiome);
        }

        chunksAmountSaveOffset = outputFile.getFilePointer();
        outputFile.writeInt(0); // We don't know the amount of chunks yet, so we'll write 0 for now.
    }

    private void writeRegion(RegionFile regionFile, ProgressTracker progressTracker) throws IOException {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                Chunk chunk = regionFile.getChunk(x, z);
                if(chunk.isEmpty()) {
                    progressTracker.increment();
                    continue;
                }

                byte[] serializedChunk = encodeChunk(chunk);

                fileLock.lock();
                outputFile.writeLong(chunk.getChunkPos().getChunkIndex());
                outputFile.writeInt(serializedChunk.length);
                outputFile.write(serializedChunk);
                fileLock.unlock();

                progressTracker.increment();
            }
        }
    }

    private byte[] encodeChunk(Chunk chunk) throws IOException {
        int arraySize = Byte.BYTES + 64; // Pre-calculating the array size allows us to define the original byte array size, avoiding useless and costly resizes.

        MapViewerRegion viewerRegionContainingChunk = mapViewerWorld.getRegion(chunk.getChunkPos().x() >> 5, chunk.getChunkPos().z() >> 5);
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                Biome modifiedBiome = viewerRegionContainingChunk.getModifiedBiome(chunk.getChunkPos().x() * 16 + x, chunk.getChunkPos().z() * 16 + z);
                if(modifiedBiome != null) {
                    for (int y = -64; y < 320; y += 4) {
                        chunk.setBiome(x, y, z, modifiedBiome);
                    }
                }
            }
        }

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
        totalChunks.getAndIncrement();

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
}
