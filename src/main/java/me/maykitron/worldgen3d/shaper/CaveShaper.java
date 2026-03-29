package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class CaveShaper {
    private final SimplexOctaveGenerator caveNoise;

    public CaveShaper(SimplexOctaveGenerator caveNoise) {
        this.caveNoise = caveNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.caveMode.equalsIgnoreCase("CUSTOM")) return density;

        // 3D Gürültü Haritasını Çıkar
        double noise3D = caveNoise.noise(x * biome.caveFreq, y * biome.caveFreq, z * biome.caveFreq, 0.5, 0.5, true);

        // YML'deki eşik değerini (örn: 0.65) aşan noktalar "Peynir Mağarası" (Cheese Cave) olur!
        if (noise3D > biome.caveThreshold) {
            // Yoğunluğu eksiye çekerek bloğu tamamen yok et (HAVA yap)
            density -= 50.0;
        }

        // İnce ve uzun tüneller (Spagetti Mağaralar) için mutlak değer mantığı
        if (Math.abs(noise3D) < 0.03) {
            density -= 50.0;
        }

        return density;
    }
}