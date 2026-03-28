package me.maykitron.worldgen3d.populator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * WorldGen3D - Yeni Nesil Maden Motoru (Dinamik Damar & Exposure Kontrolü)
 */
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

        if (!tData.rawConfig.contains("ores")) return;

        for (String key : tData.rawConfig.getConfigurationSection("ores").getKeys(false)) {
            String path = "ores." + key;
            Material oreBlock = matchMaterial(tData.rawConfig.getString(path + ".block", "STONE"));
            if (oreBlock == Material.AIR || oreBlock == Material.STONE) continue;

            String type = tData.rawConfig.getString(path + ".type", "uniform");
            int minHeight = tData.rawConfig.getInt(path + ".min-height", -64);
            int maxHeight = tData.rawConfig.getInt(path + ".max-height", 64);
            int centerHeight = tData.rawConfig.getInt(path + ".center-height", 0);

            // YENİ: Eski sistemle uyumlu kalması için fallback (vein-size) desteği de var
            int minVein = tData.rawConfig.getInt(path + ".vein-size-min", tData.rawConfig.getInt(path + ".vein-size", 4));
            int maxVein = tData.rawConfig.getInt(path + ".vein-size-max", minVein);

            // YENİ: Mağara duvarında çıkma şansı (Yazmıyorsa varsayılan %100)
            int exposureChance = tData.rawConfig.getInt(path + ".exposure-chance", 100);

            int chance = tData.rawConfig.getInt(path + ".chance", 10);

            for (int i = 0; i < chance; i++) {
                int y = minHeight + random.nextInt(Math.max(1, maxHeight - minHeight));

                // Yükseklik olasılık matematiği (Üçgensel dağılım)
                if (type.equalsIgnoreCase("triangle")) {
                    double distanceToCenter = Math.abs(y - centerHeight);
                    double maxDistance = Math.max(maxHeight - centerHeight, centerHeight - minHeight);
                    double probability = 1.0 - (distanceToCenter / maxDistance);

                    if (random.nextDouble() > probability) continue;
                }

                int startX = (chunkX * 16) + random.nextInt(16);
                int startZ = (chunkZ * 16) + random.nextInt(16);

                // YENİ: Bu damar için rastgele bir boyut seç
                int currentVeinSize = minVein;
                if (maxVein > minVein) {
                    currentVeinSize = minVein + random.nextInt((maxVein - minVein) + 1);
                }

                for (int v = 0; v < currentVeinSize; v++) {
                    int x = startX + random.nextInt(3) - 1;
                    int vY = y + random.nextInt(3) - 1;
                    int z = startZ + random.nextInt(3) - 1;

                    if (limitedRegion.isInRegion(x, vY, z)) {
                        Material currentBlock = limitedRegion.getType(x, vY, z);

                        if (tData.deepBlock.contains(currentBlock) || currentBlock == Material.STONE || currentBlock == Material.DEEPSLATE) {

                            // ==========================================================
                            // YENİ: EXPOSURE (AÇIĞA ÇIKMA) KONTROLÜ
                            // Eğer blok hava/su/lava temas ediyorsa ve şansımız tutmadıysa, es geç!
                            // ==========================================================
                            if (exposureChance < 100) {
                                if (isExposedToCave(limitedRegion, x, vY, z)) {
                                    if (random.nextInt(100) >= exposureChance) {
                                        continue; // Cevher koyma, taşı bırak!
                                    }
                                }
                            }

                            limitedRegion.setType(x, vY, z, oreBlock);
                        }
                    }
                }
            }
        }
    }

    /**
     * Bir bloğun etrafındaki 6 yöne bakarak havaya, suya veya lava açık olup olmadığını kontrol eder.
     */
    private boolean isExposedToCave(LimitedRegion region, int x, int y, int z) {
        int[][] directions = {
                {0, 1, 0}, {0, -1, 0}, // Üst, Alt
                {1, 0, 0}, {-1, 0, 0}, // Doğu, Batı
                {0, 0, 1}, {0, 0, -1}  // Güney, Kuzey
        };

        for (int[] dir : directions) {
            int checkX = x + dir[0];
            int checkY = y + dir[1];
            int checkZ = z + dir[2];

            if (region.isInRegion(checkX, checkY, checkZ)) {
                Material neighbor = region.getType(checkX, checkY, checkZ);
                if (neighbor.isAir() || neighbor == Material.WATER || neighbor == Material.LAVA) {
                    return true;
                }
            }
        }
        return false;
    }

    private Material matchMaterial(String materialName) {
        Material mat = Material.getMaterial(materialName.replace("minecraft:", "").trim().toUpperCase());
        return mat != null ? mat : Material.STONE;
    }
}