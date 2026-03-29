package me.maykitron.worldgen3d.populator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.manager.OreManager;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class OrePopulator extends BlockPopulator {

    private final WorldGen3D plugin;
    private final CustomChunkGenerator chunkGenerator;
    private final CustomBiomeProvider biomeProvider;

    public OrePopulator(WorldGen3D plugin, CustomChunkGenerator chunkGenerator, CustomBiomeProvider biomeProvider) {
        this.plugin = plugin;
        this.chunkGenerator = chunkGenerator;
        this.biomeProvider = biomeProvider;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {

        int centerX = (chunkX * 16) + 8;
        int centerZ = (chunkZ * 16) + 8;
        String biomeName = biomeProvider.getCustomBiomeName(centerX, centerZ);
        CustomChunkGenerator.TerrainData tData = chunkGenerator.getTerrainData(biomeName);

        // YENİ: Modüler Maden Profilini Kontrol Et
        if (tData.oreProfile == null || tData.oreProfile.ores.isEmpty()) return;

        for (OreManager.OreSetting ore : tData.oreProfile.ores) {
            for (int i = 0; i < ore.chance; i++) {
                int y = ore.minHeight + random.nextInt(Math.max(1, ore.maxHeight - ore.minHeight));

                if (ore.type.equalsIgnoreCase("triangle")) {
                    double distanceToCenter = Math.abs(y - ore.centerHeight);
                    double maxDistance = Math.max(ore.maxHeight - ore.centerHeight, ore.centerHeight - ore.minHeight);
                    double probability = 1.0 - (distanceToCenter / maxDistance);
                    if (random.nextDouble() > probability) continue;
                }

                int startX = (chunkX * 16) + random.nextInt(16);
                int startZ = (chunkZ * 16) + random.nextInt(16);

                int currentVeinSize = ore.veinSizeMin;
                if (ore.veinSizeMax > ore.veinSizeMin) {
                    currentVeinSize = ore.veinSizeMin + random.nextInt((ore.veinSizeMax - ore.veinSizeMin) + 1);
                }

                for (int v = 0; v < currentVeinSize; v++) {
                    int x = startX + random.nextInt(3) - 1;
                    int vY = y + random.nextInt(3) - 1;
                    int z = startZ + random.nextInt(3) - 1;

                    if (limitedRegion.isInRegion(x, vY, z)) {
                        Material currentBlock = limitedRegion.getType(x, vY, z);

                        if (tData.deepBlock.contains(currentBlock) || currentBlock == Material.STONE || currentBlock == Material.DEEPSLATE) {

                            // Exposure (Açığa Çıkma) Kontrolü
                            if (ore.exposureChance < 100) {
                                if (isExposedToCave(limitedRegion, x, vY, z)) {
                                    if (random.nextInt(100) >= ore.exposureChance) {
                                        continue;
                                    }
                                }
                            }
                            limitedRegion.setType(x, vY, z, ore.block);
                        }
                    }
                }
            }
        }
    }

    private boolean isExposedToCave(LimitedRegion region, int x, int y, int z) {
        int[][] directions = { {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1} };
        for (int[] dir : directions) {
            int checkX = x + dir[0]; int checkY = y + dir[1]; int checkZ = z + dir[2];
            if (region.isInRegion(checkX, checkY, checkZ)) {
                Material neighbor = region.getType(checkX, checkY, checkZ);
                if (neighbor.isAir() || neighbor == Material.WATER || neighbor == Material.LAVA) return true;
            }
        }
        return false;
    }
}