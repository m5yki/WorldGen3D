package me.maykitron.worldgen3d.system;

import org.bukkit.generator.LimitedRegion;
import java.io.File;

/**
 * WorldGen3D - Şematik/Yapı Yükleyici Şablonu
 * NBT, Schem veya Schematic... Tüm formatlar bu şablona uymak zorundadır.
 */
public interface StructureLoader {

    // Yapıyı dünyaya güvenlice yapıştırma metodu
    void paste(LimitedRegion region, int x, int y, int z, File file);

}