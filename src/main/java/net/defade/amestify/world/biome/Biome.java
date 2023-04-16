package net.defade.amestify.world.biome;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.defade.amestify.Main;
import net.defade.amestify.graphics.texture.BiomeTexture;
import net.defade.amestify.utils.NamespaceID;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class Biome {
    public static Biome PLAINS;

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    private static final List<Biome> BIOMES = new ArrayList<>();

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

    public static void registerBiome(Biome biome) {
        BIOMES.add(biome);
    }

    public static void unregisterBiome(Biome biome) {
        BIOMES.remove(biome);
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

    public static void init() throws FileNotFoundException {
        InputStream biomesJsonInputStream = Main.class.getClassLoader().getResourceAsStream("biomes.json");
        if(biomesJsonInputStream == null) {
            throw new FileNotFoundException("biomes.json not found");
        }

        JsonObject jsonObject = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .fromJson(new InputStreamReader(biomesJsonInputStream), JsonObject.class);

        for (String biomeId : jsonObject.keySet()) {
            JsonObject biomeObject = jsonObject.getAsJsonObject(biomeId);

            float temperature = biomeObject.getAsJsonPrimitive("temperature").getAsFloat();
            float downfall = biomeObject.getAsJsonPrimitive("downfall").getAsFloat();
            String precipitation = biomeObject.getAsJsonPrimitive("precipitation").getAsString();
            int fogColor = biomeObject.getAsJsonPrimitive("fogColor").getAsInt();
            int waterColor = biomeObject.getAsJsonPrimitive("waterColor").getAsInt();
            int waterFogColor = biomeObject.getAsJsonPrimitive("waterFogColor").getAsInt();
            int skyColor = biomeObject.getAsJsonPrimitive("skyColor").getAsInt();
            int foliageColor = biomeObject.getAsJsonPrimitive("foliageColor").getAsInt();
            String grassColorModifier = biomeObject.getAsJsonPrimitive("grassColorModifier").getAsString();
            int grassColor = biomeObject.has("grassColorOverride") ? biomeObject.getAsJsonPrimitive("grassColorOverride").getAsInt() : -1;

            // Values of 0 means that the client must generate the color with a .png file
            // (see https://minecraft.fandom.com/wiki/Color#Biome_colors).
            // The client is actually expecting the value -1 as 0 means black.
            if(foliageColor == 0) foliageColor = -1;
            if(skyColor == 0) skyColor = -1;

            Biome biome = new Builder()
                    .name(NamespaceID.from(biomeId))
                    .temperature(temperature)
                    .downfall(downfall)
                    .precipitation(Precipitation.valueOf(precipitation.toUpperCase()))
                    .effects(new BiomeEffects.Builder()
                            .fogColor(fogColor)
                            .waterColor(waterColor)
                            .waterFogColor(waterFogColor)
                            .skyColor(skyColor)
                            .foliageColor(foliageColor)
                            .grassColorModifier(BiomeEffects.GrassColorModifier.valueOf(grassColorModifier.toUpperCase()))
                            .grassColor(grassColor)
                            .build())
                    .build();

            registerBiome(biome);
            if(biomeId.equals("minecraft:plains")) {
                PLAINS = biome;
            }
        }
    }

    public static Biome getByName(NamespaceID from) {
        for (Biome biome : BIOMES) {
            if(biome.name.path().equals(from.path())) {
                return biome;
            }
        }

        return null;
    }

    public static Biome getMinecraftBiomeByName(String name) {
        for (Biome biome : BIOMES) {
            if(biome.name.asString().equals(name)) {
                return biome;
            }
        }

        return null;
    }

    public static Biome getById(int id) {
        return BIOMES.get(id);
    }

    public static Collection<Biome> unmodifiableCollection() {
        return Collections.unmodifiableCollection(BIOMES);
    }
}
