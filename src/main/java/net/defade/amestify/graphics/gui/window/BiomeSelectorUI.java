package net.defade.amestify.graphics.gui.window;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiColorEditFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.biome.Biome;
import java.util.Comparator;

public class BiomeSelectorUI extends UIComponent {
    private final Viewer viewer;

    private Biome selectedBiome = null;
    private final ImBoolean showBiomeLayer = new ImBoolean(false);
    private final float[] biomeMapViewerColor = new float[3];

    public BiomeSelectorUI(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        if(viewer.getMapViewerWorld() == null) {
            selectedBiome = null;
        } else if(selectedBiome == null) {
            selectedBiome = viewer.getMapViewerWorld().getPlainsBiome();
        }

        ImGui.begin("Biome Picker");

        renderBiomeList();
        renderDeleteBiome();
        ImGui.checkbox("Show Biome Layer", showBiomeLayer);

        ImGui.end();
    }

    public boolean shouldShowBiomeLayer() {
        return showBiomeLayer.get();
    }

    public Biome getSelectedBiome() {
        return selectedBiome;
    }

    private void renderBiomeList() {
        boolean shouldEndList = ImGui.beginListBox("##Biomes", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY() - 46);

        ImGui.pushStyleColor(ImGuiCol.Text, 255, 204, 0, 255); // Yellow
        renderListBiome(getCreatedBiomes());
        ImGui.popStyleColor();

        ImGui.separator();

        renderListBiome(getVanillaBiomes());

        if(shouldEndList) ImGui.endListBox();

        if(selectedBiome != null) ImGui.text("Selected Biome: " + selectedBiome.name().asString());
    }

    private void renderListBiome(Biome[] biomes) {
        for(Biome biome : biomes) {
            if (ImGui.selectable(biome.name().asString(), selectedBiome.name().asString().equals(biome.name().asString()))) {
                selectedBiome = biome;
            }

            if (ImGui.beginPopupContextItem(1)) {
                ImGui.colorPicker3("#Biome Map Color Viewer Picker", biomeMapViewerColor, ImGuiColorEditFlags.NoLabel | ImGuiColorEditFlags.NoAlpha);

                if (ImGui.button("Apply")) {
                    viewer.getMapViewerWorld().getBiomeColorLayer().setBiomeColor(biome, Utils.floatToRGB(biomeMapViewerColor));
                    ImGui.closeCurrentPopup();
                }

                ImGui.endPopup();
            }

            ImGui.sameLine();
            ImGui.colorButton("##" + biome, Utils.rgbToFloat(viewer.getMapViewerWorld().getBiomeColorLayer().getColor(biome)), 0, 16, 16);
        }
    }

    private void renderDeleteBiome() {
        boolean canDeleteBiome = selectedBiome != null && selectedBiome != viewer.getMapViewerWorld().getPlainsBiome();
        if(!canDeleteBiome) ImGui.beginDisabled();
        ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);

        if(ImGui.button("Delete Biome")) {
            viewer.getMapViewerWorld().unregisterBiome(selectedBiome);
            selectedBiome = viewer.getMapViewerWorld().getPlainsBiome();
        }

        ImGui.popStyleColor();
        if(!canDeleteBiome) ImGui.endDisabled();
    }

    private Biome[] getVanillaBiomes() {
        if(viewer.getMapViewerWorld() == null) return new Biome[0];
        return viewer.getMapViewerWorld().unmodifiableBiomeCollection().stream()
                .filter(biome -> biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .toArray(Biome[]::new);
    }

    private Biome[] getCreatedBiomes() {
        if(viewer.getMapViewerWorld() == null) return new Biome[0];
        return viewer.getMapViewerWorld().unmodifiableBiomeCollection().stream()
                .filter(biome -> !biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .toArray(Biome[]::new);
    }
}
