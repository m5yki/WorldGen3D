package me.maykitron.worldgen3d.context;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import java.util.Random;

public class BlockContext {
    public int x, y, z;
    public double baseHeight; // 3D gürültüden önceki ham yükseklik
    public double slope;      // Eğimi tutar (Dağlık alanlar için)
    public double density;    // O kordinattaki gerçek 3D yoğunluk

    public boolean isNearWater;
    public boolean isUnderground;
    public boolean isExposed; // Mağara duvarı veya yüzey havayla temas ediyor mu?

    public TerrainData biome;
    public Random random;
}