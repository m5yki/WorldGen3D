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

                // Ağaçlar dip dibe girmesin diye her 5 blokta bir deneme yapıyoruz (Grid Sistemi)
                if (x % 5 != 0 || z % 5 != 0) continue;

                int realX = (chunkX * 16) + x;
                int realZ = (chunkZ * 16) + z;

                String biomeName = biomeProvider.getCustomBiomeName(realX, realZ);
                CustomChunkGenerator.TerrainData tData = chunkGenerator.getTerrainData(biomeName);

                if (!tData.treesEnabled) continue;

                int highestY = 319;
                while (highestY > worldInfo.getMinHeight() && limitedRegion.getType(realX, highestY, realZ).isAir()) {
                    highestY--;
                }
                Material highestBlock = limitedRegion.getType(realX, highestY, realZ);

                if (!tData.surfaceBlock.contains(highestBlock)) continue;

                if (tData.treeChance > 0 && random.nextInt(100) < tData.treeChance) {
                    if (tData.treeFiles.isEmpty()) continue;

                    String randomSchemPath = tData.treeFiles.get(random.nextInt(tData.treeFiles.size()));

                    // YENİ: YML'den gömülme/yükselme miktarını okuyoruz!
                    // YML'de yazmıyorsa ağaçlar için varsayılan olarak "-1" (toprağa göm) alacak.
                    // İleride yapılar için ayrı bir ayar çekeceğiz ve onu "0" veya "1" yapacağız.
                    int yOffset = tData.rawConfig.getInt("schematics.trees.y-offset", -1);

                    pasteWithSystem(limitedRegion, realX, highestY + yOffset, realZ, randomSchemPath);
                }
            }
        }
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