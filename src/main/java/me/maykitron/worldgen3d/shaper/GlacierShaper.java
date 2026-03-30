package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * GlacierShaper — Buzul platosu ve buz mağarası sistemi.
 *
 * Algoritma:
 * 1. Buzul platosu: arazi yüksekliğini yatay düzleştirir (buz tabakası etkisi).
 *    Yüzey pürüzleri küçültülür → geniş düz buz alanları.
 * 2. Buz mağarası: düşük eşikli noise ile büyük oval boşluklar açar.
 *    Sadece belirli derinlik aralığında çalışır (yüzeyin 5-40 blok altı).
 * 3. Seracs (buz sütunları): düşük frekanslı dikey noise ile ara sıra
 *    yüzeyden yukarı çıkan buz bloğu sütunları oluşturur.
 *
 * YML: glacier.enabled: true
 */
public class GlacierShaper {

    private final SimplexOctaveGenerator glacierNoise;

    public GlacierShaper(SimplexOctaveGenerator glacierNoise) {
        this.glacierNoise = glacierNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.glacierEnabled) return density;

        // --------------------------------------------------
        // 1. PLATO DÜZLEŞTİRME
        // Yüzey gürültüsünü bastır → geniş düz buzul alanları
        // Mevcut density'yi plato yüksekliğine doğru çek
        // --------------------------------------------------
        double plateauY = biome.baseHeight + 4.0;
        if (y > (int)(plateauY - 6) && y < (int)(plateauY + 8)) {
            // Bu bant içinde density'yi düzleştirici bir kuvvet uygula
            double distFromPlateau = Math.abs(y - plateauY);
            double flattenForce = Math.max(0, 1.0 - (distFromPlateau / 7.0));
            // density > 0 ise katı → platoya yakınsatmak için
            // y < plateauY: density artır | y > plateauY: density azalt
            if (y < plateauY) {
                density += flattenForce * 8.0;
            } else {
                density -= flattenForce * 8.0;
            }
        }

        // --------------------------------------------------
        // 2. BUZ MAĞARALARI
        // Yüzeyin 5-45 blok altında büyük oval boşluklar
        // --------------------------------------------------
        int surfaceApprox = biome.baseHeight;
        if (y < surfaceApprox - 5 && y > surfaceApprox - 45 && y > -50) {
            // Geniş, yavaş değişen noise → büyük oval buz mağaraları
            double cavN = glacierNoise.noise(
                    x * 0.022, y * 0.018, z * 0.022,
                    0.5, 0.5, true);

            // Eşik düşük → büyük boşluklar (peynir mağarası tarzı)
            if (cavN > 0.50) {
                double carve = (cavN - 0.50) / 0.50;
                density -= carve * 55.0;
            }
        }

        // --------------------------------------------------
        // 3. SERAC SÜTUNLARI (Buz sütunları)
        // Yüzeyden 5-20 blok yukarı uzanan ince buz yapıları
        // --------------------------------------------------
        if (y > surfaceApprox && y < surfaceApprox + 22) {
            double seracN = glacierNoise.noise(
                    x * 0.08 + 300, z * 0.08 + 300,
                    0.5, 0.5, true);

            if (seracN > 0.72) {
                // Sütun gücü: yükseldikçe azalır
                double heightFade = 1.0 - ((double)(y - surfaceApprox) / 22.0);
                double pillarStrength = (seracN - 0.72) / 0.28 * heightFade;
                density += pillarStrength * 35.0;
            }
        }

        return density;
    }
}
