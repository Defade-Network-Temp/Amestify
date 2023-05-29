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

            int minRegionX = minX >> 9;
            int maxRegionX = maxX >> 9;
            int minRegionZ = minZ >> 9;
            int maxRegionZ = maxZ >> 9;
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    MapViewerRegion mapViewerRegion = viewer.getMapViewerWorld().getRegion(regionX, regionZ);
                    if(mapViewerRegion == null) continue;

                    int minRegionBlockX = Math.max(minX, regionX * 512);
                    int maxRegionBlockX = Math.min(maxX, regionX * 512 + 511);
                    int minRegionBlockZ = Math.max(minZ, regionZ * 512);
                    int maxRegionBlockZ = Math.min(maxZ, regionZ * 512 + 511);

                    // Don't use block coordinates where you update the values by 4,
                    // because if the start point at x is 3 and the end point is 6,
                    // x+=4 will be 7, thus the loop won't run.
                    for (int x = minRegionBlockX >> 2; x <= maxRegionBlockX >> 2; x++) {
                        for (int z = minRegionBlockZ >> 2; z <= maxRegionBlockZ >> 2; z++) {
                            mapViewerRegion.setBiome(x << 2, z << 2, selectedBiome);
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
            int startX = Math.min(selectedRegionOrigin.x, viewer.getHoveredBlock().x) & 0xFFFFFFFC;
            int startY = Math.min(selectedRegionOrigin.y, viewer.getHoveredBlock().y) & 0xFFFFFFFC;
            int endX = Math.max(selectedRegionOrigin.x, viewer.getHoveredBlock().x) + 4 & 0xFFFFFFFC;
            int endY = Math.max(selectedRegionOrigin.y, viewer.getHoveredBlock().y) + 4 & 0xFFFFFFFC;
            shapeRenderer.addSquare(startX, startY, endX, endY, 0x99C46200);
        } else {
            shapeRenderer.addSquare(viewer.getHoveredBlock().x & 0xFFFFFFFC, viewer.getHoveredBlock().y & 0xFFFFFFFC,
                    viewer.getHoveredBlock().x + 4 & 0xFFFFFFFC, viewer.getHoveredBlock().y + 4 & 0xFFFFFFFC, 0xAA282828);
        }
    }
}
