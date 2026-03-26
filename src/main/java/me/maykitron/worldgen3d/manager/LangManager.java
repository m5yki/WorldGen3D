package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LangManager {

    private final WorldGen3D plugin;
    private FileConfiguration langConfig;
    private String currentLang;

    public LangManager(WorldGen3D plugin) {
        this.plugin = plugin;
        setupLangSystem();
    }

    private void setupLangSystem() {
        // 1. Config'den seçili dili al (Eğer ayar yoksa varsayılan 'tr' olsun)
        this.currentLang = plugin.getConfig().getString("settings.language", "tr");

        // 2. Sunucuda 'plugins/WorldGen3D/lang' klasörünü oluştur
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // 3. Kullanılacak dil dosyasının yolu
        File langFile = new File(langFolder, currentLang + ".yml");

        // 4. Eğer dil dosyası dışarıda yoksa, içinden (resources/lang) dışarı çıkart
        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang/" + currentLang + ".yml", false);
            } catch (IllegalArgumentException e) {
                // Eğer adam config'e saçma sapan bir dil (örn: fr) yazdıysa ve bizde yoksa:
                plugin.getLogger().warning("Dil dosyasi bulunamadi: " + currentLang + ".yml! Varsayilan (en) yukleniyor.");
                currentLang = "en";
                langFile = new File(langFolder, "en.yml");
                if (!langFile.exists()) {
                    plugin.saveResource("lang/en.yml", false);
                }
            }
        }

        // 5. YML Dosyasını hafızaya yükle
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * İstenen mesajı YML dosyasından çeker ve renk kodlarını ayarlar.
     */
    public String getMessage(String path) {
        String message = langConfig.getString(path);

        // Eğer YML içinde o mesaj silinmişse/yoksa kod çökmesin diye:
        if (message == null) {
            return ChatColor.RED + "[WorldGen3D] Mesaj eksik: " + path;
        }

        // '&' işaretlerini Bukkit'in anlayacağı renk kodlarına çevir
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}