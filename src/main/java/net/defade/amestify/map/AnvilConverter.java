package net.defade.amestify.map;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.utils.ProgressDialog;
import net.defade.yokura.amethyst.AmethystChunkLoader;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import org.bson.BsonString;
import org.bson.Document;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class AnvilConverter {
    private final MongoConnector mongoConnector;
    private final Path anvilFolderPath;
    private final String fileName;
    private final String fileId;
    private final String miniGameName;

    public AnvilConverter(MongoConnector mongoConnector, Path anvilFolderPath, String fileName, String fileId, String miniGameName) {
        this.mongoConnector = mongoConnector;
        this.anvilFolderPath = anvilFolderPath;
        this.fileName = fileName;
        this.fileId = fileId;
        this.miniGameName = miniGameName;
    }

    public CompletableFuture<Void> convert(ProgressDialog progressDialog) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                progressDialog.setTitle("Converting...");
                progressDialog.setMessage("Converting anvil world to amethyst format...");

                InstanceContainer instance = new InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD, new AnvilLoader(anvilFolderPath));

                List<CompletableFuture<Chunk>> chunks = getChunks(instance);
                progressDialog.setIndeterminateProgress(false);
                progressDialog.setMaximumValue(chunks.size());

                AtomicInteger totalLoadedChunks = new AtomicInteger(0);
                for (CompletableFuture<Chunk> chunksFuture : chunks) {
                    chunksFuture.whenComplete((chunk, throwable) -> {
                        int loadedChunks = totalLoadedChunks.getAndIncrement();
                        progressDialog.setBarText(loadedChunks + " / " + chunks.size() + " chunks");
                        progressDialog.setValue(loadedChunks);
                    });
                }

                CompletableFuture<?>[] chunksArray = new CompletableFuture[chunks.size()];
                CompletableFuture.allOf(chunks.toArray(chunksArray)).join();

                for (Chunk chunk : instance.getChunks()) {
                    boolean empty = true;
                    for (Section section : chunk.getSections()) {
                        if(!section.blockPalette().isFilledPalette()) {
                            if(section.blockPalette().getAsFlexiblePalette().count() >= 0) {
                                empty = false;
                                break;
                            }
                        } else if(Block.fromStateId((short) section.blockPalette().get(0, 0, 0)).id() != Block.AIR.id()) {
                            empty = false;
                            break;
                        }
                    }

                    if(empty) {
                        instance.unloadChunk(chunk);
                        System.out.println("Unloaded empty chunk " + chunk.getChunkX() + ", " + chunk.getChunkZ());
                    }
                }

                AmethystFileSource amethystFileSource = new AmethystFileSource(fileName);
                instance.setChunkLoader(new AmethystChunkLoader(amethystFileSource));
                instance.saveChunksToStorage().join();
                instance.saveInstance().join();


                GridFSBucket gridFSBucket = GridFSBuckets.create(mongoConnector.getMongoDatabase(), "maps");

                GridFSUploadOptions options = new GridFSUploadOptions()
                        .chunkSizeBytes(4 * 1024) // 4ko chunks size
                        .metadata(new Document("game", miniGameName));

                GridFSUploadStream outputStream = gridFSBucket.openUploadStream(new BsonString(fileId), fileName, options);
                InputStream fileInputStream = Files.newInputStream(amethystFileSource.getFile());
                byte[] buffer = new byte[4 * 1024]; // 4ko chunks size
                int length;
                long fileLength = Files.size(amethystFileSource.getFile());

                progressDialog.setTitle("Sending...");
                progressDialog.setMessage("Sending the amethyst world to the database...");
                progressDialog.setMaximumValue((int) fileLength);
                progressDialog.setIndeterminateProgress(false);

                long sentBytes = 0;

                while ((length = fileInputStream.read(buffer)) != -1) {
                    sentBytes += length;
                    outputStream.write(buffer, 0, length);
                    progressDialog.setValue((int) sentBytes);
                    progressDialog.setBarText(convertUnit(sentBytes) + " / " + convertUnit(fileLength) + " transferred");
                }

                outputStream.flush();
                outputStream.close();

                progressDialog.close();
                JOptionPane.showMessageDialog(null, "Successfully converted and sent the world to the database.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        });

        return completableFuture;
    }

    private List<CompletableFuture<Chunk>> getChunks(InstanceContainer instanceContainer) throws IOException {
        List<CompletableFuture<Chunk>> chunks = new ArrayList<>();
        Path regionFolder = anvilFolderPath.resolve("region");
        Stream<Path> regionFiles = Files.list(regionFolder);

        regionFiles.forEach(path -> {
            String fileName = path.getFileName().toString();
            String[] split = fileName.split("\\.");
            int chunkX = Integer.parseInt(split[1]) * 32;
            int chunkZ = Integer.parseInt(split[2]) * 32;

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    chunks.add(instanceContainer.loadChunk(chunkX + x, chunkZ + z));
                }
            }
        });

        regionFiles.close();

        return chunks;
    }

    private static String convertUnit(long bytes) {
        if(bytes < 1024 * 1024) {
            return Math.round(bytes / 1024D * 100) / 100D + "ko";
        } else if(bytes < 1024 * 1024 * 1024) {
            return Math.round(bytes / 1024D / 1024D * 100) / 100D + "mo";
        } else {
            return Math.round(bytes / 1024D / 1024D / 1024D * 100) / 100D + "go";
        }
    }
}
