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

    // Hafızaya yüklenen biyomların dosyalarını tutacağımız liste
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

        // Vanilla biyomları (o yazdığımız 8 dosyayı) dışarı çıkartıyoruz
        String[] defaultBiomes = {"plains.yml", "desert.yml", "forest.yml", "jungle.yml",
                "savanna.yml", "snowy_tundra.yml", "taiga.yml", "ocean.yml"};

        for (String biome : defaultBiomes) {
            File targetFile = new File(vanillaFolder, biome);
            if (!targetFile.exists()) {
                try {
                    plugin.saveResource("data/biomes/vanilla/" + biome, false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Vanilla biyom JAR icinde bulunamadi: " + biome);
                }
            }
        }
    }

    private void loadBiomesBasedOnMode() {
        // Config'den modu okuyoruz (VANILLA veya CUSTOM)
        this.currentMode = plugin.getConfig().getString("settings.generator-mode", "VANILLA").toUpperCase();
        File targetFolder = currentMode.equals("CUSTOM") ? customFolder : vanillaFolder;

        // Klasördeki tüm dosyaları bul
        File[] files = targetFolder.listFiles();

        if (files == null || files.length == 0) {
            plugin.getLogger().severe(plugin.getLangManager().getMessage("prefix") + "HATA: " + currentMode + " modunda hic biyom bulunamadi! Dunya olusturulamayacak.");
            return;
        }

        // Bulunan .yml dosyalarını hafızaya ekle
        for (File file : files) {
            if (file.getName().endsWith(".yml")) {
                loadedBiomes.add(file);
            }
        }

        // Konsola kaç biyom yüklendiğini afili bir şekilde yazdırıyoruz!
        plugin.getServer().getConsoleSender().sendMessage(
                plugin.getLangManager().getMessage("prefix") + "§aMotor Modu: §e" + currentMode + " §a| Yuklenen Biyom Sayisi: §e" + loadedBiomes.size()
        );
    }

    public List<File> getLoadedBiomes() {
        return loadedBiomes;
    }
}