package net.defade.amestify.graphics.gui.dialog;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Filters;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.MongoFileUploader;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.savers.AmethystSaver;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AmethystSaveDatabaseGUI extends Dialog {
    private final Viewer viewer;
    private final AmethystSaver amethystSaver = new AmethystSaver();
    private final MongoFileUploader mongoFileUploader;

    private boolean isModifyingConfig = false;

    private final ImString worldName = new ImString("", 255);
    private final ImString game = new ImString("", 255);

    private boolean confirmationPopup = false;
    private CompletableFuture<Boolean> fileExists = null;

    private final ProgressTracker progressTracker = new ProgressTracker();
    private CompletableFuture<Path> saveFuture;
    private CompletableFuture<Void> uploadFuture;

    public AmethystSaveDatabaseGUI(Viewer viewer) {
        this.viewer = viewer;
        this.mongoFileUploader = new MongoFileUploader(viewer);
    }

    @Override
    public void render() {
        if((saveFuture == null || saveFuture.isCompletedExceptionally()) && (uploadFuture == null || uploadFuture.isCompletedExceptionally())) {
            renderSaveWorldDialog();
        } else if((saveFuture != null && !saveFuture.isDone()) || (uploadFuture != null && !uploadFuture.isDone())) {
            renderSavingDialog();
        } else {
            renderSaveCompleteDialog();
        }
    }

    private void renderSaveWorldDialog() {
        String errorMessage = null;
        if((saveFuture != null && saveFuture.isCompletedExceptionally()) || (uploadFuture != null && uploadFuture.isCompletedExceptionally())) {
            errorMessage = ((saveFuture != null && saveFuture.isCompletedExceptionally()) ? saveFuture : uploadFuture)
                    .handle((path, throwable) -> throwable.getMessage()).join();
        }
        ImGui.setNextWindowSize(errorMessage == null ? 325 : 370, 100);
        ImGui.begin("Save world to database", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        if(confirmationPopup) {
            if(fileExists.isDone()) {
                if (fileExists.join()) {
                    ImGui.textColored(255, 0, 0, 255, "A world with that name already exists!");
                    if (ImGui.button("Overwrite")) {
                        confirmationPopup = false;
                        save();
                    }
                } else {
                    save();
                }
            } else {
                String[] dots = new String[]{"", ".", "..", "..."};
                ImGui.textColored(255, 0, 0, 255, "Checking if the world exists" + dots[(int) ((System.currentTimeMillis() / 180) % 4)]);

            }
            ImGui.end();
            return;
        }

        if(errorMessage != null) {
            ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
            ImGui.textWrapped(errorMessage);
            ImGui.popStyleColor();
        }

        ImGui.inputTextWithHint("##World name", "World name", worldName);
        ImGui.sameLine();
        if(ImGui.button("Modify config")) isModifyingConfig = true;

        ImGui.inputTextWithHint("##Game", "Game", game);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, 15, 150, 15, 255);
        if(ImGui.button("Save")) {
            confirmationPopup = true;
            fileExists = checkIfFilesExists();
        }
        ImGui.sameLine();
        Utils.imGuiAlignNextItem("Cancel", 1);
        ImGui.pushStyleColor(ImGuiCol.Button, 215, 45, 45, 255);
        if(ImGui.button("Cancel")) disable();
        ImGui.popStyleColor();

        ImGui.popStyleColor();
        ImGui.end();

        if(isModifyingConfig) {
            ImGui.begin("Config", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking);
            if (ImGui.button("Close", 80, 0)) isModifyingConfig = false;
            ImGui.inputTextMultiline("##Config", viewer.getWorldConfig(), -1, -1, ImGuiInputTextFlags.AllowTabInput | ImGuiInputTextFlags.AutoSelectAll | ImGuiInputTextFlags.EnterReturnsTrue);
            ImGui.end();
        }
    }

    private void renderSavingDialog() {
        ImGui.setNextWindowSize(600, 100);
        ImGui.begin(saveFuture.isDone() ? "Uploading file..." : "Converting file...", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        Utils.imGuiProgressBar(progressTracker);
        ImGui.end();
    }

    private void renderSaveCompleteDialog() {
        ImGui.setNextWindowSize(150, 70);
        ImGui.begin("Complete!", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        Utils.imGuiAlignNextItem("Close", 0);
        if(ImGui.button("Close")) {
            saveFuture = null;
            uploadFuture = null;
            disable();
        }

        ImGui.end();
    }

    private void save() {
        confirmationPopup = false;
        fileExists = null;
        saveFuture = amethystSaver.saveToTempFile(worldName.get(), viewer.getWorldConfig().get(), viewer.getMapViewerWorld(), progressTracker);
        saveFuture.thenAccept(path -> {
            uploadFuture = mongoFileUploader.uploadFile(path, worldName.get() + ".amethyst", worldName.get(), game.get(), progressTracker);
        });
    }

    private CompletableFuture<Boolean> checkIfFilesExists() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        MongoConnector.THREAD_POOL.execute(() -> {
            GridFSBucket gridFSBucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase());
            completableFuture.complete(gridFSBucket.find(Filters.eq("_id", worldName.get())).first() != null);
        });

        return completableFuture;
    }
}
