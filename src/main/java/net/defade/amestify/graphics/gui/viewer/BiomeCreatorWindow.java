package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.type.ImString;
import net.defade.amestify.utils.NamespaceID;
import net.defade.amestify.world.MapViewerWorld;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.biome.BiomeEffects;

import java.awt.Color;

public class BiomeCreatorWindow {
    private MapViewerWorld mapViewerWorld = null;

    private final float[] pickerColor = new float[3];
    private final ImString biomeName = new ImString();

    public void render() {
        ImGui.begin("Biome Creator");

        renderColorPicker();
        renderColorsResume();
        ImGui.separator();
        renderBiomeSave();

        ImGui.end();
    }

    public void setWorld(MapViewerWorld mapViewerWorld) {
        this.mapViewerWorld = mapViewerWorld;
    }

    public void reset() {
        mapViewerWorld = null;
    }

    private void renderBiomeSave() {
        ImGui.inputTextWithHint("##Biome Name", "Biome Name", biomeName);
        ImGui.sameLine();
        if(mapViewerWorld == null) ImGui.beginDisabled();
        if(ImGui.button("Save Biome")) {
            Biome newBiome = Biome.builder()
                    .name(NamespaceID.from(biomeName.get()))
                    .effects(BiomeEffects.builder()
                            .fogColor(new Color(Settings.FOG_COLOR.getColor()[0], Settings.FOG_COLOR.getColor()[1], Settings.FOG_COLOR.getColor()[2]).getRGB())
                            .skyColor(new Color(Settings.SKY_COLOR.getColor()[0], Settings.SKY_COLOR.getColor()[1], Settings.SKY_COLOR.getColor()[2]).getRGB())
                            .waterColor(new Color(Settings.WATER_COLOR.getColor()[0], Settings.WATER_COLOR.getColor()[1], Settings.WATER_COLOR.getColor()[2]).getRGB())
                            .waterFogColor(new Color(Settings.WATER_FOG_COLOR.getColor()[0], Settings.WATER_FOG_COLOR.getColor()[1], Settings.WATER_FOG_COLOR.getColor()[2]).getRGB())
                            .foliageColor(new Color(Settings.FOLIAGE_COLOR.getColor()[0], Settings.FOLIAGE_COLOR.getColor()[1], Settings.FOLIAGE_COLOR.getColor()[2]).getRGB())
                            .grassColor(new Color(Settings.GRASS_COLOR.getColor()[0], Settings.GRASS_COLOR.getColor()[1], Settings.GRASS_COLOR.getColor()[2]).getRGB())
                            .build())
                    .build();
            mapViewerWorld.registerBiome(newBiome);
        }
        if(mapViewerWorld == null) ImGui.endDisabled();
    }

    private void renderColorPicker() {
        ImGui.beginChild("##Color", 0, 455, true);

        ImGui.setNextItemWidth(250);
        ImGui.colorPicker3("##Color Picker", pickerColor, ImGuiColorEditFlags.DisplayHex | ImGuiColorEditFlags.DisplayRGB |
                ImGuiColorEditFlags.NoSmallPreview | ImGuiColorEditFlags.NoLabel);

        for (Settings setting : Settings.values()) {
            if(ImGui.button("Apply to " + setting.getName().toLowerCase())) {
                System.arraycopy(pickerColor, 0, setting.getColor(), 0, 3);
            }
        }
        ImGui.endChild();
    }

    private void renderColorsResume() {
        for (Settings setting : Settings.values()) {
            ImGui.text(setting.getName() + ": ");
            ImGui.sameLine();
            ImGui.colorButton(setting.getName(), setting.getColor());
            ImGui.sameLine();
            ImGui.text(
                    String.format("#%06x",
                            new Color(setting.getColor()[0], setting.getColor()[1], setting.getColor()[2]).getRGB() & 0x00FFFFFF
                    ).toUpperCase()
            );
        }
    }

    private enum Settings {
        FOG_COLOR("Fog Color"),
        SKY_COLOR("Sky Color"),
        WATER_COLOR("Water Color"),
        WATER_FOG_COLOR("Water Fog Color"),
        FOLIAGE_COLOR("Foliage Color"),
        GRASS_COLOR("Grass Color");

        private final String name;
        private final float[] color = new float[3];

        Settings(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public float[] getColor() {
            return color;
        }
    }
}
