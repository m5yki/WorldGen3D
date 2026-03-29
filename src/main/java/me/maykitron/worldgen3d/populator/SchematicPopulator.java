package me.maykitron.worldgen3d.populator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.system.NbtFormat;
import me.maykitron.worldgen3d.system.SchemFormat;
import me.maykitron.worldgen3d.system.SchematicFormat;
import me.maykitron.worldgen3d.system.StructureLoader;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Random;

public class SchematicPopulator extends BlockPopulator {

    private final WorldGen3D plugin;
    private final CustomChunkGenerator chunkGenerator;
    private final CustomBiomeProvider biomeProvider;

    public SchematicPopulator(WorldGen3D plugin, CustomChunkGenerator chunkGenerator, CustomBiomeProvider biomeProvider) {
        this.plugin = plugin;
        this.chunkGenerator = chunkGenerator;
        this.biomeProvider = biomeProvider;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                // Ağaçlar dip dibe girmesin diye Grid Sistemi
                if (x % 5 != 0 || z % 5 != 0) continue;

                int realX = (chunkX * 16) + x;
                int realZ = (chunkZ * 16) + z;

                String biomeName = biomeProvider.getCustomBiomeName(realX, realZ);
                CustomChunkGenerator.TerrainData tData = chunkGenerator.getTerrainData(biomeName);

                // YENİ: Ağaç profilini kontrol et
                if (tData.treeProfile == null || !tData.treeProfile.enabled || tData.treeProfile.schematics.isEmpty()) continue;

                // Kusursuz Oturtma Sistemi (Grounding)
                int centerHighestY = getHighestBlock(worldInfo, limitedRegion, realX, realZ);

                int h1 = getHighestBlock(worldInfo, limitedRegion, realX + 2, realZ);
                int h2 = getHighestBlock(worldInfo, limitedRegion, realX - 2, realZ);
                int h3 = getHighestBlock(worldInfo, limitedRegion, realX, realZ + 2);
                int h4 = getHighestBlock(worldInfo, limitedRegion, realX, realZ - 2);

                int safeGroundY = Math.min(centerHighestY, Math.min(Math.min(h1, h2), Math.min(h3, h4)));

                Material surfaceMat = limitedRegion.getType(realX, centerHighestY, realZ);

                // Zemin kontrolünü YML'in ham verisinden okuyarak yapıyoruz
                String surfaceBlockName = tData.rawConfig.getString("blocks.surface", "minecraft:grass_block");
                if (!surfaceBlockName.contains(surfaceMat.name().toLowerCase())) {
                    if (surfaceMat != Material.GRASS_BLOCK && surfaceMat != Material.DIRT && surfaceMat != Material.PODZOL && surfaceMat != Material.COARSE_DIRT && surfaceMat != Material.SAND) {
                        continue;
                    }
                }

                if (tData.treeProfile.chance > 0 && random.nextInt(100) < tData.treeProfile.chance) {
                    String randomSchemPath = tData.treeProfile.schematics.get(random.nextInt(tData.treeProfile.schematics.size()));

                    pasteWithSystem(limitedRegion, realX, safeGroundY + tData.treeProfile.yOffset, realZ, randomSchemPath);
                }
            }
        }
    }

    private int getHighestBlock(WorldInfo worldInfo, LimitedRegion region, int x, int z) {
        int y = 319;
        if (!region.isInRegion(x, y, z)) return worldInfo.getMinHeight();
        while (y > worldInfo.getMinHeight() && region.getType(x, y, z).isAir()) { y--; }
        return y;
    }

    private void pasteWithSystem(LimitedRegion region, int x, int y, int z, String schematicPath) {
        File file = plugin.getStructureManager().getSchematicFile(schematicPath);
        if (file == null || !file.exists()) return;

        String fileName = file.getName().toLowerCase();
        StructureLoader loader;

        if (fileName.endsWith(".nbt")) { loader = new NbtFormat(); }
        else if (fileName.endsWith(".schem")) { loader = new SchemFormat(); }
        else if (fileName.endsWith(".schematic")) { loader = new SchematicFormat(); }
        else { return; }

        loader.paste(region, x, y, z, file);
    }
}