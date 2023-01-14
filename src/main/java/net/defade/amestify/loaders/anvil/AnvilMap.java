package net.defade.amestify.loaders.anvil;

import net.defade.amestify.utils.ProgressDialog;
import net.defade.amestify.world.World;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.RegionPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class AnvilMap {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final Path regionPath;
    private final int minY;
    private final int maxY;

    private final Map<RegionPos, RegionFile> regionFileCache = new HashMap<>();

    public AnvilMap(Path anvilPath, int minY, int maxY) {
        this.regionPath = anvilPath.resolve("region");
        this.minY = minY;
        this.maxY = maxY;
    }

    public CompletableFuture<RegionFile> loadRegion(RegionPos regionPos) {
        CompletableFuture<RegionFile> completableFuture = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    RegionFile regionFile = regionFileCache.get(regionPos);

                    if (regionFile == null) {
                        regionFile = new RegionFile(regionPath.resolve("r." + regionPos.x() + "." + regionPos.z() + ".mca"), regionPos, minY, maxY);
                        regionFileCache.put(regionPos, regionFile);
                    }

                    completableFuture.complete(regionFile);
                } catch (Throwable exception) {
                    completableFuture.completeExceptionally(exception);
                }
            });


        return completableFuture;
    }

    public CompletableFuture<World> loadWorld(ProgressDialog progressDialog) {
        CompletableFuture<World> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                World world = new World();
                List<Path> regionFiles = getRegionFiles();
                final int chunksToLoad = regionFiles.size() * 1024;

                List<CompletableFuture<RegionFile>> loadingFutures = new ArrayList<>();

                for (Path regionFilePath : regionFiles) {
                    String fileName = regionFilePath.getFileName().toString();
                    if (!fileName.endsWith(".mca")) {
                        return;
                    }

                    String[] split = fileName.split("\\.");
                    int regionX = Integer.parseInt(split[1]);
                    int regionZ = Integer.parseInt(split[2]);

                    loadingFutures.add(loadRegion(new RegionPos(regionX, regionZ)).whenComplete((regionFile, throwable) -> {
                        if(throwable != null) {
                            completableFuture.completeExceptionally(throwable);
                            return;
                        }

                        for (Chunk chunk : regionFile.getChunks()) {
                            progressDialog.setValue(progressDialog.getValue() + 1);
                            progressDialog.setBarText(progressDialog.getValue() + "/" + chunksToLoad + " chunks loaded.");

                            if(chunk == null) continue;
                            progressDialog.setMessage("Loading chunk [x=" + chunk.getChunkPos().x() + ", z=" + chunk.getChunkPos().z() + "]");
                            world.addChunk(chunk);
                        }
                    }));
                }

                progressDialog.setIndeterminateProgress(false);
                progressDialog.setTitle("Loading...");
                progressDialog.setMaximumValue(chunksToLoad);

                CompletableFuture.allOf(loadingFutures.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
                    if(throwable != null) {
                        completableFuture.completeExceptionally(throwable);
                        return;
                    }
                    completableFuture.complete(world);
                });


            } catch (Throwable exception) {
                completableFuture.completeExceptionally(exception);
            }
        });

        return completableFuture;
    }

    private List<Path> getRegionFiles() throws IOException {
        Stream<Path> regionFilesStream = Files.list(regionPath);
        List<Path> regionFiles = regionFilesStream.toList();

        regionFilesStream.close();

        return regionFiles;
    }
}
