package me.maykitron.worldgen3d.generator;

import org.bukkit.util.noise.SimplexOctaveGenerator;
import java.util.HashMap;
import java.util.Map;

public class BiomeBlender {

    private final CustomBiomeProvider biomeProvider;
    private final CustomChunkGenerator chunkGenerator;

    // Geçişleri devasa bir şekilde yayvanlaştıran 48 blokluk tampon bölge
    private static final int BLEND_RADIUS = 48;
    private static final int BLEND_STEP = 4;
    private static final int RADIUS_SQUARED = BLEND_RADIUS * BLEND_RADIUS;

    public BiomeBlender(CustomBiomeProvider biomeProvider, CustomChunkGenerator chunkGenerator) {
        this.biomeProvider = biomeProvider;
        this.chunkGenerator = chunkGenerator;
    }

    public double getBlendedHeight(int realX, int realZ, SimplexOctaveGenerator heightNoise) {
        // 1. AŞAMA: Etraftaki Biyomların Ağırlıklarını (Etki Alanlarını) Topla
        Map<String, Double> biomeWeights = new HashMap<>();
        double totalWeight = 0;

        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx += BLEND_STEP) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz += BLEND_STEP) {

                // Karekök almadan karesel hesap (Fast Math)
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > RADIUS_SQUARED) continue;

                // SmoothStep (Kusursuz Kavis) Ağırlık Formülü
                double weight = 1.0 - ((double) distanceSquared / RADIUS_SQUARED);
                weight = weight * weight;

                String bName = biomeProvider.getCustomBiomeName(realX + dx, realZ + dz);
                biomeWeights.put(bName, biomeWeights.getOrDefault(bName, 0.0) + weight);
                totalWeight += weight;
            }
        }

        double finalBlendedHeight = 0;
        double finalWaterLevel = 0;

        // ==========================================================
        // 2. AŞAMA: BİYOM ÇARPIŞTIRICISI (Senin bahsettiğin mantık!)
        // Her biyomun yüksekliğini sanki tüm dünya o biyommuş gibi ayrı
        // hesaplayıp, yüzdelik ağırlıklarına göre "çapraz" birleştiriyoruz.
        // ==========================================================
        for (Map.Entry<String, Double> entry : biomeWeights.entrySet()) {
            String bName = entry.getKey();
            double weightRatio = entry.getValue() / totalWeight; // Örn: %70 Ova, %30 Orman

            CustomChunkGenerator.TerrainData data = chunkGenerator.getTerrainData(bName);

            // Bu kordinat sadece bu biyom olsaydı yüksekliği ne olurdu?
            double noiseValue = heightNoise.noise(realX * data.roughness, realZ * data.roughness, 0.5, 0.5, true);
            double biomeHeight = data.baseHeight + (noiseValue * data.heightVariation);

            // Çapraz karışımı (Gradient) nihai sonuca ekle
            finalBlendedHeight += biomeHeight * weightRatio;
            finalWaterLevel += data.waterLevel * weightRatio;
        }

        // ==========================================================
        // 3. AŞAMA: TERAS KIRICI (Micro-Dithering)
        // Ovalardaki o anlamsız basamak basamak (merdiven) şekillerini
        // kırmak için çok ufak bir titreşim gürültüsü ekliyoruz.
        // ==========================================================
        double microNoise = heightNoise.noise(realX * 0.15, realZ * 0.15, 0.5, 0.5, true);
        finalBlendedHeight += microNoise * 1.5; // Maksimum 1.5 blokluk doğal toprak yamulması

        // ==========================================================
        // 4. AŞAMA: KUSURSUZ SAHİL VE OKYANUS MATEMATİĞİ
        // ==========================================================
        if (finalBlendedHeight < finalWaterLevel + 2) {
            double depthFromWater = finalWaterLevel - finalBlendedHeight;
            if (depthFromWater <= 0) { // Sığ kumsal
                finalBlendedHeight = finalWaterLevel + Math.pow(finalBlendedHeight - finalWaterLevel, 0.5);
            } else { // Okyanus tabanı
                double oceanFloor = finalWaterLevel - (Math.sqrt(depthFromWater) * 4.5);
                finalBlendedHeight = Math.max(oceanFloor, 32);
            }
        }

        return finalBlendedHeight;
    }
}