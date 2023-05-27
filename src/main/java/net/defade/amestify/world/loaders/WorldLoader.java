package net.defade.amestify.world.loaders;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.viewer.MapViewerWorld;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public interface WorldLoader {
    ExecutorService THREAD_POOL = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 2));

    CompletableFuture<Void> loadRegions(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, Consumer<RegionFile> regionFileConsumer);
}
