package net.defade.amestify.world.biome;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BiomeParser {
    public static byte[] encode(Biome biome) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        writeSizedString(biome.name().domain(), dataOutputStream);
        writeSizedString(biome.name().path(), dataOutputStream);

        dataOutputStream.writeFloat(biome.depth());
        dataOutputStream.writeFloat(biome.temperature());
        dataOutputStream.writeFloat(biome.scale());
        dataOutputStream.writeFloat(biome.downfall());

        writeSizedString(biome.category().toString(), dataOutputStream);

        writeBiomeEffects(biome.effects(), dataOutputStream);

        writeSizedString(biome.precipitation().toString(), dataOutputStream);
        writeSizedString(biome.temperatureModifier().toString(), dataOutputStream);

        return byteArrayOutputStream.toByteArray();
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

    private static void writeMoodSound(BiomeEffects.MoodSound moodSound, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(moodSound.sound().domain(), dataOutputStream);
        writeSizedString(moodSound.sound().path(), dataOutputStream);

        dataOutputStream.writeInt(moodSound.tickDelay());
        dataOutputStream.writeInt(moodSound.blockSearchExtent());
        dataOutputStream.writeDouble(moodSound.offset());
    }

    private static void writeAdditionsSound(BiomeEffects.AdditionsSound additionsSound, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(additionsSound.sound().domain(), dataOutputStream);
        writeSizedString(additionsSound.sound().path(), dataOutputStream);

        dataOutputStream.writeDouble(additionsSound.tickChance());
    }

    private static void writeMusic(BiomeEffects.Music music, DataOutputStream dataOutputStream) throws IOException {
        writeSizedString(music.sound().domain(), dataOutputStream);
        writeSizedString(music.sound().path(), dataOutputStream);

        dataOutputStream.writeInt(music.minDelay());
        dataOutputStream.writeInt(music.maxDelay());
        dataOutputStream.writeBoolean(music.replaceCurrentMusic());
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

    private static void writeSizedString(String string, DataOutputStream dataOutputStream) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length, dataOutputStream);
        dataOutputStream.write(bytes);
    }
}
