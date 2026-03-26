package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import java.io.File;

public class StructureManager {

    private final WorldGen3D plugin;

    public StructureManager(WorldGen3D plugin) {
        this.plugin = plugin;
        extractDefaultSchematics();
    }

    /**
     * Eklenti ilk kurulduğunda JAR'ın içindeki kilitli şematikleri
     * sunucunun plugins/WorldGen3D klasörüne çıkartır.
     */
    private void extractDefaultSchematics() {
        File schemFolder = new File(plugin.getDataFolder(), "data/schematics/vanilla");

        // Eğer klasör daha önce hiç oluşturulmadıysa (Yani eklenti yeni kurulduysa)
        if (!schemFolder.exists()) {
            schemFolder.mkdirs(); // Klasörleri yarat

            // JAR'ın içinden çıkartılacak varsayılan dosyaların listesi
            String[] defaultSchematics = {
                    "data/schematics/vanilla/forest_tree1.schematic",
                    "data/schematics/vanilla/forest_tree2.schematic",
                    "data/schematics/vanilla/forest_tree3.schematic",
                    "data/schematics/vanilla/forest_tree4.schematic"
            };

            for (String path : defaultSchematics) {
                try {
                    // false: Eğer kullanıcı dosyayı değiştirmişse, üstüne yazıp emeğini silme demek
                    plugin.saveResource(path, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Varsayilan sematik JAR icinde bulunamadi: " + path);
                }
            }
            plugin.getLogger().info("[WorldGen3D] Varsayilan orman sematikleri klasore basariyla kopyalandi!");
        }
    }

    /**
     * Populator'ların veya SchematicCommand'ın aradığı dosyayı döndürür.
     * @param path Örn: "vanilla/forest_tree1.schematic"
     */
    public File getSchematicFile(String path) {
        return new File(plugin.getDataFolder(), "data/schematics/" + path);
    }
}