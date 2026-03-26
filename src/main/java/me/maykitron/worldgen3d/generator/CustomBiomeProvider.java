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
    private boolean isInitialized = false;

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
    }

    private void init(WorldInfo worldInfo) {
        if (isInitialized) return;
        Random r = new Random(worldInfo.getSeed());
        tempNoise = new SimplexOctaveGenerator(r, 4);
        humidNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() + 1), 4);
        isInitialized = true;
    }

    // ÇÖZÜM 1: Artık F3 ekranında ve su renklerinde doğru Vanilla biyomu görünecek!
    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        String customName = getCustomBiomeName(x, z).toLowerCase();
        if (customName.contains("ocean")) return Biome.OCEAN;
        if (customName.contains("jungle")) return Biome.JUNGLE;
        if (customName.contains("desert")) return Biome.DESERT;
        if (customName.contains("forest")) return Biome.FOREST;
        if (customName.contains("snow") || customName.contains("tundra")) return Biome.SNOWY_PLAINS;
        return Biome.PLAINS;
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return new ArrayList<>(List.of(Biome.PLAINS, Biome.OCEAN, Biome.JUNGLE, Biome.DESERT, Biome.FOREST, Biome.SNOWY_PLAINS));
    }

    public String getCustomBiomeName(int x, int z) {
        if (tempNoise == null) return "Plains";

        // ÇÖZÜM 2: 0.0015'i 0.005 yaptık. Artık biyomlar 5000 blok değil, ortalama 500-1000 blok olacak. Keşfetmesi daha keyifli!
        double temperature = tempNoise.noise(x * 0.005, z * 0.005, 0.5, 0.5, true);
        double humidity = humidNoise.noise(x * 0.005, z * 0.005, 0.5, 0.5, true);

        temperature = (temperature + 1.0) / 2.0;
        humidity = (humidity + 1.0) / 2.0;

        BiomeData closest = loadedBiomes.get(0);
        double minDistance = Double.MAX_VALUE;

        for (BiomeData data : loadedBiomes) {
            double dist = Math.pow(temperature - data.temp, 2) + Math.pow(humidity - data.humid, 2);
            if (dist < minDistance) {
                minDistance = dist;
                closest = data;
            }
        }
        return closest.name;
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