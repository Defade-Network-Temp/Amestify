package net.defade.amestify.graphics.rendering.texture;

import net.defade.amestify.utils.Utils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class BiomeTexture {
    private static final int[] grassColorMap = new int[256 * 256];
    private static final int[] foliageColorMap = new int[256 * 256];

    public static void init() throws IOException {
        BufferedImage grassColorImage = ImageIO.read(Objects.requireNonNull(BiomeTexture.class.getClassLoader().getResourceAsStream("textures/colormap/grass.png")));
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                grassColorMap[x + (y << 8)] = grassColorImage.getRGB(x, y);
            }
        }

        BufferedImage foliageColorImage = ImageIO.read(Objects.requireNonNull(BiomeTexture.class.getClassLoader().getResourceAsStream("textures/colormap/foliage.png")));
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                foliageColorMap[x + (y << 8)] = foliageColorImage.getRGB(x, y);
            }
        }
    }

    public static int getGrassColor(int grassColorEffect, float biomeTemperature, float biomeDownfall) {
        int grassColor;

        if(grassColorEffect != -1) {
            grassColor = (int) (grassColorEffect & 0xFFFFFFFFL) | 0xFF000000;
        } else {
            grassColor = grassColor(biomeTemperature, biomeDownfall);
        }

        return grassColor;
    }

    public static int getFoliageColor(int foliageColorEffect, float biomeTemperature, float biomeDownfall) {
        int foliageColor;

        if(foliageColorEffect != -1) {
            foliageColor = (int) (foliageColorEffect & 0xFFFFFFFFL) | 0xFF000000;
        } else {
            foliageColor = foliageColor(biomeTemperature, biomeDownfall);
        }

        return foliageColor;
    }

    public static int getWaterColor(int waterColor) {
        return (int) (waterColor & 0xFFFFFFFFL) | 0xFF000000;
    }

    private static int grassColor(double temperature, double downfall) {
        temperature = Utils.clamp(0.0, 1.0, temperature);
        downfall = Utils.clamp(0.0, 1.0, downfall);

        downfall = downfall * temperature;
        int i = (int) ((1.0D - temperature) * 255.0D);
        int j = (int) ((1.0D - downfall) * 255.0D);
        int k = j << 8 | i;
        return k > grassColorMap.length ? -65281 : grassColorMap[k];
    }

    private static int foliageColor(double temperature, double downfall) {
        temperature = Utils.clamp(0.0, 1.0, temperature);
        downfall = Utils.clamp(0.0, 1.0, downfall);

        downfall = downfall * temperature;
        int i = (int) ((1.0D - temperature) * 255.0D);
        int j = (int) ((1.0D - downfall) * 255.0D);
        return foliageColorMap[j << 8 | i];
    }
}
