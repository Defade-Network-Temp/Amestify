package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import net.defade.amestify.utils.NamespaceID;
import net.defade.amestify.world.World;
import net.defade.amestify.world.biome.Biome;

import java.util.Comparator;

public class BiomeSelectorWindow {
    private World world = null;
    private Biome selectedBiome = null;

    public void render() {
        ImGui.begin("Biome Picker");

        renderBiomeList();

        renderDeleteBiome();

        ImGui.end();
    }

    public void setWorld(World world) {
        this.world = world;
        this.selectedBiome = world.getPlainsBiome();
    }

    private void renderBiomeList() {
        boolean shouldEndList = ImGui.beginListBox("##Biomes", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY() - 46);

        ImGui.pushStyleColor(ImGuiCol.Text, 255, 204, 0, 255); // Yellow
        for (String createdBiome : getCreatedBiomes()) {
            if (ImGui.selectable(createdBiome, createdBiome.equals(selectedBiome.name().asString()))) {
                selectedBiome = world.getBiomeByName(NamespaceID.from(createdBiome));
            }
        }
        ImGui.popStyleColor();

        ImGui.separator();

        for (String vanillaBiome : getVanillaBiomes()) {
            if (ImGui.selectable(vanillaBiome, selectedBiome.name().asString().equals(vanillaBiome))) {
                selectedBiome = world.getBiomeByName(NamespaceID.from(vanillaBiome));
            }
        }

        if(shouldEndList) ImGui.endListBox();

        if(selectedBiome != null) ImGui.text("Selected Biome: " + selectedBiome.name().asString());
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

    private String[] getVanillaBiomes() {
        if(world == null) return new String[0];
        return world.unmodifiableBiomeCollection().stream()
                .filter(biome -> biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .map(biome -> biome.name().asString())
                .toArray(String[]::new);
    }

    private String[] getCreatedBiomes() {
        if(world == null) return new String[0];
        return world.unmodifiableBiomeCollection().stream()
                .filter(biome -> !biome.name().asString().contains("minecraft:"))
                .sorted(Comparator.comparing(biome -> biome.name().asString()))
                .map(biome -> biome.name().asString())
                .toArray(String[]::new);
    }
}
