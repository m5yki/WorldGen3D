package me.maykitron.worldgen3d.generator;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomBiomeProvider extends BiomeProvider {

    private final WorldGen3D plugin;
    private final List<BiomeData> loadedBiomes = new ArrayList<>();
    private SimplexOctaveGenerator tempNoise;
    private SimplexOctaveGenerator humidNoise;
    private SimplexOctaveGenerator continentNoise;
    private boolean isInitialized = false;

    // YENİ: Şimşek hızında okuma için 100x100'lük önbellek haritası (Lookup Table)
    private final String[][] biomeLookupTable = new String[101][101];

    public CustomBiomeProvider(WorldGen3D plugin) {
        this.plugin = plugin;
        loadBiomes();
    }

    private void loadBiomes() {
        loadedBiomes.clear();
        for (File file : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("name", "Plains");
            double temp = config.getDouble("temperature", 0.5);
            double humid = config.getDouble("humidity", 0.5);
            loadedBiomes.add(new BiomeData(name, temp, humid));
        }
        if (loadedBiomes.isEmpty()) {
            loadedBiomes.add(new BiomeData("Plains", 0.5, 0.5));
        }

        // ==========================================================
        // YENİ: MATEMATİK DÖNGÜSÜNÜ ÇÖPE ATAN ÖNBELLEK SİSTEMİ
        // 0.0'dan 1.0'a kadar olan tüm sıcaklık/nem kombinasyonları için
        // en yakın kara biyomu sunucu açılışında SADECE 1 KERE hesaplanır!
        // ==========================================================
        for (int t = 0; t <= 100; t++) {
            for (int h = 0; h <= 100; h++) {
                double tempVal = t / 100.0;
                double humidVal = h / 100.0;

                BiomeData closest = null;
                double minDistance = Double.MAX_VALUE;

                for (BiomeData data : loadedBiomes) {
                    if (data.name.contains("Ocean") || data.name.contains("Beach")) continue; // Sular hariç

                    // Math.pow yerine doğrudan çarpım çok daha hızlıdır
                    double dist = ((tempVal - data.temp) * (tempVal - data.temp)) +
                            ((humidVal - data.humid) * (humidVal - data.humid));
                    if (dist < minDistance) {
                        minDistance = dist;
                        closest = data;
                    }
                }
                biomeLookupTable[t][h] = closest != null ? closest.name : "Plains";
            }
        }
    }

    private void init(WorldInfo worldInfo) {
        if (isInitialized) return;
        Random r = new Random(worldInfo.getSeed());
        tempNoise = new SimplexOctaveGenerator(r, 4);
        humidNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() + 1), 4);
        continentNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 12L), 4);
        isInitialized = true;
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        String customName = getCustomBiomeName(x, z).toLowerCase();

        // Minecraft Vanilla eşleştirmeleri (F3 menüsünde düzgün görünmesi için)
        if (customName.contains("warm_ocean")) return Biome.WARM_OCEAN;
        if (customName.contains("frozen_ocean")) return Biome.FROZEN_OCEAN;
        if (customName.contains("ocean")) return Biome.OCEAN;
        if (customName.contains("beach")) {
            return customName.contains("snowy") ? Biome.SNOWY_BEACH : Biome.BEACH;
        }
        if (customName.contains("jungle")) return Biome.JUNGLE;
        if (customName.contains("desert")) return Biome.DESERT;
        if (customName.contains("forest")) return Biome.FOREST;
        if (customName.contains("snow") || customName.contains("tundra")) return Biome.SNOWY_PLAINS;
        if (customName.contains("savanna")) return Biome.SAVANNA;
        if (customName.contains("taiga")) return Biome.TAIGA;
        return Biome.PLAINS;
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return new ArrayList<>(List.of(
                Biome.PLAINS, Biome.OCEAN, Biome.WARM_OCEAN, Biome.FROZEN_OCEAN,
                Biome.BEACH, Biome.SNOWY_BEACH, Biome.JUNGLE, Biome.DESERT,
                Biome.FOREST, Biome.SNOWY_PLAINS, Biome.SAVANNA, Biome.TAIGA
        ));
    }

    public String getCustomBiomeName(int x, int z) {
        if (tempNoise == null) return "Plains";

        // ==========================================================
        // 1. İKLİM DEĞERLERİNİ HESAPLA VE "ESNET" (Nadir biyomlar için)
        // 1.5 ile çarparak dalga boyunu genişletiyoruz, böylece Çöl (0.95) ve Tundra (0.05) çıkabiliyor!
        // ==========================================================
        double rawTemp = tempNoise.noise(x * 0.002, z * 0.002, 0.5, 0.5, true) * 1.5;
        double rawHumid = humidNoise.noise(x * 0.002, z * 0.002, 0.5, 0.5, true) * 1.5;

        // Değerleri 0.0 ile 1.0 arasına hapsediyoruz (Clamp)
        double temperature = Math.max(0.0, Math.min(1.0, (rawTemp + 1.0) / 2.0));
        double humidity = Math.max(0.0, Math.min(1.0, (rawHumid + 1.0) / 2.0));

        // ==========================================================
        // 2. SU VE SAHİL SİSTEMİ (Derinlik Bazlı)
        // ==========================================================
        double continent = continentNoise.noise(x * 0.001, z * 0.001, 0.5, 0.5, true);

        if (continent < -0.30) {
            // DERİN OKYANUSLAR
            if (temperature > 0.65) return "Warm_Ocean"; // İleride Warm_Deep_Ocean eklenebilir
            if (temperature < 0.35) return "Frozen_Ocean";
            return "Ocean";
        }
        else if (continent < -0.05) {
            // SIĞ OKYANUSLAR
            if (temperature > 0.65) return "Warm_Ocean";
            if (temperature < 0.35) return "Frozen_Ocean";
            return "Ocean";
        }
        else if (continent < 0.05) {
            // SAHİLLER (Kumsal geçişleri)
            return "Beach";
        }

        // ==========================================================
        // 3. KARALAR (Şimşek Hızında O(1) Okuma!)
        // ==========================================================
        int tIdx = (int) (temperature * 100);
        int hIdx = (int) (humidity * 100);

        // For döngüsü yok! Direkt tablodan çekiyoruz.
        return biomeLookupTable[tIdx][hIdx];
    }

    public void ensureInitialized(WorldInfo info) {
        init(info);
    }

    private static class BiomeData {
        String name;
        double temp;
        double humid;
        public BiomeData(String name, double temp, double humid) {
            this.name = name; this.temp = temp; this.humid = humid;
        }
    }
}