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
 * WorldGen3D - Yeni Nesil Maden Motoru (1.18+ Matematiği)
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
            int veinSize = tData.rawConfig.getInt(path + ".vein-size", 5);
            int chance = tData.rawConfig.getInt(path + ".chance", 10);

            for (int i = 0; i < chance; i++) {
                int y = minHeight + random.nextInt(Math.max(1, maxHeight - minHeight));

                if (type.equalsIgnoreCase("triangle")) {
                    double distanceToCenter = Math.abs(y - centerHeight);
                    double maxDistance = Math.max(maxHeight - centerHeight, centerHeight - minHeight);
                    double probability = 1.0 - (distanceToCenter / maxDistance);

                    if (random.nextDouble() > probability) continue;
                }

                int startX = (chunkX * 16) + random.nextInt(16);
                int startZ = (chunkZ * 16) + random.nextInt(16);

                for (int v = 0; v < veinSize; v++) {
                    int x = startX + random.nextInt(3) - 1;
                    int vY = y + random.nextInt(3) - 1;
                    int z = startZ + random.nextInt(3) - 1;

                    if (limitedRegion.isInRegion(x, vY, z)) {
                        Material currentBlock = limitedRegion.getType(x, vY, z);

                        // İŞTE ÇÖZÜLEN SATIR! deepBlock artık bir mozaik liste, o yüzden .contains() kullanıyoruz!
                        if (tData.deepBlock.contains(currentBlock) || currentBlock == Material.STONE || currentBlock == Material.DEEPSLATE) {
                            limitedRegion.setType(x, vY, z, oreBlock);
                        }
                    }
                }
            }
        }
    }

    private Material matchMaterial(String materialName) {
        Material mat = Material.getMaterial(materialName.replace("minecraft:", "").trim().toUpperCase());
        return mat != null ? mat : Material.STONE;
    }
}