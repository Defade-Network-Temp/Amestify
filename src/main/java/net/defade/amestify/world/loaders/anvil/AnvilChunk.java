package net.defade.amestify.world.loaders.anvil;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.Block;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.Section;
import net.defade.amestify.world.viewer.MapViewerWorld;
import net.defade.amestify.world.pos.ChunkPos;
import net.defade.amestify.world.chunk.palette.AdaptivePalette;
import net.defade.amestify.world.chunk.palette.FilledPalette;
import net.defade.amestify.world.chunk.palette.FlexiblePalette;
import net.defade.amestify.world.chunk.palette.Palette;
import net.querz.mca.CompressionType;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.SNBTSerializer;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class AnvilChunk extends Chunk {
    private static final SNBTSerializer SNBT_SERIALIZER = new SNBTSerializer();

    public AnvilChunk(MapViewerWorld mapViewerWorld, ChunkPos chunkPos, int minY, int maxY, CompressionType compressionType, InputStream inputStream) {
        super(mapViewerWorld, chunkPos, minY, maxY);

        NBTDeserializer nbtDeserializer = new NBTDeserializer(false);
        CompoundTag nbt;

        try {
            nbt = (CompoundTag) nbtDeserializer.fromStream(compressionType.decompress(inputStream)).getTag();
        } catch (IOException exception) {
            exception.printStackTrace();
            return;
        }

        readSections(nbt.getListTag("sections"));
        readHeightmap(nbt);
        readBlockEntities(nbt);
    }

    private void readSections(ListTag<?> sectionsNBT) {
        if(sectionsNBT == null) {
            return;
        }

        for (Tag<?> sectionNBTUncasted : sectionsNBT) {
            CompoundTag sectionNBT = (CompoundTag) sectionNBTUncasted;

            if(!sectionNBT.containsKey("Y")) continue;
            byte sectionY = sectionNBT.getByte("Y");
            if(sectionY < minY >> 4 || sectionY > maxY >> 4){
                continue;
            }

            Section section = getSection(sectionY);
            CompoundTag blockPaletteNBT = sectionNBT.getCompoundTag("block_states");
            if(blockPaletteNBT != null) {
                section.getBlockPalette().setPalette(readBlockNBTPalette(section.getBlockPalette(), blockPaletteNBT));
            }

            CompoundTag biomesPaletteNBT = sectionNBT.getCompoundTag("biomes");
            if(biomesPaletteNBT != null) {
                section.getBiomePalette().setPalette(readBiomeNBTPalette(section.getBiomePalette(), biomesPaletteNBT));
            }

            byte[] blockLight = sectionNBT.getByteArray("BlockLight");
            if(blockLight != null && blockLight.length > 0) {
                section.setBlockLight(blockLight);
            }

            byte[] skyLight = sectionNBT.getByteArray("SkyLight");
            if(skyLight != null && skyLight.length > 0) {
                section.setSkyLight(skyLight);
            }
        }
    }

    private void readHeightmap(CompoundTag chunkNBT) {
        if(chunkNBT.getCompoundTag("Heightmaps") == null) return;
        long[] packedHeightmap = chunkNBT.getCompoundTag("Heightmaps").getLongArray("WORLD_SURFACE");
        if(packedHeightmap.length == 0) {
            return;
        }
        int intPerLong = 7; // Math.floor(64.0 / 9)

        long mask = 511L; //(1 << 9) -1L
        for (int i = 0; i < heightMap.length; i++) {
            int longIndex = i / intPerLong;
            int subIndex = i % intPerLong;
            int value = (int) ((packedHeightmap[longIndex] >> (subIndex * 9)) & mask);

            heightMap[i] = value;
        }
    }

    private void readBlockEntities(CompoundTag nbt) {
        ListTag<?> blockEntitiesNBT = nbt.getListTag("block_entities");
        if(blockEntitiesNBT == null) return;

        for (Tag<?> blockEntityUncasted : blockEntitiesNBT) {
            CompoundTag blockEntity = (CompoundTag) blockEntityUncasted;

            int x = blockEntity.getInt("x");
            int y = blockEntity.getInt("y");
            int z = blockEntity.getInt("z");

            try {
                blockEntities.put(Utils.getBlockIndex(x, y, z), SNBT_SERIALIZER.toString(blockEntity));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    private int getBiomeId(String biomeId) {
        if(biomeId == null) return 0;

        return Objects.requireNonNullElse(mapViewerWorld.getBiomeByName(biomeId), mapViewerWorld.getPlainsBiome()).id();
    }

    private Palette readBiomeNBTPalette(AdaptivePalette adaptivePalette, CompoundTag paletteNBT) {
        ListTag<?> paletteEntries = paletteNBT.getListTag("palette");
        if(paletteEntries == null) return null;

        if(paletteEntries.size() == 1) {
            return new FilledPalette((byte) adaptivePalette.dimension(), getBiomeId(((StringTag) paletteEntries.get(0)).getValue()));
        } else {
            long[] values = paletteNBT.getLongArray("data");
            byte bitsPerEntry = (byte) Math.ceil(Math.log(paletteEntries.size()) / Math.log(2));

            IntArrayList paletteToValueList = new IntArrayList(paletteEntries.size());
            for (Tag<?> paletteEntry : paletteEntries) {
                paletteToValueList.add(getBiomeId(((StringTag) paletteEntry).getValue()));
            }

            FlexiblePalette flexiblePalette = new FlexiblePalette(adaptivePalette, bitsPerEntry, paletteToValueList, -1, values);
            flexiblePalette.recalculateCount();

            return flexiblePalette;
        }
    }

    private static Palette readBlockNBTPalette(AdaptivePalette adaptivePalette, CompoundTag paletteNBT) {
        ListTag<?> paletteEntries = paletteNBT.getListTag("palette");
        if(paletteEntries == null) return null;

        if(paletteEntries.size() == 1) {
            return new FilledPalette((byte) adaptivePalette.dimension(), getBlockStateId((CompoundTag) paletteEntries.get(0)));
        } else {
            long[] values = paletteNBT.getLongArray("data");
            byte bitsPerEntry = (byte) (values.length * 64 / 4096);

            IntArrayList paletteToValueList = new IntArrayList(paletteEntries.size());
            for (Tag<?> paletteEntry : paletteEntries) {
                paletteToValueList.add(getBlockStateId((CompoundTag) paletteEntry));
            }

            FlexiblePalette flexiblePalette = new FlexiblePalette(adaptivePalette, bitsPerEntry, paletteToValueList, -1, values);
            flexiblePalette.recalculateCount();

            return flexiblePalette;
        }
    }

    private static int getBlockStateId(CompoundTag blockStateNBT) {
        String name = blockStateNBT.getString("Name");
        if(name == null) return 0;
        Block block = Block.fromNamespaceId(name);

        CompoundTag properties = blockStateNBT.getCompoundTag("Properties");
        if(properties == null) {
            return block.getDefaultStateId();
        } else {
            String[] propertiesArray = new String[properties.size()];

            int index = 0;
            for (Map.Entry<? extends String, ? extends Tag<?>> property : properties) {
                propertiesArray[index] = property.getKey() + "=" + ((StringTag) property.getValue()).getValue();
                index++;
            }

            return block.getStateIdForProperties(propertiesArray);
        }
    }
}
