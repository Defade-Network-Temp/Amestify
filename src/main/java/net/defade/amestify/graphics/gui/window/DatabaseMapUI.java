package net.defade.amestify.graphics.gui.window;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImString;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.graphics.gui.dialog.DatabaseLoaderDialog;
import net.defade.amestify.graphics.rendering.Assets;
import org.bson.BsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class DatabaseMapUI extends UIComponent {
    private final Viewer viewer;

    private boolean wasConnectedToDatabase = false;
    private Map<String, List<GameMap>> savedMaps = new HashMap<>();

    private GameMap modifyingMap = null;
    private final ImString mapName = new ImString();

    private GameMap mapAwaitingDeleteConfirm = null;
    private boolean deletingMap = false;

    public DatabaseMapUI(Viewer viewer) {
        this.viewer = viewer;
        Timer updateDatabaseTimer = new Timer("Update Database Timer", true);
        updateDatabaseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetchSavedMaps();
            }
        }, 0, 5000);
    }

    @Override
    public void render() {
        checkForDBUpdates();

        ImGui.begin("Saved Maps");
        if(!viewer.getMongoConnector().isConnected()) {
            ImGui.text("Not connected to database.");
            ImGui.end();
            return;
        }

        for(Map.Entry<String, List<GameMap>> maps : savedMaps.entrySet()) {
            if(ImGui.treeNode(maps.getKey())) {
                for(GameMap map : maps.getValue()) {
                    int imageSize = (int) ImGui.calcTextSize(map.name).y;

                    if(map.equals(modifyingMap)) {
                        ImGui.inputText("##Map Name", mapName);
                        ImGui.sameLine();
                        if(ImGui.imageButton(Assets.DONE_ICON.getTextureId(), imageSize, imageSize)) {
                            renameMap(map, mapName.get() + ".amethyst");
                            modifyingMap = null;
                        }
                    } else if (deletingMap) {
                        ImGui.text("     " + map.name);
                        ImGui.sameLine();

                        ImGui.pushStyleColor(ImGuiCol.Text, 215, 45, 45, 255);
                        ImGui.text("Deleting...");
                        ImGui.popStyleColor();
                    } else if(map.equals(mapAwaitingDeleteConfirm)) {
                        ImGui.text("     " + map.name);
                        ImGui.sameLine();

                        ImGui.pushStyleColor(ImGuiCol.Button, 15, 150, 15, 255);
                        if(ImGui.button("Delete")) {
                            deletingMap = true;
                            deleteMap();
                        }
                        ImGui.popStyleColor();

                        ImGui.sameLine();

                        ImGui.pushStyleColor(ImGuiCol.Button, 215, 45, 45, 255);
                        if(ImGui.button("Cancel")) {
                            mapAwaitingDeleteConfirm = null;
                        }
                        ImGui.popStyleColor();
                    } else {
                        ImGui.text("     " + map.name);
                        ImGui.sameLine();

                        // ImGUI does a very dumb thing where it takes the texture ID as the stack ID.
                        // So if you have twice the same image on the stack, only the first one will work.
                        // https://github.com/ocornut/imgui/issues/4471
                        ImGui.pushID(map.name + " edit");
                        if(ImGui.imageButton(Assets.EDIT_ICON.getTextureId(), imageSize, imageSize)) {
                            modifyingMap = map;
                            mapName.set(map.name);
                        }
                        ImGui.popID();

                        ImGui.sameLine();
                        ImGui.pushID(map.name + " delete");
                        if(ImGui.imageButton(Assets.DELETE_ICON.getTextureId(), imageSize, imageSize)) {
                            mapAwaitingDeleteConfirm = map;
                        }
                        ImGui.popID();

                        ImGui.sameLine();
                        ImGui.pushID(map.name + " open");
                        if(ImGui.imageButton(Assets.OPEN_ICON.getTextureId(), imageSize, imageSize)) {
                            viewer.getDialog(DatabaseLoaderDialog.class).open(map.id);
                        }
                        ImGui.popID();
                    }
                }
                ImGui.treePop();
            }
        }

        ImGui.end();
    }

    @Override
    public boolean isDisabledWhenNoMapLoaded() {
        return false;
    }

    private void checkForDBUpdates() {
        if(!wasConnectedToDatabase && viewer.getMongoConnector().isConnected()) {
            fetchSavedMaps();
        } else if(wasConnectedToDatabase && !viewer.getMongoConnector().isConnected()) {
            savedMaps.clear();
        }

        wasConnectedToDatabase = viewer.getMongoConnector().isConnected();
    }

    private void renameMap(GameMap map, String newName) {
        MongoConnector.THREAD_POOL.execute(() -> {
            GridFSBucket bucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase(), "maps");
            bucket.rename(map.id, newName);

            fetchSavedMaps();
        });
    }

    private void deleteMap() {
        MongoConnector.THREAD_POOL.execute(() -> {
            GridFSBucket bucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase(), "maps");
            bucket.delete(mapAwaitingDeleteConfirm.id);

            fetchSavedMaps();
            deletingMap = false;
            mapAwaitingDeleteConfirm = null;
        });
    }

    private void fetchSavedMaps() {
        if(!viewer.getMongoConnector().isConnected()) return;

        MongoConnector.THREAD_POOL.execute(() -> {
            Map<String, List<GameMap>> savedMaps = new HashMap<>();

            GridFSBucket bucket = GridFSBuckets.create(viewer.getMongoConnector().getMongoDatabase(), "maps");
            bucket.find().forEach((Consumer<? super GridFSFile>) document -> {
                String game = document.getMetadata() != null ?document.getMetadata().getString("game") : "Unknown";
                String name = document.getFilename();
                if(name.endsWith(".amethyst")) name = name.substring(0, name.length() - 9);

                GameMap map = new GameMap(document.getId(), name, game);
                if(!savedMaps.containsKey(game)) {
                    List<GameMap> maps = new ArrayList<>();
                    maps.add(map);
                    savedMaps.put(game, maps);
                } else {
                    savedMaps.get(game).add(map);
                }
            });

            if(modifyingMap != null) {
                boolean found = false;
                for (Map.Entry<String, List<GameMap>> maps : savedMaps.entrySet()) {
                    if (modifyingMap.game.equals(maps.getKey()) && maps.getValue().contains(modifyingMap)) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    modifyingMap = null;
                }
            }

            this.savedMaps = savedMaps;
        });
    }

    private record GameMap(BsonValue id, String name, String game) {}
}
