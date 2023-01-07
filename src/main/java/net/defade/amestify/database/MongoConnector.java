package net.defade.amestify.database;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;

public class MongoConnector {
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    public CompletableFuture<Void> connect(String host, int port, String username, char[] password, String authDatabase) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            MongoCredential mongoCredential = MongoCredential.createCredential(username, authDatabase, password);
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder().
                            applyConnectionString(new ConnectionString("mongodb://" + host + ":" + port))
                            .credential(mongoCredential)
                            .build()
            );

            try {
                mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
            } catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
                return;
            }

            mongoDatabase = mongoClient.getDatabase(authDatabase);
            completableFuture.complete(null);
        });

        return completableFuture;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
