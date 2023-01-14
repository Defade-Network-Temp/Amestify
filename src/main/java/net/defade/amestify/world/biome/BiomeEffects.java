package net.defade.amestify.world.biome;

import net.defade.amestify.utils.NamespaceID;

public record BiomeEffects(int fogColor, int skyColor, int waterColor, int waterFogColor, int foliageColor,
                           int grassColor,
                           GrassColorModifier grassColorModifier, BiomeParticle biomeParticle,
                           NamespaceID ambientSound, MoodSound moodSound, AdditionsSound additionsSound,
                           Music music) {

    public static Builder builder() {
        return new Builder();
    }

    public enum GrassColorModifier {
        NONE, DARK_FOREST, SWAMP
    }

    public record MoodSound(NamespaceID sound, int tickDelay, int blockSearchExtent, double offset) { }

    public record AdditionsSound(NamespaceID sound, double tickChance) { }

    public record Music(NamespaceID sound, int minDelay, int maxDelay, boolean replaceCurrentMusic) { }

    public static final class Builder {
        private int fogColor;
        private int skyColor;
        private int waterColor;
        private int waterFogColor;
        private int foliageColor = -1;
        private int grassColor = -1;
        private GrassColorModifier grassColorModifier;
        private BiomeParticle biomeParticle;
        private NamespaceID ambientSound;
        private MoodSound moodSound;
        private AdditionsSound additionsSound;
        private Music music;

        Builder() {
        }

        public Builder fogColor(int fogColor) {
            this.fogColor = fogColor;
            return this;
        }

        public Builder skyColor(int skyColor) {
            this.skyColor = skyColor;
            return this;
        }

        public Builder waterColor(int waterColor) {
            this.waterColor = waterColor;
            return this;
        }

        public Builder waterFogColor(int waterFogColor) {
            this.waterFogColor = waterFogColor;
            return this;
        }

        public Builder foliageColor(int foliageColor) {
            this.foliageColor = foliageColor;
            return this;
        }

        public Builder grassColor(int grassColor) {
            this.grassColor = grassColor;
            return this;
        }

        public Builder grassColorModifier(GrassColorModifier grassColorModifier) {
            this.grassColorModifier = grassColorModifier;
            return this;
        }

        public Builder biomeParticle(BiomeParticle biomeParticle) {
            this.biomeParticle = biomeParticle;
            return this;
        }

        public Builder ambientSound(NamespaceID ambientSound) {
            this.ambientSound = ambientSound;
            return this;
        }

        public Builder moodSound(MoodSound moodSound) {
            this.moodSound = moodSound;
            return this;
        }

        public Builder additionsSound(AdditionsSound additionsSound) {
            this.additionsSound = additionsSound;
            return this;
        }

        public Builder music(Music music) {
            this.music = music;
            return this;
        }

        public BiomeEffects build() {
            return new BiomeEffects(fogColor, skyColor, waterColor, waterFogColor, foliageColor,
                    grassColor, grassColorModifier, biomeParticle,
                    ambientSound, moodSound, additionsSound, music);
        }
    }
}
