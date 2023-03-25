package net.defade.amestify.utils;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import net.defade.amestify.database.MongoConnector;
import org.bson.BsonString;
import org.bson.Document;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MongoFileUploader {
    private final MongoConnector mongoConnector;

    public MongoFileUploader(MongoConnector mongoConnector) {
        this.mongoConnector = mongoConnector;
    }

    public void sendFile(Path amethystFilePath, String fileName, String fileId, String miniGameName) throws IOException {
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoConnector.getMongoDatabase(), "maps");

        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(4 * 1024) // 4ko chunks size
                .metadata(new Document("game", miniGameName));

        GridFSUploadStream gridFSUploadStream = gridFSBucket.openUploadStream(new BsonString(fileId), fileName, options);
        InputStream fileInputStream = Files.newInputStream(amethystFilePath);

        byte[] buffer = new byte[4 * 1024]; // 4ko chunks size
        int length;
        long fileLength = Files.size(amethystFilePath);

        long sentBytes = 0;

        while ((length = fileInputStream.read(buffer)) != -1) {
            sentBytes += length;
            gridFSUploadStream.write(buffer, 0, length);
        }

        JOptionPane.showMessageDialog(null, "Successfully converted the anvil world!", "Finished", JOptionPane.INFORMATION_MESSAGE);

        gridFSUploadStream.flush();
        gridFSUploadStream.close();
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
