package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * VolcanoShaper — Yanardağ konisi, krater ve lav gölü sistemi.
 *
 * Algoritma:
 * 1. Düşük frekanslı noise ile yanardağ merkez noktaları belirlenir.
 *    (Nadir: sadece çok yüksek noise değerlerinde tetiklenir)
 * 2. Merkeze olan yatay mesafeye göre konik yükseltme uygulanır.
 * 3. Zirveye yakın bölgede krater oyulur (ters koni etkisi).
 * 4. Aktif yanardağda krater tabanı lav seviyesine kadar açılır.
 *
 * YML tetikleyici: terrain.type: "VOLCANO"
 */
public class VolcanoShaper {

    private final SimplexOctaveGenerator volcanoNoise;

    // Yanardağ sadece noise bu eşiği aştığında tetiklenir → nadir oluşum
    private static final double TRIGGER_THRESHOLD = 0.72;

    public VolcanoShaper(SimplexOctaveGenerator volcanoNoise) {
        this.volcanoNoise = volcanoNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (!biome.terrainType.equals("VOLCANO")) return density;

        // Çok düşük frekans: büyük aralıklı yanardağlar
        double noiseVal = volcanoNoise.noise(x * 0.0025, z * 0.0025, 0.5, 0.5, true);
        if (noiseVal < TRIGGER_THRESHOLD) return density;

        // Yanardağ merkezini bu noise değerinin maksimum noktasına kitleriz.
        // Bunun için gradyan tırmanışı yerine basit bir yaklaşım:
        // "merkeze ne kadar yakın" = noise değerinin ne kadar eşiğin üstünde olduğu
        double intensity = (noiseVal - TRIGGER_THRESHOLD) / (1.0 - TRIGGER_THRESHOLD); // [0,1]

        // Yatay etki yarıçapı — YML'den gelen coneRadius ile ölçeklenir
        double effectRadius = biome.volcanoConeRadius * intensity;
        if (effectRadius < 5) return density;

        // Cone merkezi: noise'un bu frekanstaki "tepe" noktası etrafında
        // Noise gradient'e bakarak gerçek merkezi bulmak yerine, her piksel
        // kendi lokasyonunda ne kadar kuvvet hissettiğini hesaplar.
        // Bu sayede merkeze yakın bloklar daha fazla etkilenir.
        double coneHeight = biome.baseHeight + (biome.heightVariation * intensity * 1.8);
        int craterR = biome.volcanoCraterRadius;

        // --------------------------------------------------
        // KONEK YÜKSELTMESİ
        // density = baseHeight - y → y < baseHeight: density > 0 (katı)
        // Koninin etkisiyle coneHeight'a kadar katı blok uzatılır.
        // --------------------------------------------------
        double coneFactor = intensity * intensity; // karesel → sivri zirve
        double coneBoost  = coneHeight * coneFactor;

        // Sadece taban yüksekliği üzerinde ve mevcut density pozitifse çalış
        if (y > biome.baseHeight && y <= (int) coneHeight) {
            density += coneBoost * (1.0 - ((double) y / coneHeight));
        }

        // --------------------------------------------------
        // KRATER OYMASI
        // Zirveye yakın bölgede (üst %25) iç koni oyulur.
        // --------------------------------------------------
        int craterTopY = (int) coneHeight;
        int craterBaseY = craterTopY - (craterR * 3);

        if (y >= craterBaseY && y <= craterTopY) {
            double craterProgress = (double)(y - craterBaseY) / (craterTopY - craterBaseY); // [0,1]
            double craterRadius   = craterR * craterProgress; // yukarıya çıktıkça genişler

            // Merkeze olan yatay mesafeyi intensity ile yaklaşık hesapla
            double distFromCenter = (1.0 - intensity) * biome.volcanoConeRadius;

            if (distFromCenter < craterRadius) {
                density -= 70.0; // krater oyuldu
            }
        }

        // --------------------------------------------------
        // LAV GÖLÜ (sadece aktif yanardağlarda)
        // Krater tabanı ile volcanoLavaLevel arasında lav doldurulur.
        // Bu kısım generateNoise'da Material.LAVA olarak işlenir.
        // --------------------------------------------------
        // (Lav dolumu ChunkGenerator'daki density <= 0 && y <= baseLavaLevel
        //  koşulunda halihazırda var. Krater oyulduktan sonra oraya düşer.)

        return density;
    }
}
