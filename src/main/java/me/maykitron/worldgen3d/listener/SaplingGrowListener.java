package me.maykitron.worldgen3d.listener;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class SaplingGrowListener implements Listener {

    private final WorldGen3D plugin;
    private final Random random = new Random();

    public SaplingGrowListener(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSaplingGrow(StructureGrowEvent event) {
        Location loc = event.getLocation();
        World world = loc.getWorld();

        // 1. Bu dünya bizim 3D motorumuzla mı üretilmiş kontrol et
        if (!(world.getGenerator() instanceof CustomChunkGenerator)) return;

        CustomChunkGenerator generator = (CustomChunkGenerator) world.getGenerator();
        CustomBiomeProvider biomeProvider = (CustomBiomeProvider) generator.getDefaultBiomeProvider(world);

        if (biomeProvider == null) return;

        // 2. Fidanın bulunduğu yerin "Gerçek" İklimini/Biyomunu bul
        String biomeName = biomeProvider.getCustomBiomeName(loc.getBlockX(), loc.getBlockZ());
        CustomChunkGenerator.TerrainData tData = generator.getTerrainData(biomeName);

        // 3. Eğer o biyomun ağaç profili yoksa veya kapalıysa Vanilla büyümesine izin ver
        if (tData.treeProfile == null || !tData.treeProfile.enabled || tData.treeProfile.schematics.isEmpty()) {
            return;
        }

        // ==========================================================
        // 4. VANILLA AĞACINI İPTAL ET VE KENDİ ŞEMATİĞİMİZİ KOY!
        // ==========================================================
        event.setCancelled(true); // Normal büyümeyi engelle
        loc.getBlock().setType(Material.AIR); // Yerdeki fidanı sil

        // YML'den rastgele bir ağaç seç
        String schemPath = tData.treeProfile.schematics.get(random.nextInt(tData.treeProfile.schematics.size()));
        File file = plugin.getStructureManager().getSchematicFile(schemPath);

        if (file == null || !file.exists()) {
            plugin.getLogger().warning("Fidan buyutulurken agac sematigi bulunamadi: " + schemPath);
            return;
        }

        // FAWE / WorldEdit API ile Şimşek Hızında Yapıştır
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return;

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {

                int pasteY = loc.getBlockY() + tData.treeProfile.yOffset;

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(loc.getBlockX(), pasteY, loc.getBlockZ()))
                        .ignoreAirBlocks(true) // Şematiğin etrafındaki boşluklar dünyayı kesmesin
                        .build();

                Operations.complete(operation);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Agac sematigi yapistirilirken hata olustu!");
            e.printStackTrace();
        }
    }
}