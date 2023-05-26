package net.defade.amestify.database;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MongoConnector {
    public static final Executor THREAD_POOL = Executors.newFixedThreadPool(2);

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private boolean isConnected = false;

    public CompletableFuture<Void> connect(String host, int port, String username, char[] password, String authDatabase) {
        if(isConnected) disconnect();

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        THREAD_POOL.execute(() -> {
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
            isConnected = true;
            completableFuture.complete(null);
        });

        return completableFuture;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public void disconnect() {
        isConnected = false;
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
