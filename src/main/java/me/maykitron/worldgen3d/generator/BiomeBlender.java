package me.maykitron.worldgen3d.generator;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiomeBlender {

    private final CustomChunkGenerator chunkGenerator;

    // DÜZELTME: Radius 48'den 32'ye indirildi.
    // 48'lik radius, BiomeCache'in 128x128 sınırını aşıyordu → chunk seam'leri çıkıyordu.
    // 32'lik radius hem seam'siz hem de yumuşak geçiş sağlar.
    private static final int BLEND_RADIUS = 32;
    private static final int BLEND_STEP = 4;
    private static final int RADIUS_SQUARED = BLEND_RADIUS * BLEND_RADIUS;

    private final List<BlendOffset> blendOffsets = new ArrayList<>();
    private double totalStaticWeight = 0;

    public BiomeBlender(CustomChunkGenerator chunkGenerator) {
        this.chunkGenerator = chunkGenerator;
        precalculateWeights();
    }

    private void precalculateWeights() {
        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx += BLEND_STEP) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz += BLEND_STEP) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > RADIUS_SQUARED) continue;

                // Smooth step fonksiyonu — daha yumuşak geçiş için
                double t = 1.0 - ((double) distanceSquared / RADIUS_SQUARED);
                double weight = t * t * (3.0 - 2.0 * t); // smoothstep

                blendOffsets.add(new BlendOffset(dx, dz, weight));
                totalStaticWeight += weight;
            }
        }
    }

    /**
     * DÜZELTME: detailNoise parametresi eklendi.
     * Eski kod sadece heightNoise kullanıyordu, bu da arazi çok düz yapıyordu.
     * Şimdi iki katmanlı noise: ana form + ince detay.
     */
    public double getBlendedHeight(int realX, int realZ,
                                   SimplexOctaveGenerator heightNoise,
                                   SimplexOctaveGenerator detailNoise,
                                   BiomeCache cache) {
        Map<String, Double> biomeWeights = new HashMap<>();

        for (BlendOffset offset : blendOffsets) {
            String bName = cache.getBiome(realX + offset.dx, realZ + offset.dz);
            biomeWeights.put(bName, biomeWeights.getOrDefault(bName, 0.0) + offset.weight);
        }

        double finalBlendedHeight = 0;
        double finalWaterLevel = 0;

        for (Map.Entry<String, Double> entry : biomeWeights.entrySet()) {
            String bName = entry.getKey();
            double weightRatio = entry.getValue() / totalStaticWeight;

            CustomChunkGenerator.TerrainData data = chunkGenerator.getTerrainData(bName);

            // DÜZELTME: Gerçekçi arazi için FBM (Fractional Brownian Motion) yaklaşımı.
            // Ana gürültü — geniş dağ/vadi yapıları
            double n1 = heightNoise.noise(realX * data.roughness, realZ * data.roughness, 0.5, 0.5, true);
            // İkinci oktav — orta ölçek tepecikler (roughness * 2.5)
            double n2 = heightNoise.noise(realX * data.roughness * 2.5, realZ * data.roughness * 2.5, 0.5, 0.5, true) * 0.4;
            // Detay gürültüsü — küçük pürüzler
            double n3 = detailNoise.noise(realX * 0.08, realZ * 0.08, 0.5, 0.5, true) * 0.15;

            // Kombine noise [-1, 1] aralığında
            double combinedNoise = n1 + n2 + n3;

            // DÜZELTME: heightVariation artık YML'deki değeri tam kullanıyor (eskisi 0.5 ile çarpıyordu)
            double biomeHeight = data.baseHeight + (combinedNoise * data.heightVariation);

            finalBlendedHeight += biomeHeight * weightRatio;
            finalWaterLevel    += data.waterLevel * weightRatio;
        }

        // Okyanus geçişi yumuşatması
        if (finalBlendedHeight < finalWaterLevel + 2) {
            double depthFromWater = finalWaterLevel - finalBlendedHeight;
            if (depthFromWater <= 0) {
                finalBlendedHeight = finalWaterLevel + Math.pow(Math.max(0, finalBlendedHeight - finalWaterLevel), 0.5);
            } else {
                double oceanFloor = finalWaterLevel - (Math.sqrt(depthFromWater) * 5.0);
                finalBlendedHeight = Math.max(oceanFloor, 20);
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
    // BİYOM KABELİ (LAZY GRID CACHE)
    // DÜZELTME: Grid boyutu 128'den 192'ye çıkarıldı.
    // BLEND_RADIUS=32 ile chunk (16) + 2*radius(64) = 96 blok gerekir.
    // 128 bunu tam karşılamıyordu → seam. 192 güvenli tampon sağlar.
    // offsetX/Z merkezi doğru hizalandı.
    // ==========================================================
    public static class BiomeCache {
        private static final int GRID_SIZE = 192;
        private static final int HALF_GRID = GRID_SIZE / 2;

        private final String[][] grid = new String[GRID_SIZE][GRID_SIZE];
        private final int centerX;
        private final int centerZ;
        private final CustomBiomeProvider provider;

        public BiomeCache(int startX, int startZ, CustomBiomeProvider provider) {
            // Chunk merkezini al (startX = chunkX*16, merkez = startX + 8)
            this.centerX = startX + 8;
            this.centerZ = startZ + 8;
            this.provider = provider;
        }

        public String getBiome(int x, int z) {
            int localX = (x - centerX) + HALF_GRID;
            int localZ = (z - centerZ) + HALF_GRID;

            if (localX >= 0 && localX < GRID_SIZE && localZ >= 0 && localZ < GRID_SIZE) {
                if (grid[localX][localZ] == null) {
                    grid[localX][localZ] = provider.getCustomBiomeName(x, z);
                }
                return grid[localX][localZ];
            }
            // Grid dışına çıkılırsa direkt hesapla (blend radius küçük olduğu için nadir olur)
            return provider.getCustomBiomeName(x, z);
        }
    }
}
