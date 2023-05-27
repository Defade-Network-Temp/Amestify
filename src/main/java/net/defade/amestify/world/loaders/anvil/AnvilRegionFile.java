package net.defade.amestify.world.loaders.anvil;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.viewer.MapViewerWorld;
import net.defade.amestify.world.pos.RegionPos;
import net.defade.amestify.world.RegionFile;
import net.querz.mca.CompressionType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class AnvilRegionFile implements RegionFile {
    private final RegionPos regionPos;
    private final int minY;
    private final int maxY;

    private final Chunk[] chunks = new Chunk[1024];

    public AnvilRegionFile(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, Path regionFilePath, RegionPos regionPos, int minY, int maxY) throws IOException {
        this.regionPos = regionPos;
        this.minY = minY;
        this.maxY = maxY;

        loadChunks(progressTracker, mapViewerWorld, regionFilePath);
    }

    @Override
    public RegionPos getRegionPos() {
        return regionPos;
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return chunks[RegionFile.getChunkIndex(x, z)];
    }

    @Override
    public int getMinY() {
        return minY;
    }

    private void loadChunks(ProgressTracker progressTracker, MapViewerWorld mapViewerWorld, Path regionFilePath) throws IOException {
        RandomAccessFile regionFile = new RandomAccessFile(regionFilePath.toFile(), "r");

        for (int i = 0; i < 1024; i++) {
            regionFile.seek(i * 4);

            int offset = regionFile.read() << 16;
            offset |= (regionFile.read() & 0xFF) << 8;
            offset |= regionFile.read() & 0xFF;

            if (regionFile.readByte() == 0) {
                if(progressTracker != null) progressTracker.increment("Loaded " + progressTracker.getCurrent() + " out of " +
                        progressTracker.getTotal() + " chunks (" +
                        (int) (progressTracker.getProgress() * 100) + "%)");
                continue;
            }

            regionFile.seek(4096L * offset + 4); // +4: skip data size

            byte compressionScheme = regionFile.readByte();
            InputStream chunkInputStream = new FileInputStream(regionFile.getFD());
            CompressionType compressionType = CompressionType.getFromID(compressionScheme);

            Chunk chunk = new AnvilChunk(
                    mapViewerWorld,
                    RegionFile.getChunkPosFromIndex(i).add(regionPos.x() * 32, regionPos.z() * 32),
                    minY, maxY, // TODO minY and maxY
                    compressionType, chunkInputStream
            );

            if(progressTracker != null) progressTracker.increment("Loaded " + progressTracker.getCurrent() + " out of " +
                    progressTracker.getTotal() + " chunks (" +
                    (int) (progressTracker.getProgress() * 100) + "%)");
            chunks[i] = chunk;
        }

        regionFile.close();
    }
}
