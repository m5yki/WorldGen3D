package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class MountainShaper {

    private final SimplexOctaveGenerator mountainNoise;

    public MountainShaper(SimplexOctaveGenerator mountainNoise) {
        this.mountainNoise = mountainNoise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        String type = biome.terrainType;

        if (type.equals("AMPLIFIED") || type.equals("JAGGED") || type.equals("MOUNTAIN")) {

            // DÜZELTME: Eski kod sadece y > baseHeight'ta çalışıyordu ve katkısı küçüktü.
            // Şimdi hem yükseklerde hem alçakta (vadi etkisi) çalışan tam bir dağ sistemi.

            // Düşük frekans: büyük dağ silsileleri
            double mLow = mountainNoise.noise(x * 0.008, y * 0.004, z * 0.008, 0.5, 0.5, true);
            // Orta frekans: kayalık yüzeyler
            double mMid = mountainNoise.noise(x * 0.025, y * 0.015, z * 0.025, 0.5, 0.5, true) * 0.45;
            // Yüksek frekans: sivri tepeler
            double mHigh = mountainNoise.noise(x * 0.06, y * 0.035, z * 0.06, 0.5, 0.5, true) * 0.2;

            double mNoise = mLow + mMid + mHigh; // [-1.65, 1.65] aralığında

            if (type.equals("JAGGED")) {
                // JAGGED: Mutlak değer ile sivri, sarp tepeler (vanilla "Jagged Peaks" gibi)
                mNoise = Math.abs(mNoise) * 1.4 - 0.5;
            }

            // Yüksekliğe göre kademeli güçlendirme
            // Taban yüksekliğinin altında → vadi etkisi (negatif)
            // Taban yüksekliğinin üstünde → zirve etkisi (pozitif)
            double yFactor;
            if (y > biome.baseHeight) {
                // Daha yükseğe çıktıkça katkı güçlenir
                yFactor = 1.0 + ((y - biome.baseHeight) / 40.0);
            } else {
                // Alçakta vadi oluşturmak için hafif negatif katkı
                yFactor = 0.3;
            }

            density += (mNoise * 28.0 * yFactor);
        }

        return density;
    }
}
