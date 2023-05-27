package net.defade.amestify.world.biome;

import net.defade.amestify.utils.NamespaceID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BiomeParser {
    public static byte[] encode(Biome biome) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        writeSizedString(biome.name().domain(), dataOutputStream);
        writeSizedString(biome.name().path(), dataOutputStream);

        dataOutputStream.writeFloat(biome.temperature());
        dataOutputStream.writeFloat(biome.downfall());

        writeBiomeEffects(biome.effects(), dataOutputStream);

        writeSizedString(biome.precipitation().toString(), dataOutputStream);
        writeSizedString(biome.temperatureModifier().toString(), dataOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    public static Biome decode(byte[] biome) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(biome);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        NamespaceID name = NamespaceID.from(readSizedString(dataInputStream), readSizedString(dataInputStream));

        float temperature = dataInputStream.readFloat();
        float downfall = dataInputStream.readFloat();

        BiomeEffects biomeEffects = readBiomeEffects(dataInputStream);

        Biome.Precipitation precipitation = Biome.Precipitation.valueOf(readSizedString(dataInputStream));
        Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.valueOf(readSizedString(dataInputStream));

        return Biome.builder()
                .name(name)
                .temperature(temperature)
                .downfall(downfall)
                .effects(biomeEffects)
                .precipitation(precipitation)
                .temperatureModifier(temperatureModifier)
                .build();
    }

    private static void writeBiomeEffects(BiomeEffects biomeEffect, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(biomeEffect.fogColor());
        dataOutputStream.writeInt(biomeEffect.skyColor());
        dataOutputStream.writeInt(biomeEffect.waterColor());
        dataOutputStream.writeInt(biomeEffect.waterFogColor());
        dataOutputStream.writeInt(biomeEffect.foliageColor());
        dataOutputStream.writeInt(biomeEffect.grassColor());

        dataOutputStream.writeBoolean(biomeEffect.grassColorModifier() != null);
        if (biomeEffect.grassColorModifier() != null) {
            writeSizedString(biomeEffect.grassColorModifier().toString(), dataOutputStream);
        }

        dataOutputStream.writeBoolean(biomeEffect.biomeParticle() != null);
        if (biomeEffect.biomeParticle() != null) {
            writeBiomeParticle(biomeEffect.biomeParticle(), dataOutputStream);
        }

        dataOutputStream.writeBoolean(biomeEffect.ambientSound() != null);
        if (biomeEffect.ambientSound() != null) {
            writeSizedString(biomeEffect.ambientSound().domain(), dataOutputStream);
            writeSizedString(biomeEffect.ambientSound().path(), dataOutputStream);
        }

        dataOutputStream.writeBoolean(biomeEffect.moodSound() != null);
        if(biomeEffect.moodSound() != null) {
            writeMoodSound(biomeEffect.moodSound(), dataOutputStream);
        }

        dataOutputStream.writeBoolean(biomeEffect.additionsSound() != null);
        if(biomeEffect.additionsSound() != null) {
            writeAdditionsSound(biomeEffect.additionsSound(), dataOutputStream);
        }

        dataOutputStream.writeBoolean(biomeEffect.music() != null);
        if(biomeEffect.music() != null) {
            writeMusic(biomeEffect.music(), dataOutputStream);
        }
    }

    private static BiomeEffects readBiomeEffects(DataInputStream dataInputStream) throws IOException {
        int fogColor = dataInputStream.readInt();
        int skyColor = dataInputStream.readInt();
        int waterColor = dataInputStream.readInt();
        int waterFogColor = dataInputStream.readInt();
        int foliageColor = dataInputStream.readInt();
        int grassColor = dataInputStream.readInt();

        BiomeEffects.GrassColorModifier grassColorModifier = null;
        if(dataInputStream.readBoolean()) {
            grassColorModifier = BiomeEffects.GrassColorModifier.valueOf(readSizedString(dataInputStream));
        }

        BiomeParticle biomeParticle = null;
        if(dataInputStream.readBoolean()) {
            biomeParticle = readBiomeParticle(dataInputStream);
        }

        NamespaceID ambientSound = null;
        if(dataInputStream.readBoolean()) {
            ambientSound = NamespaceID.from(readSizedString(dataInputStream), readSizedString(dataInputStream));
        }

        BiomeEffects.MoodSound moodSound = null;
        if(dataInputStream.readBoolean()) {
            moodSound = readMoodSound(dataInputStream);
        }

        BiomeEffects.AdditionsSound additionsSound = null;
        if(dataInputStream.readBoolean()) {
            additionsSound = readAdditionsSound(dataInputStream);
        }

        BiomeEffects.Music music = null;
        if(dataInputStream.readBoolean()) {
            music = readMusic(dataInputStream);
        }

        return new BiomeEffects(fogColor, skyColor, waterColor, waterFogColor, foliageColor, grassColor, grassColorModifier, biomeParticle, ambientSound, moodSound, additionsSound, music);
    }

    private static void writeBiomeParticle(BiomeParticle biomeParticle, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeFloat(biomeParticle.probability());

        if (biomeParticle.option() instanceof BiomeParticle.BlockOption blockOption) {
            dataOutputStream.write(0);

            dataOutputStream.writeShort(blockOption.blockStateId());
        } else if (biomeParticle.option() instanceof BiomeParticle.DustOption dustOption) {
            dataOutputStream.write(1);

            dataOutputStream.writeFloat(dustOption.red());
            dataOutputStream.writeFloat(dustOption.green());
            dataOutputStream.writeFloat(dustOption.blue());
            dataOutputStream.writeFloat(dustOption.scale());
        }
    }

    private static BiomeParticle readBiomeParticle(DataInputStream dataInputStream) throws IOException {
        float probability = dataInputStream.readFloat();
        BiomeParticle.Option option = null;

        byte optionType = dataInputStream.readByte();
        switch (optionType) {
            case 0 -> option = new BiomeParticle.BlockOption(dataInputStream.readShort());

            case 1 -> {
                float red, green, blue, scale;

                red = dataInputStream.readFloat();
                green = dataInputStream.readFloat();
                blue = dataInputStream.readFloat();
                scale = dataInputStream.readFloat();

                option = new BiomeParticle.DustOption(red, green, blue, scale);
            }
        }

        return new BiomeParticle(probability, option);
    }

    private static void writeMoodSound(BiomeEffects.MoodSound moodSound, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(moodSound.sound().domain(), dataOutputStream);
        writeSizedString(moodSound.sound().path(), dataOutputStream);

        dataOutputStream.writeInt(moodSound.tickDelay());
        dataOutputStream.writeInt(moodSound.blockSearchExtent());
        dataOutputStream.writeDouble(moodSound.offset());
    }

    private static BiomeEffects.MoodSound readMoodSound(DataInputStream dataInputStream) throws IOException {
        NamespaceID sound = NamespaceID.from(readSizedString(dataInputStream), readSizedString(dataInputStream));

        int tickDelay = dataInputStream.readInt();
        int blockSearchExtent = dataInputStream.readInt();
        double offset = dataInputStream.readDouble();

        return new BiomeEffects.MoodSound(sound, tickDelay, blockSearchExtent, offset);
    }

    private static void writeAdditionsSound(BiomeEffects.AdditionsSound additionsSound, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(additionsSound.sound().domain(), dataOutputStream);
        writeSizedString(additionsSound.sound().path(), dataOutputStream);

        dataOutputStream.writeDouble(additionsSound.tickChance());
    }

    private static BiomeEffects.AdditionsSound readAdditionsSound(DataInputStream dataInputStream) throws IOException {
        NamespaceID sound = NamespaceID.from(readSizedString(dataInputStream), readSizedString(dataInputStream));
        double tickChance = dataInputStream.readDouble();

        return new BiomeEffects.AdditionsSound(sound, tickChance);
    }

    private static void writeMusic(BiomeEffects.Music music, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(music.sound().domain(), dataOutputStream);
        writeSizedString(music.sound().path(), dataOutputStream);

        dataOutputStream.writeInt(music.minDelay());
        dataOutputStream.writeInt(music.maxDelay());
        dataOutputStream.writeBoolean(music.replaceCurrentMusic());
    }

    private static BiomeEffects.Music readMusic(DataInputStream dataInputStream) throws IOException {
        NamespaceID sound = NamespaceID.from(readSizedString(dataInputStream), readSizedString(dataInputStream));

        int minDelay = dataInputStream.readInt();
        int maxDelay = dataInputStream.readInt();
        boolean replaceCurrentMusic = dataInputStream.readBoolean();

        return new BiomeEffects.Music(sound, minDelay, maxDelay, replaceCurrentMusic);
    }

    private static void writeVarInt(int value, DataOutputStream dataOutputStream) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                dataOutputStream.writeByte(value);
                return;
            }

            dataOutputStream.writeByte((value & 0x7F) | 0x80);

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    private static int readVarInt(DataInputStream dataInputStream) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = dataInputStream.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }


    private static void writeSizedString(String string, DataOutputStream dataOutputStream) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length, dataOutputStream);
        dataOutputStream.write(bytes);
    }

    private static String readSizedString(DataInputStream dataInputStream) throws IOException {
        int length = readVarInt(dataInputStream);
        byte[] bytes = new byte[length];
        dataInputStream.readFully(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
