package me.maykitron.worldgen3d.populator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class FloraPopulator extends BlockPopulator {

    private final CustomChunkGenerator chunkGenerator;
    private final CustomBiomeProvider biomeProvider;

    public FloraPopulator(WorldGen3D plugin, CustomChunkGenerator chunkGenerator, CustomBiomeProvider biomeProvider) {
        this.chunkGenerator = chunkGenerator;
        this.biomeProvider = biomeProvider;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = (chunkX * 16) + x;
                int realZ = (chunkZ * 16) + z;

                String biomeName = biomeProvider.getCustomBiomeName(realX, realZ);
                CustomChunkGenerator.TerrainData tData = chunkGenerator.getTerrainData(biomeName);
                YamlConfiguration config = tData.rawConfig;

                int highestY = 319;
                while (highestY > worldInfo.getMinHeight() && limitedRegion.getType(realX, highestY, realZ).isAir()) {
                    highestY--;
                }
                Material highestBlock = limitedRegion.getType(realX, highestY, realZ);

                // ========================================================
                // SU ALTI BİYOÇEŞİTLİLİĞİ
                // ========================================================
                if (highestBlock == Material.WATER) {

                    double temp = config.getDouble("temperature", 0.5);
                    if (temp <= 0.15 && highestY == tData.waterLevel) {
                        limitedRegion.setType(realX, highestY, realZ, Material.ICE);
                        continue;
                    }

                    int floorY = highestY;
                    while (limitedRegion.getType(realX, floorY, realZ) == Material.WATER && floorY > worldInfo.getMinHeight()) { floorY--; }
                    Material floorBlock = limitedRegion.getType(realX, floorY, realZ);

                    if (floorBlock == Material.SAND || floorBlock == Material.GRAVEL || floorBlock == Material.DIRT) {
                        boolean oceanFloorEnabled = config.getBoolean("ocean-floor.enabled", false);
                        int oceanFloorChance = config.getInt("ocean-floor.chance", 0);
                        int kelpChance = config.getInt("flora.water-flora.kelp-chance", 0);
                        int seagrassChance = config.getInt("flora.water-flora.seagrass-chance", 0);

                        if (oceanFloorEnabled && random.nextInt(100) < oceanFloorChance) {
                            List<String> coralBlocks = config.getStringList("ocean-floor.blocks");
                            if (!coralBlocks.isEmpty()) {
                                limitedRegion.setType(realX, floorY + 1, realZ, matchMaterial(coralBlocks.get(random.nextInt(coralBlocks.size()))));
                            }
                        } else if (kelpChance > 0 && random.nextInt(100) < kelpChance) {
                            int kelpTop = highestY - 1 - random.nextInt(3);
                            if (kelpTop > floorY + 1) {
                                for (int ky = floorY + 1; ky < kelpTop; ky++) {
                                    limitedRegion.setType(realX, ky, realZ, Material.KELP_PLANT);
                                }
                                limitedRegion.setType(realX, kelpTop, realZ, Material.KELP);
                            }
                        } else if (seagrassChance > 0 && random.nextInt(100) < seagrassChance) {
                            limitedRegion.setType(realX, floorY + 1, realZ, Material.SEAGRASS);
                        }
                    }
                    continue;
                }

                // ========================================================
                // KAR VE SAHİL
                // ========================================================
                double temp = config.getDouble("temperature", 0.5);
                if (temp <= 0.25 && highestBlock.isSolid() && highestBlock != Material.ICE) {
                    limitedRegion.setType(realX, highestY + 1, realZ, (highestY > 100 && random.nextBoolean()) ? Material.SNOW_BLOCK : Material.SNOW);
                    continue;
                }

                int sugarcaneChance = config.getInt("flora.coastal-flora.sugarcane-chance", 0);
                if (sugarcaneChance > 0 && (highestBlock == Material.SAND || highestBlock == Material.DIRT || highestBlock == Material.GRASS_BLOCK)) {
                    boolean nextToWater = (limitedRegion.getType(realX + 1, highestY - 1, realZ) == Material.WATER ||
                            limitedRegion.getType(realX - 1, highestY - 1, realZ) == Material.WATER ||
                            limitedRegion.getType(realX, highestY - 1, realZ + 1) == Material.WATER ||
                            limitedRegion.getType(realX, highestY - 1, realZ - 1) == Material.WATER);

                    if (nextToWater && random.nextInt(100) < sugarcaneChance) {
                        int caneHeight = 1 + random.nextInt(3);
                        for (int i = 0; i < caneHeight; i++) limitedRegion.setType(realX, highestY + 1 + i, realZ, Material.SUGAR_CANE);
                        continue;
                    }
                }

                // ========================================================
                // ÇİÇEKLER VE OTLAR
                // ========================================================
                if (highestBlock != Material.SAND && highestBlock != Material.SNOW_BLOCK) {
                    int grassChance = config.getInt("flora.grass-chance", 0);
                    int flowerChance = config.getInt("flora.flowers.chance", 0);

                    if (grassChance > 0 && random.nextInt(100) < grassChance) {
                        limitedRegion.setType(realX, highestY + 1, realZ, Material.SHORT_GRASS);
                        continue;
                    }
                    if (flowerChance > 0 && random.nextInt(100) < flowerChance) {
                        List<String> flowerTypes = config.getStringList("flora.flowers.types");
                        if (!flowerTypes.isEmpty()) {
                            limitedRegion.setType(realX, highestY + 1, realZ, matchMaterial(flowerTypes.get(random.nextInt(flowerTypes.size()))));
                        }
                    }
                }
            }
        }
    }

    private Material matchMaterial(String materialName) {
        Material mat = Material.getMaterial(materialName.replace("minecraft:", "").trim().toUpperCase());
        return mat != null ? mat : Material.AIR;
    }
}