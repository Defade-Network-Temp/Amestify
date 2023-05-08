package net.defade.amestify.world;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.defade.amestify.Main;
import net.defade.amestify.graphics.BiomeColorLayer;
import net.defade.amestify.loaders.anvil.RegionFile;
import net.defade.amestify.utils.NamespaceID;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.biome.BiomeEffects;
import net.defade.amestify.world.chunk.pos.RegionPos;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<Integer, Biome> biomes = new HashMap<>();
    private final BiomeColorLayer biomeColorLayer = new BiomeColorLayer();
    private final Biome plainsBiome;

    private final Map<RegionPos, RegionFile> regionFiles = new HashMap<>();

    public World() throws FileNotFoundException {
        Biome.resetCounter();

        InputStream biomesJsonInputStream = Main.class.getClassLoader().getResourceAsStream("biomes.json");
        if(biomesJsonInputStream == null) {
            throw new FileNotFoundException("biomes.json not found");
        }

        JsonObject jsonObject = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .fromJson(new InputStreamReader(biomesJsonInputStream), JsonObject.class);

        Biome plains = null;
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

            Biome biome = Biome.builder()
                    .name(NamespaceID.from(biomeId))
                    .temperature(temperature)
                    .downfall(downfall)
                    .precipitation(Biome.Precipitation.valueOf(precipitation.toUpperCase()))
                    .effects(BiomeEffects.builder()
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
                plains = biome;
            }
        }

        plainsBiome = plains;
    }

    public Biome getPlainsBiome() {
        return plainsBiome;
    }

    public Biome getBiomeByName(NamespaceID from) {
        for (Biome biome : unmodifiableBiomeCollection()) {
            if(biome.name().path().equals(from.path())) {
                return biome;
            }
        }

        return null;
    }

    public Biome getMinecraftBiomeByName(String from) {
        for (Biome biome : unmodifiableBiomeCollection()) {
            if(biome.name().asString().equals(from)) {
                return biome;
            }
        }

        return null;
    }

    public Biome getBiomeById(int id) {
        return biomes.get(id);
    }

    public Collection<Biome> unmodifiableBiomeCollection() {
        return biomes.values();
    }

    public void registerBiome(Biome biome) {
        biomeColorLayer.registerBiome(biome);
        biomes.put(biome.id(), biome);
    }

    public void unregisterBiome(Biome biome) {
        biomes.remove(biome.id());
        getRegions().forEach(regionFile -> regionFile.unregisterBiome(biome));
    }

    public void addRegion(RegionFile regionFile) {
        regionFiles.put(regionFile.getRegionPos(), regionFile);
    }

    public RegionFile getRegion(int regionX, int regionZ) {
        return regionFiles.get(new RegionPos(regionX, regionZ));
    }

    public Collection<RegionFile> getRegions() {
        return regionFiles.values();
    }

    public Biome getBiomeAt(int x, int z) {
        RegionFile regionFile = regionFiles.get(new RegionPos(x >> 9, z >> 9));
        if(regionFile == null) return null;
        return regionFile.getMapViewerBiome(x & 0x1FF, z & 0x1FF, 0);
    }

    public BiomeColorLayer getBiomeColorLayer() {
        return biomeColorLayer;
    }
}
