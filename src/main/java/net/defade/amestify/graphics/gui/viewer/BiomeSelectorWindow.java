package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiColorEditFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.World;
import net.defade.amestify.world.biome.Biome;
import java.util.Comparator;

public class BiomeSelectorWindow {
    private World world = null;
    private Biome selectedBiome = null;
    private final ImBoolean showBiomeLayer = new ImBoolean(false);
    private final float[] biomeMapViewerColor = new float[3];

    public void render() {
        ImGui.begin("Biome Picker");

        renderBiomeList();
        renderDeleteBiome();
        ImGui.checkbox("Show Biome Layer", showBiomeLayer);

        ImGui.end();
    }

    public void setWorld(World world) {
        this.world = world;
        this.selectedBiome = world.getPlainsBiome();
    }

    public boolean shouldShowBiomeLayer() {
        return showBiomeLayer.get();
    }

    public Biome getSelectedBiome() {
        return selectedBiome;
    }

    public void reset() {
        world = null;
        selectedBiome = null;
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
                    world.getBiomeColorLayer().setBiomeColor(biome, Utils.floatToRGB(biomeMapViewerColor));
                    ImGui.closeCurrentPopup();
                }

                ImGui.endPopup();
            }

            ImGui.sameLine();
            ImGui.colorButton("##" + biome, Utils.rgbToFloat(world.getBiomeColorLayer().getColor(biome)), 0, 16, 16);
        }
    }

    private void renderDeleteBiome() {
        boolean canDeleteBiome = selectedBiome != null && selectedBiome != world.getPlainsBiome();
        if(!canDeleteBiome) ImGui.beginDisabled();
        ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);

        if(ImGui.button("Delete Biome")) {
            world.unregisterBiome(selectedBiome);
            selectedBiome = world.getPlainsBiome();
        }

        ImGui.popStyleColor();
        if(!canDeleteBiome) ImGui.endDisabled();
    }

    private Biome[] getVanillaBiomes() {
        if(world == null) return new Biome[0];
        return world.unmodifiableBiomeCollection().stream()
                .filter(biome -> biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .toArray(Biome[]::new);
    }

    private Biome[] getCreatedBiomes() {
        if(world == null) return new Biome[0];
        return world.unmodifiableBiomeCollection().stream()
                .filter(biome -> !biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .toArray(Biome[]::new);
    }
}
