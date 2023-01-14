package net.defade.amestify.world.biome;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.defade.amestify.Main;
import net.defade.amestify.utils.NamespaceID;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Biome {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    private static final Map<Integer, Biome> BIOMES = new HashMap<>();

    private static final BiomeEffects DEFAULT_EFFECTS = BiomeEffects.builder()
            .fogColor(0xC0D8FF)
            .skyColor(0x78A7FF)
            .waterColor(0x3F76E4)
            .waterFogColor(0x50533)
            .build();

    private final int id = ID_COUNTER.getAndIncrement();

    private final NamespaceID name;
    private final float depth;
    private final float temperature;
    private final float scale;
    private final float downfall;
    private final Category category;
    private final BiomeEffects effects;
    private final Precipitation precipitation;
    private final TemperatureModifier temperatureModifier;

    Biome(NamespaceID name, float depth, float temperature, float scale, float downfall, Category category, BiomeEffects effects, Precipitation precipitation, TemperatureModifier temperatureModifier) {
        this.name = name;
        this.depth = depth;
        this.temperature = temperature;
        this.scale = scale;
        this.downfall = downfall;
        this.category = category;
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

    public float depth() {
        return this.depth;
    }

    public float temperature() {
        return this.temperature;
    }

    public float scale() {
        return this.scale;
    }

    public float downfall() {
        return this.downfall;
    }

    public Category category() {
        return this.category;
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

    public enum Precipitation {
        NONE, RAIN, SNOW
    }

    public enum Category {
        NONE, TAIGA, EXTREME_HILLS, JUNGLE, MESA, PLAINS,
        SAVANNA, ICY, THE_END, BEACH, FOREST, OCEAN,
        DESERT, RIVER, SWAMP, MUSHROOM, NETHER, UNDERGROUND,
        MOUNTAIN
    }

    public enum TemperatureModifier {
        NONE, FROZEN
    }

    public static final class Builder {
        private NamespaceID name;
        private float depth = 0.2f;
        private float temperature = 0.25f;
        private float scale = 0.2f;
        private float downfall = 0.8f;
        private Category category = Category.NONE;
        private BiomeEffects effects = DEFAULT_EFFECTS;
        private Precipitation precipitation = Precipitation.RAIN;
        private TemperatureModifier temperatureModifier = TemperatureModifier.NONE;

        Builder() {
        }

        public Builder name(NamespaceID name) {
            this.name = name;
            return this;
        }

        public Builder depth(float depth) {
            this.depth = depth;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
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
            return new Biome(name, depth, temperature, scale, downfall, category, effects, precipitation, temperatureModifier);
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

            BIOMES.put(biome.id, biome);
        }
    }

    public static Biome getByName(NamespaceID from) {
        for (Biome biome : BIOMES.values()) {
            if(biome.name.path().equals(from.path())) {
                return biome;
            }
        }

        return null;
    }

    public static Biome getMinecraftBiomeByName(String name) {
        for (Biome biome : BIOMES.values()) {
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
        return Collections.unmodifiableCollection(BIOMES.values());
    }
}
