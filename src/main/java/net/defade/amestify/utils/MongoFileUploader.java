package net.defade.amestify.utils;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.graphics.gui.Viewer;
import org.bson.BsonString;
import org.bson.Document;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MongoFileUploader {
    private final Viewer viewer;

    public MongoFileUploader(Viewer viewer) {
        this.viewer = viewer;
    }

    public CompletableFuture<Void> uploadFile(Path amethystFilePath, String fileName, String fileId, String miniGameName, ProgressTracker progressTracker) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        MongoConnector.THREAD_POOL.execute(() -> {
            try {
                GridFSBucket gridFSBucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase(), "maps");
                gridFSBucket.find(Filters.eq("filename", fileName)).forEach((Consumer<? super GridFSFile>) document -> gridFSBucket.delete(document.getId()));

                GridFSUploadOptions options = new GridFSUploadOptions()
                        .chunkSizeBytes(128 * 1024) // 128ko chunks size
                        .metadata(new Document("game", miniGameName));

                GridFSUploadStream gridFSUploadStream = gridFSBucket.openUploadStream(new BsonString(fileId), fileName, options);
                InputStream fileInputStream = Files.newInputStream(amethystFilePath);

                byte[] buffer = new byte[128 * 1024]; // 128ko chunks size
                long fileLength = Files.size(amethystFilePath);
                progressTracker.reset(fileLength);

                long totalSent = 0;
                int length;
                while ((length = fileInputStream.read(buffer)) != -1) {
                    gridFSUploadStream.write(buffer, 0, length);
                    totalSent += length;

                    progressTracker.increment(length);
                    progressTracker.setMessage(convertUnit(totalSent) + "/" + convertUnit(fileLength) + " (" + Math.round(totalSent / (double) fileLength * 100) + "%)");
                }

                gridFSUploadStream.flush();
                gridFSUploadStream.close();

                completableFuture.complete(null);
            } catch (Exception exception) {
                completableFuture.completeExceptionally(exception);
            }
        });

        return completableFuture;
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
