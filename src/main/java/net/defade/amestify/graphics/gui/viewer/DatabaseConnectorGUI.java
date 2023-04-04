package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.defade.amestify.database.MongoConnector;
import java.util.concurrent.CompletableFuture;

public class DatabaseConnectorGUI {
    private final ImString host = new ImString("localhost", 253); // A DNS name can be 253 characters long max
    private final ImString port = new ImString("27017", 5); // A port can be 5 characters long max
    private final ImString username = new ImString();
    private final ImString password = new ImString();
    private final ImString database = new ImString();

    private final MongoConnector mongoConnector = new MongoConnector();
    private CompletableFuture<Void> connectFuture = null;
    private Throwable throwable = null;

    public void renderImGui() {
        ImGui.setNextWindowSize(400, 200);
        ImGui.begin("Connect to database", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        ImGui.inputTextWithHint("##Host", "Host", host);
        ImGui.inputTextWithHint("##Port", "Port", port);
        ImGui.inputTextWithHint("##Username", "Username", username);
        ImGui.inputTextWithHint("##Password", "Password", password, ImGuiInputTextFlags.Password);
        ImGui.inputTextWithHint("##Database", "Database", database);

        if((connectFuture == null || connectFuture.isDone()) && ImGui.button("Connect")) {
            connectFuture = mongoConnector.connect(host.get(), Integer.parseInt(port.get()), username.get(), password.get().toCharArray(), database.get());
            connectFuture.exceptionally(throwable -> {
                this.throwable = throwable;
                return null;
            });
        }

        if(connectFuture != null) {
            if(!connectFuture.isDone()) {
                ImGui.text("Connecting...");
            } else if(throwable != null) {
                ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
                ImGui.textWrapped("Error: " + throwable.getMessage());
                ImGui.popStyleColor();
            }
        }

        ImGui.end();
    }

    public boolean isConnected() {
        return connectFuture != null && connectFuture.isDone() && !connectFuture.isCompletedExceptionally();
    }
}
