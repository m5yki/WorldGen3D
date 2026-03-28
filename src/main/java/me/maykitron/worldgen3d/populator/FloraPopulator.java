package me.maykitron.worldgen3d.populator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

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

                // YENİ: Biyomun iklim değerlerini YML'den dinamik olarak çekiyoruz
                double temperature = tData.rawConfig.getDouble("temperature", 0.5);

                int highestY = 319;
                while (highestY > worldInfo.getMinHeight() && limitedRegion.getType(realX, highestY, realZ).isAir()) {
                    highestY--;
                }
                Material highestBlock = limitedRegion.getType(realX, highestY, realZ);

                // ========================================================
                // 1. SU ALTI BİYOÇEŞİTLİLİĞİ VE BUZLANMA (İKLİM KONTROLLÜ)
                // ========================================================
                if (highestBlock == Material.WATER) {

                    // Soğuk bir bölgedeysek (Buzlu okyanus veya Tundra sınırı) suların yüzeyini dondur!
                    if (temperature <= 0.15 && highestY == tData.waterLevel) {
                        limitedRegion.setType(realX, highestY, realZ, Material.ICE);
                        continue; // Yüzey donduysa alta bitki koyma ihtimalini düşürüyoruz
                    }

                    int floorY = highestY;
                    while (limitedRegion.getType(realX, floorY, realZ) == Material.WATER && floorY > worldInfo.getMinHeight()) {
                        floorY--;
                    }

                    Material floorBlock = limitedRegion.getType(realX, floorY, realZ);
                    if (floorBlock == Material.SAND || floorBlock == Material.GRAVEL || floorBlock == Material.DIRT) {

                        if (tData.oceanFloorEnabled && random.nextInt(100) < tData.oceanFloorChance) {
                            if (!tData.oceanFloorBlocks.isEmpty()) {
                                limitedRegion.setType(realX, floorY + 1, realZ, matchMaterial(tData.oceanFloorBlocks.get(random.nextInt(tData.oceanFloorBlocks.size()))));
                            }
                        } else if (tData.kelpChance > 0 && random.nextInt(100) < tData.kelpChance) {
                            int kelpTop = highestY - 1 - random.nextInt(3);
                            if (kelpTop > floorY + 1) {
                                for (int ky = floorY + 1; ky < kelpTop; ky++) {
                                    limitedRegion.setType(realX, ky, realZ, Material.KELP_PLANT);
                                }
                                limitedRegion.setType(realX, kelpTop, realZ, Material.KELP);
                            }
                        } else if (tData.seagrassChance > 0 && random.nextInt(100) < tData.seagrassChance) {
                            limitedRegion.setType(realX, floorY + 1, realZ, Material.SEAGRASS);
                        }
                    }
                    continue;
                }

                // ========================================================
                // 2. İKLİM BAZLI KAR YAĞIŞI (Kar Kaplaması)
                // ========================================================
                // Eğer sıcaklık 0.25 ve altındaysa, yaprakların, toprağın veya kumun üzerine kar yağdır!
                if (temperature <= 0.25 && highestBlock.isSolid() && highestBlock != Material.ICE) {
                    // Yüksek dağ zirvelerindeyse (Örn: Y > 100) kar kalınlığını rastgele artır
                    if (highestY > 100 && random.nextBoolean()) {
                        limitedRegion.setType(realX, highestY + 1, realZ, Material.SNOW_BLOCK);
                    } else {
                        limitedRegion.setType(realX, highestY + 1, realZ, Material.SNOW); // İnce kar katmanı
                    }
                    continue; // Yer karla kaplandıysa ot veya çiçek çıkmasına gerek yok
                }

                // ========================================================
                // 3. SAHİL KENARI (Şeker Kamışları)
                // ========================================================
                if (tData.sugarcaneChance > 0 && (highestBlock == Material.SAND || highestBlock == Material.DIRT || highestBlock == Material.GRASS_BLOCK)) {
                    boolean nextToWater = false;
                    if (limitedRegion.getType(realX + 1, highestY - 1, realZ) == Material.WATER ||
                            limitedRegion.getType(realX - 1, highestY - 1, realZ) == Material.WATER ||
                            limitedRegion.getType(realX, highestY - 1, realZ + 1) == Material.WATER ||
                            limitedRegion.getType(realX, highestY - 1, realZ - 1) == Material.WATER) {
                        nextToWater = true;
                    }

                    if (nextToWater && random.nextInt(100) < tData.sugarcaneChance) {
                        int caneHeight = 1 + random.nextInt(3);
                        for (int i = 0; i < caneHeight; i++) {
                            limitedRegion.setType(realX, highestY + 1 + i, realZ, Material.SUGAR_CANE);
                        }
                        continue;
                    }
                }

                // ========================================================
                // 4. KARA BİTKİLERİ (Çiçekler ve Otlar)
                // ========================================================
                if (tData.surfaceBlock.contains(highestBlock) && highestBlock != Material.SAND && highestBlock != Material.SNOW_BLOCK) {
                    if (tData.grassChance > 0 && random.nextInt(100) < tData.grassChance) {
                        limitedRegion.setType(realX, highestY + 1, realZ, Material.SHORT_GRASS);
                        continue;
                    }
                    if (tData.flowerChance > 0 && random.nextInt(100) < tData.flowerChance) {
                        if (!tData.flowerTypes.isEmpty()) {
                            limitedRegion.setType(realX, highestY + 1, realZ, matchMaterial(tData.flowerTypes.get(random.nextInt(tData.flowerTypes.size()))));
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