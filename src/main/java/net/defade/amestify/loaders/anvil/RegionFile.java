package net.defade.amestify.loaders.anvil;

import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.ChunkPos;
import net.defade.amestify.world.chunk.pos.RegionPos;
import net.querz.mca.CompressionType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class RegionFile {
    private final Chunk[] chunks = new Chunk[1024];

    public RegionFile(ProgressTracker progressTracker, Path regionFilePath, RegionPos regionPos, int minY, int maxY) throws IOException {
        RandomAccessFile regionFile = new RandomAccessFile(regionFilePath.toFile(), "r");

        for (int i = 0; i < 1024; i++) {
            regionFile.seek(i * 4);

            int offset = regionFile.read() << 16;
            offset |= (regionFile.read() & 0xFF) << 8;
            offset |= regionFile.read() & 0xFF;

            if (regionFile.readByte() == 0) {
                if(progressTracker != null) progressTracker.increment();
                continue;
            }

            regionFile.seek(4096L * offset + 4); // +4: skip data size

            byte compressionScheme = regionFile.readByte();
            InputStream chunkInputStream = new FileInputStream(regionFile.getFD());
            CompressionType compressionType = CompressionType.getFromID(compressionScheme);

            Chunk chunk = new Chunk(
                    getChunkPosFromIndex(i).add(regionPos.x() * 32, regionPos.z() * 32),
                    minY, maxY, // TODO minY and maxY
                    compressionType, chunkInputStream
            );
            if(progressTracker != null) progressTracker.increment();
            if(chunk.isEmpty()) {
                continue;
            }

            chunks[i] = chunk;
        }

        regionFile.close();
    }

    public Chunk getChunk(ChunkPos chunkPos) {
        return chunks[getChunkIndex(chunkPos)];
    }

    public List<Chunk> getChunks() {
        return Arrays.asList(chunks);
    }

    public static int getChunkIndex(ChunkPos chunkPos) {
        return (chunkPos.x() & 0x1F) + ((chunkPos.z() & 0x1F) << 5);
    }

    public static ChunkPos getChunkPosFromIndex(int index) {
        return new ChunkPos(index & 0x1f, index >> 5);
    }
}
