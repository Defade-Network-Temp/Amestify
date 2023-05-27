package net.defade.amestify.world.loaders.anvil;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.loaders.WorldLoader;
import net.defade.amestify.world.viewer.MapViewerWorld;
import net.defade.amestify.world.pos.RegionPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AnvilMapLoader implements WorldLoader {
    private final Path regionPath;
    private final int minY;
    private final int maxY;

    public AnvilMapLoader(Path anvilPath, int minY, int maxY) {
        this.regionPath = anvilPath.resolve("region");
        this.minY = minY;
        this.maxY = maxY;
    }

    private CompletableFuture<AnvilRegionFile> loadRegion(MapViewerWorld mapViewerWorld, RegionPos regionPos, ProgressTracker progressTracker) {
        CompletableFuture<AnvilRegionFile> completableFuture = new CompletableFuture<>();
            THREAD_POOL.submit(() -> {
                try {
                    completableFuture.complete(new AnvilRegionFile(progressTracker, mapViewerWorld, regionPath.resolve("r." + regionPos.x() + "." + regionPos.z() + ".mca"), regionPos, minY, maxY));
                } catch (Throwable exception) {
                    completableFuture.completeExceptionally(exception);
                }
            });


        return completableFuture;
    }

    @Override
    public CompletableFuture<Void> loadRegions(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, Consumer<RegionFile> consumer) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                List<Path> regionFiles = getRegionFiles();
                final int chunksToLoad = regionFiles.size() * 1024;
                if(progressTracker != null) progressTracker.reset(chunksToLoad);

                List<CompletableFuture<AnvilRegionFile>> loadingFutures = new ArrayList<>();

                for (Path regionFilePath : regionFiles) {
                    String fileName = regionFilePath.getFileName().toString();
                    if (!fileName.endsWith(".mca")) {
                        return;
                    }

                    String[] split = fileName.split("\\.");
                    int regionX = Integer.parseInt(split[1]);
                    int regionZ = Integer.parseInt(split[2]);

                    loadingFutures.add(loadRegion(mapViewerWorld, new RegionPos(regionX, regionZ), progressTracker).whenComplete((region, throwable) -> {
                        if(throwable != null) {
                            completableFuture.completeExceptionally(throwable);
                            return;
                        }

                        consumer.accept(region);
                    }));
                }

                CompletableFuture.allOf(loadingFutures.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        completableFuture.completeExceptionally(throwable);
                    } else {
                        completableFuture.complete(null);
                    }
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
