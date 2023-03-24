package net.defade.amestify.graphics.texture.block;

/**
 * @param isTranslucent true if texture has transparent pixels
 * @param isIgnored     true if the block shouldn't be rendered
 */
public record BlockTexture(int textureLayer, boolean isGrass, boolean isFoliage, boolean isWater, boolean isTranslucent,
                           boolean isIgnored) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockTexture that = (BlockTexture) o;
        return textureLayer == that.textureLayer;
    }

    @Override
    public int hashCode() {
        return textureLayer;
    }
}
