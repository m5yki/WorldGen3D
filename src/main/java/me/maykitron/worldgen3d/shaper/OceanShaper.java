package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;

public class OceanShaper {
    public double apply(int y, double density, TerrainData biome) {
        if (biome.name.contains("Ocean")) {
            // Sadece su seviyesinin altındayken çalışır
            if (y <= biome.waterLevel && y > biome.baseHeight) {
                // Suya yaklaştıkça sığ, derine indikçe çukurlaşan fizik formülü
                double depthRatio = (double) (biome.waterLevel - y) / (biome.waterLevel - biome.baseHeight);
                density -= Math.pow(depthRatio, 2.5) * 15.0; // Suyu çökertir!
            }
        }
        return density;
    }
}