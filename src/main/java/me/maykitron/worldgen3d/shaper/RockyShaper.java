package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class RockyShaper {
    private final SimplexOctaveGenerator rockyNoise;

    public RockyShaper(SimplexOctaveGenerator rockyNoise) {
        this.rockyNoise = rockyNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        // Eğer biyom çok düzse (roughness düşükse) hiç kaya oluşturma
        if (biome.roughness < 0.012 && !biome.terrainType.equals("MOUNTAINOUS")) {
            return density;
        }

        // Yüksek frekanslı, çok keskin bir gürültü (3D kayalık yapısı için)
        double rNoise = rockyNoise.noise(x * 0.025, y * 0.035, z * 0.025, 0.5, 0.5, true);

        // Sadece yüzeye yakın yerlerde (density 0'a yakınken) çalışır.
        // Yerin çok altını (tamamen dolu) veya gökyüzünü (tamamen boş) etkilemez.
        if (Math.abs(density) < 12.0) {

            // Yoğunluğu rastgele artırıp azaltarak keskin kayalıklar/uçurumlar oluşturur
            double rockFactor = rNoise * 6.0;

            // Eğer dağlık bir alandaysak kayaları daha da belirginleştir
            if (biome.terrainType.equals("MOUNTAINOUS") || biome.terrainType.equals("ROCKY")) {
                rockFactor *= 1.5;
            }

            density += rockFactor;
        }

        return density;
    }
}