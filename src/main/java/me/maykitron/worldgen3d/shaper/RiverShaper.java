package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class RiverShaper {
    private final SimplexOctaveGenerator riverNoise;

    public RiverShaper(SimplexOctaveGenerator riverNoise) {
        this.riverNoise = riverNoise;
    }

    // YENİ: baseHeight parametresi eklendi
    public double apply(int x, int y, int z, double density, double baseHeight, TerrainData biome) {
        if (!biome.riverEnabled) return density;

        double rNoise = riverNoise.noise(x * 0.001, z * 0.001, 0.5, 0.5, true);
        double riverValley = Math.abs(rNoise);

        // Nehir yatağının içindeysek
        if (riverValley < biome.riverWidth) {
            double carveForce = (biome.riverWidth - riverValley) / biome.riverWidth;

            // YENİ MANTIK: Nehrin yüzeyi küresel denize (62) değil, bulunduğu arazinin yüksekliğine bağlı
            double localRiverSurface = baseHeight - (carveForce * 4.0);
            double riverBedY = localRiverSurface - biome.riverDepth;

            // Eğer bloğun Y'si nehir tabanından yüksekteyse o bölgeyi havaya uçur
            if (y > riverBedY) {
                density -= (carveForce * carveForce) * 30.0; // Vadiyi açmak için güçlü oyma
            }
        }
        return density;
    }
}