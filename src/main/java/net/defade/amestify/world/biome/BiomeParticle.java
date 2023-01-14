package net.defade.amestify.world.biome;

import net.defade.amestify.utils.NamespaceID;

public record BiomeParticle(float probability, Option option) {
    public interface Option { }

    public record BlockOption(int blockStateId) implements Option {
        private static final String type = "block";
    }

    public record DustOption(float red, float green, float blue, float scale) implements Option {
        private static final String type = "dust";
    }

    public record NormalOption(NamespaceID type) implements Option { }
}
