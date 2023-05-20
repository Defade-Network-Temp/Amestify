package net.defade.amestify.world.biome;

import net.defade.amestify.graphics.rendering.texture.BiomeTexture;
import net.defade.amestify.utils.NamespaceID;
import java.util.concurrent.atomic.AtomicInteger;

public final class Biome {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private static final BiomeEffects DEFAULT_EFFECTS = BiomeEffects.builder()
            .fogColor(0xC0D8FF)
            .skyColor(0x78A7FF)
            .waterColor(0x3F76E4)
            .waterFogColor(0x50533)
            .build();

    private final int id = ID_COUNTER.getAndIncrement();

    private final NamespaceID name;
    private final float temperature;
    private final float downfall;
    private final BiomeEffects effects;
    private final Precipitation precipitation;
    private final TemperatureModifier temperatureModifier;

    Biome(NamespaceID name, float temperature, float downfall, BiomeEffects effects, Precipitation precipitation, TemperatureModifier temperatureModifier) {
        this.name = name;
        this.temperature = temperature;
        this.downfall = downfall;
        this.effects = effects;
        this.precipitation = precipitation;
        this.temperatureModifier = temperatureModifier;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int id() {
        return this.id;
    }

    public NamespaceID name() {
        return this.name;
    }

    public float temperature() {
        return this.temperature;
    }

    public float downfall() {
        return this.downfall;
    }

    public BiomeEffects effects() {
        return this.effects;
    }

    public Precipitation precipitation() {
        return this.precipitation;
    }

    public TemperatureModifier temperatureModifier() {
        return this.temperatureModifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Biome biome = (Biome) o;
        return id == biome.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public enum Precipitation {
        NONE, RAIN, SNOW
    }

    public enum TemperatureModifier {
        NONE, FROZEN
    }

    public static final class Builder {
        private NamespaceID name;
        private float temperature = 0.25f;
        private float downfall = 0.8f;
        private BiomeEffects effects = DEFAULT_EFFECTS;
        private Precipitation precipitation = Precipitation.RAIN;
        private TemperatureModifier temperatureModifier = TemperatureModifier.NONE;

        Builder() {
        }

        public Builder name(NamespaceID name) {
            this.name = name;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public Builder effects(BiomeEffects effects) {
            this.effects = effects;
            return this;
        }

        public Builder precipitation(Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }

        public Builder temperatureModifier(TemperatureModifier temperatureModifier) {
            this.temperatureModifier = temperatureModifier;
            return this;
        }

        public Biome build() {
            return new Biome(
                    name,
                    temperature,
                    downfall,
                    BiomeEffects.builder()
                            .fogColor(effects.fogColor())
                            .skyColor(effects.skyColor())
                            .waterColor(BiomeTexture.getWaterColor(effects.waterColor()))
                            .waterFogColor(effects.waterFogColor())
                            .foliageColor(BiomeTexture.getFoliageColor(effects.foliageColor(), temperature, downfall))
                            .grassColor(BiomeTexture.getGrassColor(effects.grassColor(), temperature, downfall))
                            .grassColorModifier(effects.grassColorModifier())
                            .biomeParticle(effects.biomeParticle())
                            .ambientSound(effects.ambientSound())
                            .moodSound(effects.moodSound())
                            .additionsSound(effects.additionsSound())
                            .music(effects.music())
                            .build(),
                    precipitation,
                    temperatureModifier
            );
        }
    }

    public static void resetCounter() {
        ID_COUNTER.set(0);
    }
}
