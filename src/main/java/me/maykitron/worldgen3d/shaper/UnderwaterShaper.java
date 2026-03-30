package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class UnderwaterShaper {
    private final SimplexOctaveGenerator underwaterNoise;

    public UnderwaterShaper(SimplexOctaveGenerator underwaterNoise) {
        this.underwaterNoise = underwaterNoise;
    }

    // YENİ: localWaterLevel parametresi ile tamamen bağımsız çalışır
    public double apply(int x, int y, int z, double density, int localWaterLevel, TerrainData biome) {
        // Eğer su seviyesinin üstündeysek (karadaysak) bu shaper'ı atla
        if (y > localWaterLevel) return density;

        // Derinlik hesaplaması (su yüzeyinden ne kadar aşağıdayız?)
        double depth = localWaterLevel - y;

        // Su altı için özel, düşük frekanslı yumuşak gürültü (kum tepeleri gibi)
        double uNoise = underwaterNoise.noise(x * 0.004, z * 0.004, 0.5, 0.5, true);

        // Su altındayken karadan gelen sert yoğunlukları eziyoruz
        // Bu sayede okyanus tabanı pürüzsüzleşir ve kendi şeklini alır
        if (biome.isWaterBiome) {
            // Tamamen su biyomuysa tabanı okyanus derinliğine çek
            density -= (depth * 0.5) + (uNoise * 4.0);
        } else {
            // Kara biyomunun içindeki bir göl veya nehir tabanıysa daha yumuşak oy
            if (depth < 10) { // Sığ sular
                density -= (uNoise * 2.0);
            }
        }

        return density;
    }
}