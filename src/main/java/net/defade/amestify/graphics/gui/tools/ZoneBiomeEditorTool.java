package net.defade.amestify.graphics.gui.tools;

import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.graphics.gui.window.BiomeSelectorUI;
import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.viewer.MapViewerRegion;
import org.joml.Vector2i;

public class ZoneBiomeEditorTool extends Tool {
    private final Viewer viewer;
    private Vector2i selectedRegionOrigin = null;

    public ZoneBiomeEditorTool(Viewer viewer) {
        super("Zone Biome Editor", Assets.GRID_SELECTION_ICON);
        this.viewer = viewer;
    }

    @Override
    public void updateClick(boolean pressed) {
        if(pressed) {
            if(selectedRegionOrigin == null) {
                selectedRegionOrigin = new Vector2i(viewer.getHoveredBlock());
            }
        } else if(selectedRegionOrigin != null) {
            Biome selectedBiome = viewer.getUIComponent(BiomeSelectorUI.class).getSelectedBiome();
            int minX = Math.min(selectedRegionOrigin.x, viewer.getHoveredBlock().x);
            int maxX = Math.max(selectedRegionOrigin.x, viewer.getHoveredBlock().x);
            int minZ = Math.min(selectedRegionOrigin.y, viewer.getHoveredBlock().y);
            int maxZ = Math.max(selectedRegionOrigin.y, viewer.getHoveredBlock().y);

            int minRegionX = (int) Math.floor((double) minX / 512);
            int maxRegionX = (int) Math.floor((double) maxX / 512);
            int minRegionZ = (int) Math.floor((double) minZ / 512);
            int maxRegionZ = (int) Math.floor((double) maxZ / 512);
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    MapViewerRegion mapViewerRegion = viewer.getMapViewerWorld().getRegion(regionX, regionZ);
                    if(mapViewerRegion == null) continue;

                    int minRegionBlockX = Math.max(minX, regionX * 512);
                    int maxRegionBlockX = Math.min(maxX, regionX * 512 + 511);
                    int minRegionBlockZ = Math.max(minZ, regionZ * 512);
                    int maxRegionBlockZ = Math.min(maxZ, regionZ * 512 + 511);

                    for (int x = minRegionBlockX; x <= maxRegionBlockX; x += 4) {
                        for (int z = minRegionBlockZ; z <= maxRegionBlockZ; z += 4) {
                            mapViewerRegion.setBiome(x, z, selectedBiome);
                        }
                    }

                    mapViewerRegion.getRenderer().updateMesh();
                }
            }

            selectedRegionOrigin = null;
        }
    }

    @Override
    public void renderShapes(ShapeRenderer shapeRenderer) {
        if(selectedRegionOrigin != null) {
            shapeRenderer.addSquare(selectedRegionOrigin.x, selectedRegionOrigin.y, viewer.getHoveredBlock().x + 1, viewer.getHoveredBlock().y, 0x99C46200);
        }
    }
}
