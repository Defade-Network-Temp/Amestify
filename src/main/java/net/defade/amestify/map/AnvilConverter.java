package net.defade.amestify.map;

import net.defade.amestify.database.MongoConnector;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Void> convert() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {

        });

        return completableFuture;
    }

    public float getProgress() {
        return 0.0f;
    }
}
