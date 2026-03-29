package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreManager {

    private final WorldGen3D plugin;
    // Tüm maden profillerini hafızada (RAM) tuttuğumuz O(1) hızındaki liste
    private final Map<String, OreProfile> profiles = new HashMap<>();

    public OreManager(WorldGen3D plugin) {
        this.plugin = plugin;
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        File folder = new File(plugin.getDataFolder(), "data/ores");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".yml")) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
                    String id = f.getName().replace(".yml", "");
                    profiles.put(id, new OreProfile(config));
                }
            }
        }
        plugin.getLogger().info("Yuklenen Maden Profili (Ore Profile) Sayisi: " + profiles.size());
    }

    // Ana motorun (CustomChunkGenerator) saniyesinde profil sorması için metod
    public OreProfile getProfile(String id) {
        return profiles.get(id);
    }

    // ==========================================================
    // MADEN PROFİLİ (YML'den okunan verilerin objesi)
    // ==========================================================
    public static class OreProfile {
        public final List<OreSetting> ores = new ArrayList<>();

        public OreProfile(YamlConfiguration config) {
            if (!config.contains("ores")) return;

            for (String key : config.getConfigurationSection("ores").getKeys(false)) {
                String path = "ores." + key;

                // BUKKIT KORUMASI: 'minecraft:' yazısını silerek bloğu doğru bulmasını sağlıyoruz!
                String rawName = config.getString(path + ".block", "STONE");
                String cleanName = rawName.replace("minecraft:", "").trim().toUpperCase();
                Material mat = Material.matchMaterial(cleanName);

                if (mat == null) {
                    System.out.println("[WorldGen3D] UYARI: Maden blogu bulunamadi -> " + rawName);
                    continue; // Blok yanlış yazılmışsa çökme, sıradakine geç!
                }

                OreSetting setting = new OreSetting();
                setting.block = mat;
                setting.type = config.getString(path + ".type", "uniform");
                setting.minHeight = config.getInt(path + ".min-height", -64);
                setting.maxHeight = config.getInt(path + ".max-height", 64);
                setting.centerHeight = config.getInt(path + ".center-height", 0);

                // Dinamik boyut (Eski 'vein-size' ayarıyla geriye dönük uyumlu)
                setting.veinSizeMin = config.getInt(path + ".vein-size-min", config.getInt(path + ".vein-size", 4));
                setting.veinSizeMax = config.getInt(path + ".vein-size-max", setting.veinSizeMin);

                // Açığa çıkma (Mağara duvarında parlama) şansı
                setting.exposureChance = config.getInt(path + ".exposure-chance", 100);
                setting.chance = config.getInt(path + ".chance", 10);

                ores.add(setting);
            }
        }
    }

    // ==========================================================
    // TEK BİR MADENİN AYARLARI
    // ==========================================================
    public static class OreSetting {
        public Material block;
        public String type;
        public int minHeight, maxHeight, centerHeight;
        public int veinSizeMin, veinSizeMax;
        public int exposureChance, chance;
    }
}