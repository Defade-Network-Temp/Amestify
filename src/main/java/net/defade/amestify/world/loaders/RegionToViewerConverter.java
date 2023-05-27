package net.defade.amestify.world.loaders;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.viewer.MapViewerRegion;
import net.defade.amestify.world.viewer.MapViewerWorld;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public class RegionToViewerConverter {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private CompletableFuture<MapViewerWorld> mapViewerWorldFuture;

    private MapViewerWorld mapViewerWorld;

    public void convert(WorldLoader worldLoader) {
        this.mapViewerWorldFuture = new CompletableFuture<>();
        try {
            this.mapViewerWorld = new MapViewerWorld(worldLoader);
        } catch (FileNotFoundException exception) {
            mapViewerWorldFuture.completeExceptionally(exception);
            return;
        }

        worldLoader.loadRegions(progressTracker, mapViewerWorld, regionFile -> {
            mapViewerWorld.addRegion(new MapViewerRegion(regionFile, mapViewerWorld.getPlainsBiome()));
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
