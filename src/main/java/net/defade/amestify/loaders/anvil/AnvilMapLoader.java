package net.defade.amestify.loaders.anvil;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.World;
import net.defade.amestify.world.chunk.pos.RegionPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class AnvilMapLoader {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final Path regionPath;
    private final int minY;
    private final int maxY;

    public AnvilMapLoader(Path anvilPath, int minY, int maxY) {
        this.regionPath = anvilPath.resolve("region");
        this.minY = minY;
        this.maxY = maxY;
    }

    private CompletableFuture<RegionFile> loadRegion(World world, RegionPos regionPos, ProgressTracker progressTracker) {
        CompletableFuture<RegionFile> completableFuture = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    completableFuture.complete(new RegionFile(progressTracker, world, regionPath.resolve("r." + regionPos.x() + "." + regionPos.z() + ".mca"), regionPos, minY, maxY));
                } catch (Throwable exception) {
                    completableFuture.completeExceptionally(exception);
                }
            });


        return completableFuture;
    }

    public CompletableFuture<World> loadWorld(ProgressTracker progressTracker) {
        CompletableFuture<World> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                World world = new World();
                List<Path> regionFiles = getRegionFiles();
                final int chunksToLoad = regionFiles.size() * 1024;
                progressTracker.reset(chunksToLoad);

                List<CompletableFuture<RegionFile>> loadingFutures = new ArrayList<>();

                for (Path regionFilePath : regionFiles) {
                    String fileName = regionFilePath.getFileName().toString();
                    if (!fileName.endsWith(".mca")) {
                        return;
                    }

                    String[] split = fileName.split("\\.");
                    int regionX = Integer.parseInt(split[1]);
                    int regionZ = Integer.parseInt(split[2]);

                    loadingFutures.add(loadRegion(world, new RegionPos(regionX, regionZ), progressTracker).whenComplete((regionFile, throwable) -> {
                        if(throwable != null) {
                            completableFuture.completeExceptionally(throwable);
                            executor.shutdownNow();
                            return;
                        }

                        world.addRegion(regionFile);
                    }));
                }

                CompletableFuture.allOf(loadingFutures.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
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
