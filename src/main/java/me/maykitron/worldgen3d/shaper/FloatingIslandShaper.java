package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * FloatingIslandShaper — Gökyüzünde yüzen ada bantları.
 *
 * Algoritma:
 * 1. İki ayrı 3D noise: üst yüzey (adanın tepesi) ve alt oyma (adanın altı).
 * 2. YML'deki min-y / max-y bandında çalışır.
 * 3. Üst noise > eşik → katı blok (ada gövdesi).
 * 4. Alt noise karesel azalma ile adanın altını oyar → sallantılı alt yüzey.
 * 5. Her biyomda tetiklenebilir; yüzen-ada biyomları için daha yüksek yoğunluk.
 *
 * YML: floating-islands.enabled: true
 */
public class FloatingIslandShaper {

    private final SimplexOctaveGenerator floatingNoise;

    private static final double ISLAND_THRESHOLD   = 0.55; // üst yüzey eşiği
    private static final double ISLAND_FREQ        = 0.018; // ada boyutu
    private static final double ISLAND_FREQ_VERT   = 0.025; // dikey sıkıştırma

    public FloatingIslandShaper(SimplexOctaveGenerator floatingNoise) {
        this.floatingNoise = floatingNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.floatingIslandsEnabled) return density;

        int minY = biome.floatingIslandMinY;
        int maxY = biome.floatingIslandMaxY;

        // Band dışındaysa hiç hesaplama yapma
        if (y < minY - 20 || y > maxY + 10) return density;

        // 3D noise — hem yatay hem dikey boyut
        double n = floatingNoise.noise(
                x * ISLAND_FREQ,
                y * ISLAND_FREQ_VERT,
                z * ISLAND_FREQ,
                0.5, 0.5, true);

        // Bant içinde mi? [0=bant altı, 1=bant üstü]
        double bandProgress = (double)(y - minY) / (maxY - minY);
        bandProgress = Math.max(0.0, Math.min(1.0, bandProgress));

        // Ada yoğunluğu: bant ortasında maksimum, kenarlarda sıfıra yakın
        // Smoothstep: 0→0, 0.5→1, 1→0
        double bandWeight = 1.0 - Math.abs(bandProgress * 2.0 - 1.0);
        bandWeight = bandWeight * bandWeight * (3.0 - 2.0 * bandWeight); // smoothstep

        if (n > ISLAND_THRESHOLD && bandWeight > 0.15) {
            // Ada gövdesi — noise ve bant ağırlığıyla ölçeklenir
            double islandDensity = (n - ISLAND_THRESHOLD) / (1.0 - ISLAND_THRESHOLD);
            density += islandDensity * bandWeight * 60.0;
        }

        // Alt oyma: adanın altını serbest bırak (yerçekimsiz görünüm)
        if (y < minY + (maxY - minY) * 0.4) {
            double underNoise = floatingNoise.noise(
                    x * ISLAND_FREQ * 1.2 + 100,
                    y * ISLAND_FREQ_VERT * 1.5,
                    z * ISLAND_FREQ * 1.2 + 100,
                    0.5, 0.5, true);

            if (underNoise > 0.40) {
                // Alt yüzeyi kademelice oy
                double carve = (underNoise - 0.40) / 0.60;
                density -= carve * carve * 50.0 * (1.0 - bandProgress);
            }
        }

        return density;
    }
}
