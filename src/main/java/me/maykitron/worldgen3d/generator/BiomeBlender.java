package me.maykitron.worldgen3d.generator;

import org.bukkit.util.noise.SimplexOctaveGenerator;

public class BiomeBlender {

    private final CustomBiomeProvider biomeProvider;
    private final CustomChunkGenerator chunkGenerator;

    // ÇÖZÜM: Yarıçap 32 oldu! Geçişler artık çok daha yayvan ve pürüzsüz yamaçlar şeklinde olacak.
    private static final int BLEND_RADIUS = 32;
    private static final int BLEND_STEP = 4;

    public BiomeBlender(CustomBiomeProvider biomeProvider, CustomChunkGenerator chunkGenerator) {
        this.biomeProvider = biomeProvider;
        this.chunkGenerator = chunkGenerator;
    }

    public double getBlendedHeight(int realX, int realZ, SimplexOctaveGenerator heightNoise) {
        double blendedBaseHeight = 0;
        double blendedVariation = 0;
        double blendedRoughness = 0;
        double blendedWaterLevel = 0;
        double totalWeight = 0;

        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx += BLEND_STEP) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz += BLEND_STEP) {

                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > BLEND_RADIUS) continue;

                // Daha pürüzsüz (SmoothStep) bir ağırlık formülü
                double weight = Math.pow(1.0 - (distance / (double) BLEND_RADIUS), 2);

                String bName = biomeProvider.getCustomBiomeName(realX + dx, realZ + dz);
                CustomChunkGenerator.TerrainData data = chunkGenerator.getTerrainData(bName);

                blendedBaseHeight += data.baseHeight * weight;
                blendedVariation += data.heightVariation * weight;
                blendedRoughness += data.roughness * weight;
                blendedWaterLevel += data.waterLevel * weight;
                totalWeight += weight;
            }
        }

        blendedBaseHeight /= totalWeight;
        blendedVariation /= totalWeight;
        blendedRoughness /= totalWeight;
        blendedWaterLevel /= totalWeight;

        double noiseValue = heightNoise.noise(realX * blendedRoughness, realZ * blendedRoughness, 0.5, 0.5, true);
        double finalHeight = blendedBaseHeight + (noiseValue * blendedVariation);

        if (finalHeight < blendedWaterLevel) {
            double depth = blendedWaterLevel - finalHeight;
            double smoothedDepth = depth + (Math.pow(depth, 1.3) * 0.4);
            finalHeight = blendedWaterLevel - smoothedDepth;
        }

        return finalHeight;
    }
}