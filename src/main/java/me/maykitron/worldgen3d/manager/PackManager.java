package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackManager {

    private final WorldGen3D plugin;
    private final File vanillaFolder;
    private final File customFolder;
    private final List<File> loadedBiomes = new ArrayList<>();
    private String currentMode;

    public PackManager(WorldGen3D plugin) {
        this.plugin = plugin;
        this.vanillaFolder = new File(plugin.getDataFolder(), "data/biomes/vanilla");
        this.customFolder = new File(plugin.getDataFolder(), "data/biomes/custom");

        setupFolders();
        loadBiomesBasedOnMode();
    }

    private void setupFolders() {
        if (!vanillaFolder.exists()) vanillaFolder.mkdirs();
        if (!customFolder.exists()) customFolder.mkdirs();

        // YENİ: Maden ve Ağaç klasörleri de oluşturuluyor
        File oreFolder = new File(plugin.getDataFolder(), "data/ores");
        File treeFolder = new File(plugin.getDataFolder(), "data/trees");
        if (!oreFolder.exists()) oreFolder.mkdirs();
        if (!treeFolder.exists()) treeFolder.mkdirs();

        // 1. Vanilla Biyomları Çıkart
        String[] defaultBiomes = {
                "plains.yml", "desert.yml", "forest.yml", "jungle.yml",
                "savanna.yml", "snowy_tundra.yml", "taiga.yml", "ocean.yml",
                "beach.yml", "warm_ocean.yml", "frozen_ocean.yml, glacier.yml",
                "mountain.yml", "snowy_beach.yml", "swamp.yml, volcano.yml"
        };

        for (String biome : defaultBiomes) {
            File targetFile = new File(vanillaFolder, biome);
            if (!targetFile.exists()) {
                try {
                    plugin.saveResource("data/biomes/vanilla/" + biome, false);
                } catch (Exception ignored) {}
            }
        }

        // 2. Varsayılan Maden (Ore) ve Ağaç (Tree) Profillerini Çıkart
        File defaultOre = new File(oreFolder, "default_ores.yml");
        if (!defaultOre.exists()) {
            try { plugin.saveResource("data/ores/default_ores.yml", false); } catch (Exception ignored) {}
        }

        File defaultTree = new File(treeFolder, "forest_trees.yml");
        if (!defaultTree.exists()) {
            try { plugin.saveResource("data/trees/forest_trees.yml", false); } catch (Exception ignored) {}
        }
    }

    private void loadBiomesBasedOnMode() {
        this.currentMode = plugin.getConfig().getString("settings.generator-mode", "VANILLA").toUpperCase();
        File targetFolder = currentMode.equals("CUSTOM") ? customFolder : vanillaFolder;

        File[] files = targetFolder.listFiles();

        if (files == null || files.length == 0) {
            plugin.getLogger().severe(plugin.getLangManager().getMessage("prefix") + "HATA: " + currentMode + " modunda hic biyom bulunamadi!");
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(".yml")) {
                loadedBiomes.add(file);
            }
        }

        plugin.getServer().getConsoleSender().sendMessage(
                plugin.getLangManager().getMessage("prefix") + "§aMotor Modu: §e" + currentMode + " §a| Yuklenen Biyom Sayisi: §e" + loadedBiomes.size()
        );
    }

    public List<File> getLoadedBiomes() {
        return loadedBiomes;
    }
}