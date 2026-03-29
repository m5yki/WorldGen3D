package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;

public class BlockPlacer {

    private final WorldGen3D plugin;

    public BlockPlacer(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    /**
     * Ana Terrain (3D Gürültü) Motoru için kullanılır.
     * Not: ChunkData aşamasında sadece Vanilla blokları konulabilir.
     */
    public void setChunkBlock(ChunkGenerator.ChunkData chunkData, int x, int y, int z, Material material) {
        chunkData.setBlock(x, y, z, material);
    }

    /**
     * Ağaç, Maden ve Çiçek Popülatörleri için kullanılır.
     * İŞTE BÜTÜN ÖZEL BLOK (ItemsAdder/Oraxen) BÜYÜSÜ BURADA GERÇEKLEŞECEK!
     */
    public void setRegionBlock(LimitedRegion region, int x, int y, int z, String blockId) {

        // 1. VANILLA BLOKLARI (Örn: "minecraft:stone" veya direkt "stone")
        if (blockId.startsWith("minecraft:") || !blockId.contains(":")) {
            String cleanName = blockId.replace("minecraft:", "").trim().toUpperCase();
            Material mat = Material.matchMaterial(cleanName);
            if (mat != null) {
                region.setType(x, y, z, mat);
            }
        }

        // 2. ITEMSADDER BLOKLARI (Örn: "itemsadder:custom_ruby_ore")
        else if (blockId.startsWith("itemsadder:")) {
            if (plugin.isItemsAdderHooked()) {
                String iaID = blockId.replace("itemsadder:", "").trim();
                // İleride ItemsAdder API'sini bağladığımızda buradaki yorum satırlarını kaldıracağız:
                // dev.lone.itemsadder.api.CustomBlock customBlock = dev.lone.itemsadder.api.CustomBlock.getInstance(iaID);
                // if (customBlock != null) customBlock.place(region, x, y, z);

                // Şimdilik API bağlı olmadığı için yer tutucu (placeholder) olarak Bedrock koyuyoruz ki çalıştığını görelim
                region.setType(x, y, z, Material.BEDROCK);
            }
        }

        // 3. ORAXEN BLOKLARI (Örn: "oraxen:magic_wood")
        else if (blockId.startsWith("oraxen:")) {
            // İleride Oraxen API'si bağlandığında burası çalışacak
            // io.th0rgal.oraxen.api.OraxenBlocks.place(blockId, location);
            region.setType(x, y, z, Material.BEDROCK);
        }
    }
}