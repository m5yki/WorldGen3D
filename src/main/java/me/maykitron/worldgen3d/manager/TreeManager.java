package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeManager {
    private final WorldGen3D plugin;
    private final Map<String, TreeProfile> profiles = new HashMap<>();

    public TreeManager(WorldGen3D plugin) {
        this.plugin = plugin;
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        File folder = new File(plugin.getDataFolder(), "data/trees");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".yml")) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
                    String id = f.getName().replace(".yml", "");
                    profiles.put(id, new TreeProfile(config));
                }
            }
        }
    }

    public TreeProfile getProfile(String id) {
        return profiles.get(id);
    }

    public static class TreeProfile {
        public final boolean enabled;
        public final int chance;
        public final int yOffset;
        public final List<String> schematics;

        public TreeProfile(YamlConfiguration config) {
            this.enabled = config.getBoolean("enabled", false);
            this.chance = config.getInt("chance", 0);
            this.yOffset = config.getInt("y-offset", -1);
            this.schematics = config.getStringList("files");
        }
    }
}