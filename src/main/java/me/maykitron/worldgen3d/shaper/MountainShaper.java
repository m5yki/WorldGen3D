package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class MountainShaper {
    private final SimplexOctaveGenerator mountainNoise;

    public MountainShaper(SimplexOctaveGenerator mountainNoise) {
        this.mountainNoise = mountainNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        if (biome.terrainType.equals("AMPLIFIED") || biome.terrainType.equals("JAGGED")) {
            // Yüksek rakımlarda çalışan 3D Dağ Gürültüsü
            double mNoise = mountainNoise.noise(x * 0.015, y * 0.015, z * 0.015, 0.5, 0.5, true);

            // Sadece taban yüksekliğinin üzerinde yükseliyorsak sivrileştir
            if (y > biome.baseHeight) {
                density += (mNoise * 20.0); // Yoğunluğa ekle = Katı bloklar göğe yükselsin
            }
        }
        return density;
    }
}