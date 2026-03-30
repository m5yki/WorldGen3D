package me.maykitron.worldgen3d.shaper;

import me.maykitron.worldgen3d.generator.CustomChunkGenerator.TerrainData;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class CaveShaper {

    private final SimplexOctaveGenerator caveNoise;
    private final SimplexOctaveGenerator cave2Noise;

    public CaveShaper(SimplexOctaveGenerator caveNoise, SimplexOctaveGenerator cave2Noise) {
        this.caveNoise = caveNoise;
        this.cave2Noise = cave2Noise;
    }

    public double apply(int x, int y, int z, double density, TerrainData biome) {
        // DÜZELTME: Eski kod sadece "CUSTOM" modda çalışıyordu.
        // Artık "BOTH", "CHEESE", "SPAGHETTI" modları var; varsayılan "BOTH"
        String mode = biome.caveMode.toUpperCase();
        if (mode.equals("NONE") || mode.equals("DISABLED")) return density;

        // Mağaralar sadece belirli bir derinlik aralığında oluşur
        // Taban (minHeight) ve yüzey arasında, su seviyesinin 4 blok altından itibaren
        if (y > biome.waterLevel - 4) return density;
        if (y < -58) return density; // Bedrock katmanını koru

        // Y derinliğine göre mağara yoğunluğunu artır (derin = daha fazla mağara)
        double depthFactor = Math.min(1.0, (biome.waterLevel - 4 - y) / 60.0);

        double freq = biome.caveFreq; // YML'den gelir, varsayılan 0.045

        boolean doCheese    = mode.equals("BOTH") || mode.equals("CHEESE") || mode.equals("CUSTOM");
        boolean doSpaghetti = mode.equals("BOTH") || mode.equals("SPAGHETTI");

        // -------------------------------------------------------
        // 1. PEYNİR MAĞARALARI (Cheese Caves) — Büyük açık boşluklar
        // noise > threshold → boşluk
        // -------------------------------------------------------
        if (doCheese) {
            double n = caveNoise.noise(x * freq, y * freq * 1.2, z * freq, 0.5, 0.5, true);
            // Derinleştikçe eşik biraz düşer → daha fazla mağara
            double adjustedThreshold = biome.caveThreshold - (depthFactor * 0.08);
            if (n > adjustedThreshold) {
                density -= 60.0;
            }
        }

        // -------------------------------------------------------
        // 2. SPAGETTİ MAĞARALARI (Spaghetti Caves) — İnce tüneller
        // |noise| < küçük eşik → tünel
        // İki farklı noise katmanının farkını kullan → daha karmaşık ağ
        // -------------------------------------------------------
        if (doSpaghetti) {
            double sa = caveNoise.noise(x * freq * 0.7, y * freq * 0.9, z * freq * 0.7, 0.5, 0.5, true);
            double sb = cave2Noise.noise(x * freq * 0.7, y * freq * 0.9, z * freq * 0.7, 0.5, 0.5, true);

            // İki noise arasındaki dar kesişim = ince tünel
            double spagThreshold = biome.spaghettiThreshold; // varsayılan 0.025
            if (Math.abs(sa) < spagThreshold && Math.abs(sb) < spagThreshold * 1.5) {
                density -= 60.0;
            }

            // Yatay tüneller için ikinci katman
            double sc = cave2Noise.noise(x * freq * 0.5, y * freq * 0.3, z * freq * 0.5, 0.5, 0.5, true);
            if (Math.abs(sc) < spagThreshold * 0.8) {
                density -= 60.0;
            }
        }

        return density;
    }
}
