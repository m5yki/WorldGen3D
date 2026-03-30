package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * OceanTrenchShaper — Derin okyanus hendekleri.
 *
 * Algoritma:
 * 1. Düşük frekanslı noise ile hendek ekseni belirlenir.
 * 2. Sadece derin okyanus biyomlarında (baseHeight < 45) aktif olur.
 * 3. Okyanus tabanını ekstra trenchExtraDepth kadar aşağı çeker.
 * 4. Hendek duvarları sarp → belirgin uçurum etkisi.
 *
 * YML: trench.enabled: true (sadece okyanus biyomlarına ekle)
 */
public class OceanTrenchShaper {

    private final SimplexOctaveGenerator trenchNoise;

    private static final double TRENCH_FREQ  = 0.0015; // uzun ve dar hendekler
    private static final double TRENCH_WIDTH = 0.040;  // hendek genişliği

    public OceanTrenchShaper(SimplexOctaveGenerator trenchNoise) {
        this.trenchNoise = trenchNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.trenchEnabled) return density;

        // Sadece derin okyanus tabanında çalış
        if (biome.baseHeight > 45) return density;

        int trenchFloor = CustomChunkGenerator.GLOBAL_SEA_LEVEL
                         - (biome.baseHeight > 0 ? (62 - biome.baseHeight) : 30)
                         - biome.trenchExtraDepth;
        trenchFloor = Math.max(trenchFloor, -55); // mutlak dip

        // Hendek ekseni — iki farklı açıda noise → çapraz hendek ağı
        double n1 = trenchNoise.noise(x * TRENCH_FREQ, z * TRENCH_FREQ * 0.6, 0.5, 0.5, true);
        double n2 = trenchNoise.noise(
                x * TRENCH_FREQ * 0.7 + 200,
                z * TRENCH_FREQ * 1.1 + 400,
                0.5, 0.5, true);

        // En dar hendek eksenini seç
        double trenchVal = Math.min(Math.abs(n1), Math.abs(n2));

        if (trenchVal < TRENCH_WIDTH) {
            double edge = trenchVal / TRENCH_WIDTH; // [0=merkez, 1=kenar]

            if (y > trenchFloor) {
                // Kademelice derinleşen oyma
                double carve = (1.0 - edge) * (1.0 - edge);
                density -= carve * (biome.trenchExtraDepth * 1.2);
            }

            // Sarp duvar: kenar hattında pozitif katkı
            if (edge > 0.78 && edge < 1.0) {
                density += (1.0 - edge) * 30.0;
            }
        }

        return density;
    }
}
