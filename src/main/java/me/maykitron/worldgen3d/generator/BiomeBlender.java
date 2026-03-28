package me.maykitron.worldgen3d.generator;

import org.bukkit.util.noise.SimplexOctaveGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BiomeBlender {

    private final CustomChunkGenerator chunkGenerator;

    // Geçişleri devasa bir şekilde yayvanlaştıran 48 blokluk tampon bölge
    private static final int BLEND_RADIUS = 48;
    private static final int BLEND_STEP = 4;
    private static final int RADIUS_SQUARED = BLEND_RADIUS * BLEND_RADIUS;

    // YENİ: Önceden hesaplanmış, asla değişmeyen statik ağırlıklar!
    private final List<BlendOffset> blendOffsets = new ArrayList<>();
    private double totalStaticWeight = 0;

    public BiomeBlender(CustomChunkGenerator chunkGenerator) {
        this.chunkGenerator = chunkGenerator;
        precalculateWeights();
    }

    // Matematik işlemi sunucu açılırken sadece 1 KERE yapılır!
    private void precalculateWeights() {
        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx += BLEND_STEP) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz += BLEND_STEP) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > RADIUS_SQUARED) continue;

                double weight = 1.0 - ((double) distanceSquared / RADIUS_SQUARED);
                weight = weight * weight;

                blendOffsets.add(new BlendOffset(dx, dz, weight));
                totalStaticWeight += weight;
            }
        }
    }

    public double getBlendedHeight(int realX, int realZ, SimplexOctaveGenerator heightNoise, BiomeCache cache) {
        Map<String, Double> biomeWeights = new HashMap<>();

        // 1. AŞAMA: Ağırlıkları Topla (Artık matematik veya noise çağrısı yok! Sadece diziden okuyoruz)
        for (BlendOffset offset : blendOffsets) {
            String bName = cache.getBiome(realX + offset.dx, realZ + offset.dz);
            biomeWeights.put(bName, biomeWeights.getOrDefault(bName, 0.0) + offset.weight);
        }

        double finalBlendedHeight = 0;
        double finalWaterLevel = 0;

        // 2. AŞAMA: Çapraz Birleştirme
        for (Map.Entry<String, Double> entry : biomeWeights.entrySet()) {
            String bName = entry.getKey();
            double weightRatio = entry.getValue() / totalStaticWeight;

            CustomChunkGenerator.TerrainData data = chunkGenerator.getTerrainData(bName);

            double noiseValue = heightNoise.noise(realX * data.roughness, realZ * data.roughness, 0.5, 0.5, true);
            double biomeHeight = data.baseHeight + (noiseValue * data.heightVariation);

            finalBlendedHeight += biomeHeight * weightRatio;
            finalWaterLevel += data.waterLevel * weightRatio;
        }

        // 3. AŞAMA: Teras Kırıcı ve Sahil Yumuşatması
        double microNoise = heightNoise.noise(realX * 0.15, realZ * 0.15, 0.5, 0.5, true);
        finalBlendedHeight += microNoise * 1.5;

        if (finalBlendedHeight < finalWaterLevel + 2) {
            double depthFromWater = finalWaterLevel - finalBlendedHeight;
            if (depthFromWater <= 0) {
                finalBlendedHeight = finalWaterLevel + Math.pow(finalBlendedHeight - finalWaterLevel, 0.5);
            } else {
                double oceanFloor = finalWaterLevel - (Math.sqrt(depthFromWater) * 4.5);
                finalBlendedHeight = Math.max(oceanFloor, 32);
            }
        }

        return finalBlendedHeight;
    }

    private static class BlendOffset {
        final int dx, dz;
        final double weight;
        BlendOffset(int dx, int dz, double weight) {
            this.dx = dx; this.dz = dz; this.weight = weight;
        }
    }

    // ==========================================================
    // YENİ MUHTEŞEM MOTOR: YEREL BİYOM MATRİSİ (LAZY GRID CACHE)
    // ==========================================================
    public static class BiomeCache {
        private final String[][] grid = new String[128][128]; // 1 Chunk ve tampon bölgesi için devasa harita
        private final int offsetX;
        private final int offsetZ;
        private final CustomBiomeProvider provider;

        public BiomeCache(int startX, int startZ, CustomBiomeProvider provider) {
            this.offsetX = startX - 50; // Negatif kordinat taşmalarını önlemek için merkez kaydırması
            this.offsetZ = startZ - 50;
            this.provider = provider;
        }

        public String getBiome(int x, int z) {
            int localX = x - offsetX;
            int localZ = z - offsetZ;

            if (localX >= 0 && localX < 128 && localZ >= 0 && localZ < 128) {
                // Eğer bu kordinat daha önce hesaplanmadıysa, hesapla ve RAM'e kaydet
                if (grid[localX][localZ] == null) {
                    grid[localX][localZ] = provider.getCustomBiomeName(x, z);
                }
                // Önceden hesaplanmışsa saniyesinde geri döndür!
                return grid[localX][localZ];
            }
            return provider.getCustomBiomeName(x, z);
        }
    }
}