package net.defade.amestify.world;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.loaders.WorldLoader;
import net.defade.amestify.world.pos.RegionPos;
import net.defade.amestify.world.viewer.MapViewerRegion;
import net.defade.amestify.world.viewer.MapViewerWorld;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public interface RegionFile {
    RegionPos getRegionPos();

    Chunk getChunk(int x, int z);

    int getMinY();

    static CompletableFuture<MapViewerWorld> convert(ProgressTracker progressTracker, WorldLoader worldLoader) {
        final CompletableFuture<MapViewerWorld> mapViewerWorldFuture = new CompletableFuture<>();
        final MapViewerWorld mapViewerWorld;

        try {
            mapViewerWorld = new MapViewerWorld(worldLoader);
        } catch (FileNotFoundException exception) {
            mapViewerWorldFuture.completeExceptionally(exception);
            return mapViewerWorldFuture;
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

        return mapViewerWorldFuture;
    }
}
