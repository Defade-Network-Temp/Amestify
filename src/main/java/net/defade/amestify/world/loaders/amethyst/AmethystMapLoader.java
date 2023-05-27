package net.defade.amestify.world.loaders.amethyst;

import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.biome.BiomeParser;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.loaders.WorldLoader;
import net.defade.amestify.world.pos.ChunkPos;
import net.defade.amestify.world.pos.RegionPos;
import net.defade.amestify.world.viewer.MapViewerWorld;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class AmethystMapLoader implements WorldLoader {
    private final Viewer viewer;
    private final Path path;

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private RandomAccessFile file;

    private final Map<Integer, Integer> biomes = new HashMap<>();
    private final Map<RegionPos, List<Long>> regionChunksIndexes = new HashMap<>();

    public AmethystMapLoader(Viewer viewer, Path path) {
        this.viewer = viewer;
        this.path = path;
    }

    @Override
    public CompletableFuture<Void> loadRegions(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, Consumer<RegionFile> regionFileConsumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        THREAD_POOL.submit(() -> {
            try {
                file = new RandomAccessFile(path.toFile(), "r");

                loadWorld(mapViewerWorld);
                loadChunksIndexes();

                int totalChunks = 0;
                for (List<Long> chunksIndexes : regionChunksIndexes.values()) {
                    totalChunks += chunksIndexes.size();
                }
                if(progressTracker != null) progressTracker.reset(totalChunks);

                List<CompletableFuture<RegionFile>> regionFutures = new ArrayList<>();
                for (RegionPos regionPos : regionChunksIndexes.keySet()) {
                    CompletableFuture<RegionFile> regionFuture = loadRegion(progressTracker, mapViewerWorld, regionPos);

                    regionFuture.whenComplete((regionFile, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            regionFileConsumer.accept(regionFile);
                        }
                    });

                    regionFutures.add(regionFuture);
                }

                CompletableFuture.allOf(regionFutures.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(null);
                        try {
                            file.close();
                        } catch (IOException ignored) { }
                    }
                });

            } catch (IOException exception) {
                exception.printStackTrace();
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    private void loadWorld(MapViewerWorld mapViewerWorld) throws IOException {
        String config = file.readUTF();
        viewer.getWorldConfig().set(config);

        int biomesAmount = file.readInt();
        for (int i = 0; i < biomesAmount; i++) {
            int biomeId = file.readInt();
            byte[] biome = new byte[file.readInt()];
            file.read(biome);

            Biome decodedBiome = BiomeParser.decode(biome);
            if (mapViewerWorld.getBiomeByName(decodedBiome.name()) == null) {
                mapViewerWorld.registerBiome(decodedBiome);
            }

            biomes.put(biomeId, mapViewerWorld.getBiomeByName(decodedBiome.name()).id());
        }
    }

    private void loadChunksIndexes() throws IOException {
        int chunksAmount = file.readInt();
        for (int i = 0; i < chunksAmount; i++) {
            long fileIndex = file.getFilePointer();
            long chunkPos = file.readLong();
            int x = Utils.getChunkCoordX(chunkPos);
            int z = Utils.getChunkCoordZ(chunkPos);

            RegionPos regionPos = new RegionPos(x >> 5, z >> 5);
            if(!regionChunksIndexes.containsKey(regionPos)) {
                List<Long> chunksIndexes = new ArrayList<>();
                chunksIndexes.add(fileIndex);
                regionChunksIndexes.put(regionPos, chunksIndexes);
            } else {
                regionChunksIndexes.get(regionPos).add(fileIndex);
            }

            file.skipBytes(file.readInt());
        }
    }

    private CompletableFuture<RegionFile> loadRegion(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, RegionPos regionPos) throws IOException {
        CompletableFuture<RegionFile> regionFuture = new CompletableFuture<>();
        List<CompletableFuture<Chunk>> loadingChunks = new ArrayList<>();

        for (long chunkIndex : regionChunksIndexes.get(regionPos)) {
            CompletableFuture<Chunk> chunkFuture = new CompletableFuture<>();
            loadingChunks.add(chunkFuture);

            reentrantLock.lock();
            file.seek(chunkIndex);

            long chunkPos = file.readLong();
            int x = Utils.getChunkCoordX(chunkPos);
            int z = Utils.getChunkCoordZ(chunkPos);

            byte[] chunkData = new byte[file.readInt()];
            file.read(chunkData);

            reentrantLock.unlock();

            Chunk chunk = new AmethystChunk(mapViewerWorld, new ChunkPos(x, z), -64, 320,
                    biomes, ByteBuffer.wrap(chunkData)); // TODO

            if(progressTracker != null) progressTracker.increment("Loaded " + progressTracker.getCurrent() + " out of " +
                    progressTracker.getTotal() + " chunks (" + (int) (progressTracker.getProgress() * 100) + "%");
            chunkFuture.complete(chunk);
        }

        CompletableFuture.allOf(loadingChunks.toArray(new CompletableFuture[0])).thenRun(() -> {
            List<Chunk> chunks = loadingChunks.stream().map(CompletableFuture::join).toList();
            regionFuture.complete(new AmethystRegionFile(regionPos, -64, chunks));
        });

        return regionFuture;
    }
}
