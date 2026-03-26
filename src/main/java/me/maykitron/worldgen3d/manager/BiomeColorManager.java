package me.maykitron.worldgen3d.manager;

import org.bukkit.block.Biome;

/**
 * WorldGen3D - Biyom Renk Yoneticisi
 * YML'den okunan "color-style" degerine gore, Minecraft'in istemcisine (Client)
 * gonderilecek en uygun renk tonuna sahip Vanilla biyomunu secer.
 */
public class BiomeColorManager {

    public static Biome getColorBiome(String colorStyle) {
        if (colorStyle == null) return Biome.PLAINS;

        switch (colorStyle.toUpperCase()) {
            case "BRIGHT_GREEN":
                // Ormanlar icin: Fosforlu, cok canli ve acik bir yesil tonu
                return Biome.JUNGLE;

            case "DARK_GREEN":
                // Tayga icin: Koyu, kasvetli ve hafif mat bir yesil
                return Biome.DARK_FOREST;

            case "OLIVE":
                // Savana icin: Sarimtirak, zeytin yesili, kurumaya yuz tutmus otlar
                return Biome.SAVANNA;

            case "BROWNISH":
                // Coller icin: Tamamen soluk, kahverengiye calan tonlar
                return Biome.DESERT;

            case "AQUA":
                // Okyanuslar icin: Turkuaz, acik mavi, tropikal deniz rengi
                return Biome.WARM_OCEAN;

            case "SNOWY":
                // Tundra icin: Soguk atmosfer rengi
                return Biome.SNOWY_PLAINS;

            case "NORMAL":
            default:
                // Ovalar icin: Minecraft'in klasik, standart yesil tonu
                return Biome.PLAINS;
        }
    }
}