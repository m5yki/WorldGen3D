package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class RiverShaper {
    private final SimplexOctaveGenerator riverNoise;

    public RiverShaper(SimplexOctaveGenerator riverNoise) {
        this.riverNoise = riverNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.riverEnabled) return density;

        double rNoise = riverNoise.noise(x * 0.001, z * 0.001, 0.5, 0.5, true);
        double riverValley = Math.abs(rNoise);

        // Nehir yatağının içindeysek yoğunluğu aşındır (oy)
        if (riverValley < biome.riverWidth) {
            double carveForce = (biome.riverWidth - riverValley) / biome.riverWidth;

            // YML'den gelen riverDepth değerine göre nehir tabanını belirliyoruz
            double riverBedY = biome.waterLevel - biome.riverDepth;

            // Eğer bloğun Y'si nehir tabanından yüksekteyse, o bloğu havaya uçur!
            if (y > riverBedY) {
                density -= (carveForce * carveForce) * 25.0; // Devasa 3D oyma kuvveti
            }
        }
        return density;
    }
}