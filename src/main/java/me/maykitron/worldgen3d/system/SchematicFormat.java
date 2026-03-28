package me.maykitron.worldgen3d.system;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

public class SchematicFormat implements StructureLoader {

    // EFSANEVİ RAM ÖNBELLEĞİ: 10 dakika dokunulmazsa RAM'den uçur, en fazla 50 şematik tut!
    private static final Cache<String, Clipboard> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();

    @SuppressWarnings({"deprecation", "removal"})
    @Override
    public void paste(LimitedRegion region, int x, int y, int z, File file) {

        // RAM'de varsa saniyesinde çek (getIfPresent)
        Clipboard clipboard = CACHE.getIfPresent(file.getName());

        // Yoksa diske inip oku ve RAM'e kaydet
        if (clipboard == null) {
            ClipboardFormat format = ClipboardFormats.findByAlias("schematic");
            if (format == null) format = ClipboardFormats.findByFile(file);
            if (format == null) return;

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
                CACHE.put(file.getName(), clipboard);
            } catch (Exception e) {
                System.out.println("[WorldGen3D] Schematic okunamadi: " + file.getName());
                return;
            }
        }

        if (clipboard == null) return;

        BlockVector3 origin = clipboard.getOrigin();

        for (int bX = clipboard.getMinimumPoint().getBlockX(); bX <= clipboard.getMaximumPoint().getBlockX(); bX++) {
            for (int bY = clipboard.getMinimumPoint().getBlockY(); bY <= clipboard.getMaximumPoint().getBlockY(); bY++) {
                for (int bZ = clipboard.getMinimumPoint().getBlockZ(); bZ <= clipboard.getMaximumPoint().getBlockZ(); bZ++) {
                    BaseBlock block = clipboard.getFullBlock(BlockVector3.at(bX, bY, bZ));
                    if (block.getMaterial().isAir()) continue;

                    int targetX = x + (bX - origin.getBlockX());
                    int targetY = y + (bY - origin.getBlockY());
                    int targetZ = z + (bZ - origin.getBlockZ());

                    if (region.isInRegion(targetX, targetY, targetZ)) {
                        try {
                            BlockData bd = Bukkit.createBlockData(block.getAsString());
                            region.setBlockData(targetX, targetY, targetZ, bd);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }
}