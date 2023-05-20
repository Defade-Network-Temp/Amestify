package net.defade.amestify.world;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.loaders.anvil.AnvilMapLoader;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AnvilToViewerRegionConverter {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private CompletableFuture<MapViewerWorld> mapViewerWorldFuture;

    private MapViewerWorld mapViewerWorld;

    public void convert(Path worldPath) {
        this.mapViewerWorldFuture = new CompletableFuture<>();
        try {
            this.mapViewerWorld = new MapViewerWorld(worldPath);
        } catch (FileNotFoundException exception) {
            mapViewerWorldFuture.completeExceptionally(exception);
            return;
        }

        AnvilMapLoader anvilMapLoader = new AnvilMapLoader(worldPath, -64, 320);

        anvilMapLoader.loadRegions(progressTracker, mapViewerWorld, anvilRegionFile -> {
            mapViewerWorld.addRegion(new MapViewerRegion(anvilRegionFile, mapViewerWorld.getPlainsBiome()));
        }).whenComplete((unused, throwable) -> {
            if(throwable != null) {
                mapViewerWorldFuture.completeExceptionally(throwable);
            } else {
                mapViewerWorldFuture.complete(mapViewerWorld);
            }
        });
    }

    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public CompletableFuture<MapViewerWorld> getMapViewerWorldFuture() {
        return mapViewerWorldFuture;
    }

    public void reset() {
        mapViewerWorldFuture = null;
        mapViewerWorld = null;
    }
}
