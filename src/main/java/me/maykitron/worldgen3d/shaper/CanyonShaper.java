package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * CanyonShaper — Derin kanyon ve yarık sistemi.
 *
 * Algoritma:
 * 1. Düşük frekanslı noise ile kanyon ekseni belirlenir.
 * 2. |noise| < width → kanal içindeyiz.
 * 3. Kanal içinde yoğunluk büyük negatif değerle kırılır → derin oyuk.
 * 4. Kanal duvarları sarp bırakılır (cliff etkisi için pozitif katkı).
 * 5. Kanal tabanı: GLOBAL_SEA_LEVEL - canyonDepth
 */
public class CanyonShaper {

    private final SimplexOctaveGenerator canyonNoise;

    public CanyonShaper(SimplexOctaveGenerator canyonNoise) {
        this.canyonNoise = canyonNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.canyonEnabled) return density;

        // Kanyon sadece belirli yükseklik aralığında çalışır
        int canyonFloor = CustomChunkGenerator.GLOBAL_SEA_LEVEL - biome.canyonDepth;
        if (y < canyonFloor - 5 || y > biome.baseHeight + biome.heightVariation) return density;

        // Ana kanyon ekseni: düşük frekans = uzun ve kıvrımlı kanyonlar
        double n1 = canyonNoise.noise(x * biome.canyonFreq, z * biome.canyonFreq, 0.5, 0.5, true);
        // İkinci noise farklı açıda → kanyonu kıvırır, T kavşakları oluşturur
        double n2 = canyonNoise.noise(
                x * biome.canyonFreq * 1.3 + 500,
                z * biome.canyonFreq * 0.8 + 300,
                0.5, 0.5, true);

        double canyonVal = Math.min(Math.abs(n1), Math.abs(n2));

        if (canyonVal < biome.canyonWidth) {
            // Kanyon içindeyiz — ne kadar merkezdeyiz? [0=merkez, 1=kenar]
            double edge = canyonVal / biome.canyonWidth;

            // Kanal tabanından yukarıda mıyız?
            if (y > canyonFloor) {
                // Eğrisel oyma: merkezdeyken güçlü, kenarda yumuşak
                double carve = (1.0 - edge) * (1.0 - edge); // karesel
                density -= carve * 80.0;
            }

            // Duvar etkisi: kenar hattında yoğunluğu artır → sarp cliff
            double wallZone = Math.abs(edge - 0.85);
            if (wallZone < 0.12) {
                density += (0.12 - wallZone) * 40.0;
            }
        }

        return density;
    }
}
