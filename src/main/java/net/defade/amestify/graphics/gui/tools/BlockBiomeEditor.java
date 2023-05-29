package net.defade.amestify.graphics.gui.tools;

import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.graphics.gui.window.BiomeSelectorUI;
import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.world.viewer.MapViewerRegion;
import org.joml.Vector2i;

public class BlockBiomeEditor extends Tool {
    private final Viewer viewer;

    private Vector2i previousBiomePosition = null;

    public BlockBiomeEditor(Viewer viewer) {
        super("Block Biome Editor", Assets.BLOCK_BIOME_EDITOR_ICON);
        this.viewer = viewer;
    }

    public void updateClick(boolean isButtonPressed) {
        if (isButtonPressed) {
            Vector2i currentBiomePosition = new Vector2i(viewer.getHoveredBlock().x >> 2, viewer.getHoveredBlock().y >> 2);

            if (!currentBiomePosition.equals(previousBiomePosition)) {
                if (previousBiomePosition == null) {
                    previousBiomePosition = currentBiomePosition;
                }

                int startX = previousBiomePosition.x;
                int startY = previousBiomePosition.y;
                int endX = currentBiomePosition.x;
                int endY = currentBiomePosition.y;

                int deltaX = Math.abs(endX - startX);
                int deltaY = Math.abs(endY - startY);

                int stepX = startX < endX ? 1 : -1;
                int stepY = startY < endY ? 1 : -1;

                int error = deltaX - deltaY;
                int errorIncrement;

                while (true) {
                    MapViewerRegion mapViewerRegion = viewer.getMapViewerWorld().getRegion(startX >> 7, startY >> 7);
                    if (mapViewerRegion != null) {
                        mapViewerRegion.setBiome(startX * 4, startY * 4, viewer.getUIComponent(BiomeSelectorUI.class).getSelectedBiome());
                    }

                    if (startX == endX && startY == endY) break;

                    errorIncrement = 2 * error;
                    if (errorIncrement > -deltaY) {
                        error -= deltaY;
                        startX += stepX;
                    }

                    if (errorIncrement < deltaX) {
                        error += deltaX;
                        startY += stepY;
                    }
                }

                for (int x = startX >> 7; x <= endX >> 7; x++) {
                    for (int y = startY >> 7; y <= endY >> 7; y++) {
                        MapViewerRegion mapViewerRegion = viewer.getMapViewerWorld().getRegion(x, y);
                        if (mapViewerRegion != null) {
                            mapViewerRegion.getRenderer().updateMesh();
                        }
                    }
                }

                previousBiomePosition = currentBiomePosition;
            }
        } else {
            previousBiomePosition = null;
        }
    }

    @Override
    public void renderShapes(ShapeRenderer shapeRenderer) {
        shapeRenderer.addSquare(viewer.getHoveredBlock().x & 0xFFFFFFFC, viewer.getHoveredBlock().y & 0xFFFFFFFC,
                viewer.getHoveredBlock().x + 4 & 0xFFFFFFFC, viewer.getHoveredBlock().y + 4 & 0xFFFFFFFC, 0xAA282828);
    }
}
