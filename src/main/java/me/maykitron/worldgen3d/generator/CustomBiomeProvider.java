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
    private SimplexOctaveGenerator elevationNoise;
    private boolean isInitialized = false;

    // 101×101 lookup tablosu: [sıcaklık indeksi][nem indeksi] → biyom adı
    // Sunucu açılışında 1 kere hesaplanır, sonra O(1) okuma.
    private final String[][] biomeLookupTable = new String[101][101];

    // ==========================================================
    // KIYI TAMPONU SABİTLERİ
    // continent bu aralıktaysa → kara ama kıyıya yakın
    // Bu bantta sıcaklık ılımanlaştırılır: Çöl ve Tundra kıyıya çıkamaz.
    // ==========================================================
    private static final double COAST_INNER = 0.0;   // plajın bittiği yer
    private static final double COAST_OUTER = 0.18;  // tamponun bittiği yer

    // Tampon içinde sıcaklığın kısıtlanacağı aralık
    private static final double COAST_TEMP_MIN = 0.28;
    private static final double COAST_TEMP_MAX = 0.72;

    public CustomBiomeProvider(WorldGen3D plugin) {
        this.plugin = plugin;
        loadBiomes();
    }

    // ----------------------------------------------------------
    // BIYOM YÜKLEMESİ & LOOKUP TABLO
    // ----------------------------------------------------------
    private void loadBiomes() {
        loadedBiomes.clear();

        for (File file : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name  = config.getString("name",        "Plains");
            double temp  = config.getDouble("temperature", 0.5);
            double humid = config.getDouble("humidity",    0.5);
            loadedBiomes.add(new BiomeData(name, temp, humid));
        }

        if (loadedBiomes.isEmpty()) {
            loadedBiomes.add(new BiomeData("Plains", 0.5, 0.5));
        }

        // Lookup tablosunu doldur.
        // Okyanus, plaj, yanardağ ve dağ biyomları kara matrisine girmez —
        // bunlar continent/elevation noise ile ayrıca belirlenir.
        for (int t = 0; t <= 100; t++) {
            for (int h = 0; h <= 100; h++) {
                double tempVal  = t / 100.0;
                double humidVal = h / 100.0;

                BiomeData closest  = null;
                double    minDist  = Double.MAX_VALUE;

                for (BiomeData data : loadedBiomes) {
                    if (isExcludedFromMatrix(data.name)) continue;

                    double dist = sq(tempVal - data.temp) + sq(humidVal - data.humid);
                    if (dist < minDist) {
                        minDist  = dist;
                        closest  = data;
                    }
                }
                biomeLookupTable[t][h] = (closest != null) ? closest.name : "Plains";
            }
        }
    }

    /**
     * Kara sıcaklık/nem matrisine GİRMEYECEK biyomlar.
     * Bunların yerleşimi continent veya elevation noise tarafından kontrol edilir.
     */
    private boolean isExcludedFromMatrix(String name) {
        String lower = name.toLowerCase();
        return lower.contains("ocean")
            || lower.contains("beach")
            || lower.contains("mountain")
            || lower.contains("volcano");
    }

    // ----------------------------------------------------------
    // NOISE BAŞLATMA
    // ----------------------------------------------------------
    private void init(WorldInfo worldInfo) {
        if (isInitialized) return;
        long seed = worldInfo.getSeed();

        // Her noise farklı seed ile — aralarında korelasyon olmasın
        tempNoise      = new SimplexOctaveGenerator(new Random(seed),            4);
        humidNoise     = new SimplexOctaveGenerator(new Random(seed + 1L),       4);
        continentNoise = new SimplexOctaveGenerator(new Random(seed * 12L),      4);
        elevationNoise = new SimplexOctaveGenerator(new Random(seed * 17L + 5),  4);

        isInitialized = true;
    }

    // ----------------------------------------------------------
    // VANILLA BİYOM EŞLEŞTİRMESİ (F3 menüsü için)
    // ----------------------------------------------------------
    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        init(worldInfo);
        String name = getCustomBiomeName(x, z).toLowerCase();

        if (name.contains("warm_ocean"))                      return Biome.WARM_OCEAN;
        if (name.contains("frozen_ocean"))                    return Biome.FROZEN_OCEAN;
        if (name.contains("ocean"))                           return Biome.OCEAN;
        if (name.contains("snowy_beach"))                     return Biome.SNOWY_BEACH;
        if (name.contains("beach"))                           return Biome.BEACH;
        if (name.contains("volcano"))                         return Biome.BASALT_DELTAS;
        if (name.contains("mountain") || name.contains("glacier")) return Biome.JAGGED_PEAKS;
        if (name.contains("jungle"))                          return Biome.JUNGLE;
        if (name.contains("swamp"))                           return Biome.SWAMP;
        if (name.contains("desert"))                          return Biome.DESERT;
        if (name.contains("savanna"))                         return Biome.SAVANNA;
        if (name.contains("taiga"))                           return Biome.TAIGA;
        if (name.contains("forest"))                          return Biome.FOREST;
        if (name.contains("snow") || name.contains("tundra")) return Biome.SNOWY_PLAINS;
        return Biome.PLAINS;
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return new ArrayList<>(List.of(
                Biome.PLAINS, Biome.FOREST, Biome.TAIGA, Biome.DESERT,
                Biome.JUNGLE, Biome.SWAMP, Biome.SNOWY_PLAINS, Biome.SAVANNA,
                Biome.JAGGED_PEAKS, Biome.BASALT_DELTAS,
                Biome.OCEAN, Biome.WARM_OCEAN, Biome.FROZEN_OCEAN,
                Biome.BEACH, Biome.SNOWY_BEACH
        ));
    }

    // ----------------------------------------------------------
    // ANA BİYOM BELİRLEME MANTIĞI
    // ----------------------------------------------------------
    public String getCustomBiomeName(int x, int z) {
        if (tempNoise == null) return "Plains";

        // ======================================================
        // ADIM 1 — KONTİNAN NOISE
        // Haritanın kara/okyanus/kıyı dağılımını belirler.
        // Frekans 0.0008 → büyük kıta ve okyanus şekilleri
        // ======================================================
        double continent = continentNoise.noise(x * 0.0008, z * 0.0008, 0.5, 0.5, true);

        if (continent < -0.20) {
            // --- Derin okyanus (%~20 alan) ---
            return deepOcean(x, z);
        }
        if (continent < -0.08) {
            // --- Sığ okyanus (%~12 alan) ---
            return shallowOcean(x, z);
        }
        if (continent < COAST_INNER) {
            // --- Plaj / Kıyı şeridi (%~8 alan) ---
            return coastBiome(x, z);
        }

        // ======================================================
        // ADIM 2 — YÜKSEKLİK NOISE (kara alanlar için)
        // Yüksek + sürekli kıta → dağ veya yanardağ
        // ======================================================
        double elevation = elevationNoise.noise(x * 0.0012, z * 0.0012, 0.5, 0.5, true);

        if (continent > 0.50 && elevation > 0.60) {
            // Yanardağ: çok nadir, çok yüksek kıta içi alanlar
            return "Volcano";
        }
        if (continent > 0.32 && elevation > 0.42) {
            // Dağ: yüksek kıta, orta-yüksek rakım
            return "Mountain";
        }
        if (continent > 0.20 && elevation < -0.50) {
            // Buzul: yüksek kıta + çok alçak rakım (rölatif çukur = buzul vadisi)
            // Sıcaklık kontrolü aşağıda yapılır
        }

        // ======================================================
        // ADIM 3 — İKLİM NOISE (kara biyom seçimi)
        // 1.5 çarpanı: noise [-1,1] → [-1.5, 1.5] → normalize sonrası
        // 0.0 ve 1.0 uçlarına daha fazla alan → Çöl ve Tundra gerçekten çıkabilsin
        // ======================================================
        double rawTemp  = tempNoise.noise(x * 0.0015, z * 0.0015,  0.5, 0.5, true) * 1.5;
        double rawHumid = humidNoise.noise(x * 0.0015, z * 0.0015, 0.5, 0.5, true) * 1.5;

        double temperature = clamp01((rawTemp  + 1.0) / 2.0);
        double humidity    = clamp01((rawHumid + 1.0) / 2.0);

        // ======================================================
        // ADIM 4 — KIYI TAMPONU (ÇÖL/TUNDRA-DENİZ SORUNUNU ÇÖZER)
        //
        // Kıyıya yakın karada (continent COAST_INNER ile COAST_OUTER arası)
        // sıcaklık değeri ılımanlaştırılır.
        //
        // Formül: tamponun içinde ne kadar derindeysek
        // (t=0 → plaj kenarı, t=1 → tamponun iç sınırı)
        // sıcaklığı orijinal değer ile ılıman aralık arasında lerp ederiz.
        //
        // Sonuç: Çöl (temp ~1.0) ve Tundra (temp ~0.0) kıyıya çıkamaz.
        // Çöl → Savanna → Beach geçişi, Tundra → Taiga → Beach geçişi doğal olur.
        // ======================================================
        if (continent >= COAST_INNER && continent < COAST_OUTER) {
            double t = (continent - COAST_INNER) / (COAST_OUTER - COAST_INNER); // [0,1]
            // t=0 (plaj kenarı) → tam ılıman | t=1 (tampon sonu) → orijinal değer
            double clampedTemp = clamp(temperature, COAST_TEMP_MIN, COAST_TEMP_MAX);
            temperature = lerp(clampedTemp, temperature, t * t); // karesel — geçiş yumuşak
        }

        // ======================================================
        // ADIM 5 — ÖZEL DURUMLAR (matris dışı biyomlar)
        // ======================================================

        // Buzul: soğuk + yüksek kıta + rölatif alçak rakım
        if (temperature < 0.15 && continent > 0.20 && elevation < -0.45) {
            return "Glacier";
        }

        // Bataklık: ılık-nemli + alçak kıta (kıyı hattına yakın iç kesim)
        if (temperature > 0.38 && temperature < 0.72
                && humidity > 0.72
                && continent >= COAST_OUTER && continent < 0.28) {
            return "Swamp";
        }

        // ======================================================
        // ADIM 6 — LOOKUP TABLO (O(1) kara biyom ataması)
        // ======================================================
        int tIdx = (int) (temperature * 100);
        int hIdx = (int) (humidity    * 100);
        // Sınır güvenliği
        tIdx = Math.max(0, Math.min(100, tIdx));
        hIdx = Math.max(0, Math.min(100, hIdx));

        return biomeLookupTable[tIdx][hIdx];
    }

    // ----------------------------------------------------------
    // YARDIMCI METODLAR
    // ----------------------------------------------------------

    /** Derin okyanus — sıcaklığa göre tip belirle */
    private String deepOcean(int x, int z) {
        double rawTemp   = tempNoise.noise(x * 0.002, z * 0.002, 0.5, 0.5, true);
        double temperature = clamp01((rawTemp + 1.0) / 2.0);
        if (temperature > 0.68) return "Warm_Ocean";
        if (temperature < 0.28) return "Frozen_Ocean";
        return "Ocean";
    }

    /** Sığ okyanus — derin okyanusla aynı mantık, ileride farklılaştırılabilir */
    private String shallowOcean(int x, int z) {
        double rawTemp   = tempNoise.noise(x * 0.002, z * 0.002, 0.5, 0.5, true);
        double temperature = clamp01((rawTemp + 1.0) / 2.0);
        if (temperature > 0.68) return "Warm_Ocean";
        if (temperature < 0.28) return "Frozen_Ocean";
        return "Ocean";
    }

    /** Kıyı / plaj biyomu — sıcaklığa göre karlı veya normal */
    private String coastBiome(int x, int z) {
        double rawTemp   = tempNoise.noise(x * 0.002, z * 0.002, 0.5, 0.5, true);
        double temperature = clamp01((rawTemp + 1.0) / 2.0);
        return (temperature < 0.22) ? "Snowy_Beach" : "Beach";
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double sq(double v) {
        return v * v;
    }

    public void ensureInitialized(WorldInfo info) {
        init(info);
    }

    // ----------------------------------------------------------
    // İÇ SINIF
    // ----------------------------------------------------------
    private static class BiomeData {
        final String name;
        final double temp;
        final double humid;

        BiomeData(String name, double temp, double humid) {
            this.name  = name;
            this.temp  = temp;
            this.humid = humid;
        }
    }
}
