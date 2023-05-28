package net.defade.amestify.graphics.gui.dialog;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.loaders.amethyst.AmethystMapLoader;
import org.bson.BsonValue;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DatabaseLoaderDialog extends Dialog {
    private final Viewer viewer;

    private final ProgressTracker progressTracker = new ProgressTracker();
    private Path tempPath;
    private CompletableFuture<?> future;

    public DatabaseLoaderDialog(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        if(future == null) {
            disable();
            return;
        }

        ImGui.setNextWindowSize(600, 100);
        ImGui.begin("Loading amethyst file from database", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);
        if(future.isDone()) {
            if(future.isCompletedExceptionally()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
                ImGui.textWrapped(future.handle((unused, throwable) -> throwable.getMessage()).join());
                ImGui.popStyleColor();

                Utils.imGuiAlignNextItem("Close", 1);
                if(ImGui.button("Close")) {
                    disable();
                }
            }

            ImGui.end();
            return;
        }

        Utils.imGuiProgressBar(progressTracker);
        ImGui.end();
    }

    public void open(BsonValue fileId) {
        enable();
        future = new CompletableFuture<>();
        viewer.setMapViewerWorld(null);

        download(fileId);
        future.whenComplete((unused, throwable) -> {
            if (throwable == null) load();
        });
    }

    private void download(BsonValue fileId) {
        MongoConnector.THREAD_POOL.execute(() -> {
            try {
                tempPath = Utils.createAmethystTempFile(fileId.asString().toString() + ".amethyst");
            } catch (IOException exception) {
                future.completeExceptionally(exception);
                return;
            }

            GridFSBucket bucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase(), "maps");

            try {
                GridFSDownloadStream downloadStream = bucket.openDownloadStream(fileId);
                OutputStream fileOutputStream = Files.newOutputStream(tempPath);
                long fileSize = downloadStream.getGridFSFile().getLength();
                progressTracker.reset(fileSize);

                byte[] buffer = new byte[128 * 1024];
                long bytesRead = 0;
                int currentBytes;
                while ((currentBytes = downloadStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, currentBytes);
                    bytesRead += currentBytes;
                    progressTracker.increment(currentBytes, "Downloaded " + Utils.convertFileSizeUnit(bytesRead) + " / " +
                            Utils.convertFileSizeUnit(fileSize) + " (" + progressTracker.getProgress() * 100 + "%)");
                }

                future.complete(null);
                downloadStream.close();
                fileOutputStream.close();
            } catch (IOException exception) {
                future.completeExceptionally(exception);
            }
        });
    }

    private void load() {
        future = RegionFile.convert(progressTracker, new AmethystMapLoader(viewer, tempPath)).whenComplete((mapViewerWorld, throwable) -> {
            if(throwable != null) {
                throwable.printStackTrace();
            } else {
                viewer.setMapViewerWorld(mapViewerWorld);
                disable();
            }
        });
    }

    @Override
    protected void reset() {
        future = null;
        progressTracker.reset(0);
        tempPath = null;
    }
}
