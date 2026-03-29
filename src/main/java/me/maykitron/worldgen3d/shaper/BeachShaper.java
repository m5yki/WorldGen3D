package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;

public class BeachShaper {
    public double apply(int y, double density, TerrainData biome) {
        if (biome.name.contains("Beach")) {
            int distToWater = Math.abs(y - biome.waterLevel);
            // Su seviyesinin 4 blok altı ve üstünü hedefler
            if (distToWater < 4) {
                // Yoğunluğu 0'a çekmek demek, o noktayı tam yüzey çizgisine oturtmak demektir
                density *= 0.2;
            }
        }
        return density;
    }
}