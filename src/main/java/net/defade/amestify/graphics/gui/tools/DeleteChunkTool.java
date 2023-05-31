package net.defade.amestify.graphics.gui.tools;

import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.world.viewer.MapViewerRegion;

public class DeleteChunkTool extends Tool {
    private final Viewer viewer;

    private int lastModifiedChunkX = Integer.MAX_VALUE;
    private int lastModifiedChunkZ = Integer.MAX_VALUE;
    private boolean wasPressed = false;
    private boolean isDeleting = false;

    public DeleteChunkTool(Viewer viewer) {
        super("Delete Chunk", Assets.DELETE_CHUNK_ICON);
        this.viewer = viewer;
    }

    @Override
    public void updateClick(boolean pressed) {
        if (pressed) {
            int chunkX = viewer.getHoveredBlock().x >> 4;
            int chunkZ = viewer.getHoveredBlock().y >> 4;
            MapViewerRegion mapViewerRegion = viewer.getMapViewerWorld()
                    .getRegion(viewer.getHoveredBlock().x >> 9, viewer.getHoveredBlock().y >> 9);

            if (mapViewerRegion != null && (lastModifiedChunkX != chunkX || lastModifiedChunkZ != chunkZ)) {
                if(!wasPressed) {
                    wasPressed = true;
                    isDeleting = !mapViewerRegion.isChunkDeleted(chunkX, chunkZ);
                }

                mapViewerRegion.setChunkDeleted(chunkX, chunkZ, isDeleting);
                mapViewerRegion.getRenderer().updateMesh();
                lastModifiedChunkX = chunkX;
                lastModifiedChunkZ = chunkZ;
            }
        } else {
            lastModifiedChunkX = Integer.MAX_VALUE;
            lastModifiedChunkZ = Integer.MAX_VALUE;
            wasPressed = false;
        }
    }

    @Override
    public void renderShapes(ShapeRenderer shapeRenderer) {
        int chunkBlockX = viewer.getHoveredBlock().x & 0xFFFFFFF0;
        int chunkBlockZ = viewer.getHoveredBlock().y & 0xFFFFFFF0;

        shapeRenderer.addSquare(chunkBlockX, chunkBlockZ, chunkBlockX + 16, chunkBlockZ + 16, 0xAA282828);
    }
}
