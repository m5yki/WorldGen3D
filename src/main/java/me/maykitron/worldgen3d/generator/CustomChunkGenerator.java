package me.maykitron.worldgen3d.generator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.populator.FloraPopulator;
import me.maykitron.worldgen3d.populator.OrePopulator;
import me.maykitron.worldgen3d.populator.SchematicPopulator;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomChunkGenerator extends ChunkGenerator {

    private final WorldGen3D plugin;
    private final CustomBiomeProvider biomeProvider;
    private final List<TerrainData> loadedTerrainData = new ArrayList<>();

    private final BiomeBlender biomeBlender;
    private SimplexOctaveGenerator heightNoise;
    private SimplexOctaveGenerator caveNoise;
    private SimplexOctaveGenerator lavaNoise;
    private boolean isNoiseInitialized = false;

    public CustomChunkGenerator(WorldGen3D plugin, CustomBiomeProvider biomeProvider) {
        this.plugin = plugin;
        this.biomeProvider = biomeProvider;
        this.biomeBlender = new BiomeBlender(biomeProvider, this);
        loadTerrainDataFromYML();
    }

    @Nullable
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return this.biomeProvider;
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(@NotNull org.bukkit.World world) {
        List<BlockPopulator> populators = new ArrayList<>();
        populators.add(new OrePopulator(plugin, this, biomeProvider));
        populators.add(new SchematicPopulator(plugin, this, biomeProvider));
        populators.add(new FloraPopulator(plugin, this, biomeProvider));
        return populators;
    }

    @Override
    public boolean shouldGenerateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        int realX = (chunkX * 16) + 8;
        int realZ = (chunkZ * 16) + 8;
        TerrainData tData = getTerrainData(biomeProvider.getCustomBiomeName(realX, realZ));
        return tData.caveMode.equalsIgnoreCase("VANILLA");
    }

    private void loadTerrainDataFromYML() {
        loadedTerrainData.clear();
        for (File file : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            loadedTerrainData.add(new TerrainData(config));
        }
    }

    private void initializeNoise(WorldInfo worldInfo) {
        if (isNoiseInitialized) return;
        Random random = new Random(worldInfo.getSeed());
        heightNoise = new SimplexOctaveGenerator(random, 8);
        caveNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 3L), 4);
        lavaNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 4L), 2);
        biomeProvider.ensureInitialized(worldInfo);
        isNoiseInitialized = true;
    }

    public TerrainData getTerrainData(String biomeName) {
        for (TerrainData data : loadedTerrainData) {
            if (data.name.equals(biomeName)) return data;
        }
        return loadedTerrainData.get(0);
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        initializeNoise(worldInfo);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = (chunkX * 16) + x;
                int realZ = (chunkZ * 16) + z;

                // Nihai yüksekliği Zımpara Motorundan (BiomeBlender) alıyoruz. Kavisli göller orada hesaplanıyor!
                double finalHeightDouble = biomeBlender.getBlendedHeight(realX, realZ, heightNoise);
                int finalHeight = (int) Math.round(finalHeightDouble);

                TerrainData tData = getTerrainData(biomeProvider.getCustomBiomeName(realX, realZ));

                // Yüzdelik Oranlara Göre Rastgele Mozaik Blok Seçimi
                Material actualSurface = tData.surfaceBlock.getRandom(random);
                Material actualSub = tData.subBlock.getRandom(random);

                // Eğer su seviyesinin altındaysa yüzeyi kum veya çakıl yap (Çimen olmasın)
                if (finalHeight < tData.waterLevel) {
                    actualSurface = random.nextBoolean() ? Material.SAND : Material.GRAVEL;
                    actualSub = actualSurface;
                }

                // 1. Yüzey Katmanı
                chunkData.setBlock(x, finalHeight, z, actualSurface);

                // 2. Toprak Katmanı (YML'deki 'sub-depth' kadar iniyor)
                for (int y = finalHeight - 1; y >= finalHeight - tData.subDepth; y--) {
                    chunkData.setBlock(x, y, z, actualSub);
                }

                // 3. Derin Taş Katmanı (Andezit/Taş mozaikleri)
                for (int y = finalHeight - tData.subDepth - 1; y > worldInfo.getMinHeight(); y--) {
                    chunkData.setBlock(x, y, z, tData.deepBlock.getRandom(random));
                }

                chunkData.setBlock(x, worldInfo.getMinHeight(), z, Material.BEDROCK);

                // Suyu Doldur
                for (int y = finalHeight + 1; y <= tData.waterLevel; y++) {
                    chunkData.setBlock(x, y, z, Material.WATER);
                }

                // =========================================================
                // 3D MAĞARALAR VE MÜKEMMEL YÜZEY KORUMASI
                // =========================================================
                if (tData.caveMode.equalsIgnoreCase("CUSTOM")) {

                    // 1. YÜZEY KORUMASI: Mağaralar toprağın hemen altından değil, en az 12 blok derinden başlamalı!
                    // Yoksa yüzey çöker ve o anlamsız merdivenimsi çukurlar oluşur.
                    int maxCaveY = finalHeight - 12;

                    // 2. KIYI VE SU KORUMASI: Eğer arazi su seviyesinin altındaysa VEYA su seviyesine çok yakınsa (plaj/sahil),
                    // mağaraları zorla okyanus tabanının veya kumsalın çok daha altına (su seviyesinden en az 12 blok aşağı) it!
                    if (finalHeight < tData.waterLevel + 5) {
                        maxCaveY = Math.min(maxCaveY, tData.waterLevel - 12);
                    }

                    for (int y = worldInfo.getMinHeight() + 2; y < maxCaveY; y++) {
                        double cNoise = (caveNoise.noise(realX * tData.caveFreq, y * tData.caveFreq, realZ * tData.caveFreq, 0.5, 0.5, true) + 1.0) / 2.0;

                        if (cNoise > tData.caveThreshold) {

                            // Ekstra Güvenlik: Kazara suyu veya katman kayasını (bedrock) silmesini engeller
                            Material currentBlock = chunkData.getBlockData(x, y, z).getMaterial();
                            if (currentBlock == Material.WATER || currentBlock == Material.BEDROCK) continue;

                            int localLavaLevel = tData.baseLavaLevel;
                            if (tData.dynamicLava) {
                                double lNoise = lavaNoise.noise(realX * 0.01, realZ * 0.01, 0.5, 0.5, true);
                                localLavaLevel += (int) (lNoise * 10);
                            }

                            if (y <= localLavaLevel) {
                                chunkData.setBlock(x, y, z, Material.LAVA);
                                if (y == localLavaLevel && random.nextInt(100) < 8) {
                                    chunkData.setBlock(x, y - 1, z, Material.MAGMA_BLOCK);
                                }
                            } else {
                                chunkData.setBlock(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================================
    // MÜKEMMEL YÜZDELİK HESAPLAYICISI (Hata vermeyen versiyon)
    // ==========================================================
    @SuppressWarnings("unchecked")
    public static class RandomBlockSelector {
        private final List<Material> materials = new ArrayList<>();
        private final List<Integer> weights = new ArrayList<>();
        private int totalWeight = 0;

        // Object kabul ederek hem tekli hem de liste verileri hatasız çözer
        public RandomBlockSelector(Object obj, Material defaultMat) {
            List<String> list = new ArrayList<>();
            if (obj instanceof List) {
                list = (List<String>) obj;
            } else if (obj instanceof String) {
                list.add((String) obj);
            }

            if (list.isEmpty()) {
                materials.add(defaultMat); weights.add(100); totalWeight = 100;
                return;
            }

            for (String s : list) {
                String[] parts = s.split(":");
                Material mat = null; int weight = 100;
                try {
                    if (parts.length == 3) { mat = Material.getMaterial(parts[1].toUpperCase()); weight = Integer.parseInt(parts[2]); }
                    else if (parts.length == 2 && !s.startsWith("minecraft:")) { mat = Material.getMaterial(parts[0].toUpperCase()); weight = Integer.parseInt(parts[1]); }
                    else { mat = Material.getMaterial(s.replace("minecraft:", "").toUpperCase()); }
                } catch (Exception ignored) {}

                if (mat == null) mat = defaultMat;
                materials.add(mat); weights.add(weight); totalWeight += weight;
            }
        }

        public Material getRandom(Random random) {
            if (totalWeight <= 0) return materials.get(0);
            int r = random.nextInt(totalWeight);
            int count = 0;
            for (int i = 0; i < materials.size(); i++) {
                count += weights.get(i);
                if (r < count) return materials.get(i);
            }
            return materials.get(0);
        }

        public boolean contains(Material mat) { return materials.contains(mat); }
    }

    // ==========================================================
    // RAM ÖNBELLEK VERİ MERKEZİ (Işık hızında jenerasyon için)
    // ==========================================================
    public static class TerrainData {
        public final String name;
        public final int baseHeight;
        public final int heightVariation;
        public final double roughness;
        public final int waterLevel;
        public final int subDepth;

        public final RandomBlockSelector surfaceBlock;
        public final RandomBlockSelector subBlock;
        public final RandomBlockSelector deepBlock;

        public final String caveMode;
        public final double caveFreq;
        public final double caveThreshold;
        public final boolean dynamicLava;
        public final int baseLavaLevel;
        public final YamlConfiguration rawConfig;

        public final int grassChance, flowerChance, treeChance;
        public final List<String> flowerTypes, treeFiles;
        public final boolean treesEnabled;

        public final int seagrassChance;
        public final int kelpChance;
        public final int sugarcaneChance;
        public final boolean oceanFloorEnabled;
        public final int oceanFloorChance;
        public final List<String> oceanFloorBlocks;

        public TerrainData(YamlConfiguration rawConfig) {
            this.rawConfig = rawConfig;
            this.name = rawConfig.getString("name", "Bilinmeyen");
            this.baseHeight = rawConfig.getInt("terrain.base-height", 65);
            this.heightVariation = rawConfig.getInt("terrain.height-variation", 15);
            this.roughness = rawConfig.getDouble("terrain.roughness", 0.015);
            this.waterLevel = rawConfig.getInt("terrain.water-level", 62);
            this.subDepth = rawConfig.getInt("blocks.sub-depth", 4);

            this.surfaceBlock = new RandomBlockSelector(rawConfig.get("blocks.surface"), Material.GRASS_BLOCK);
            this.subBlock = new RandomBlockSelector(rawConfig.get("blocks.sub"), Material.DIRT);
            this.deepBlock = new RandomBlockSelector(rawConfig.get("blocks.deep"), Material.STONE);

            this.caveMode = rawConfig.getString("caves.mode", "VANILLA");
            this.caveFreq = rawConfig.getDouble("caves.noise-frequency", 0.025);
            this.caveThreshold = rawConfig.getDouble("caves.threshold", 0.45);
            this.dynamicLava = rawConfig.getBoolean("caves.dynamic-lava", true);
            this.baseLavaLevel = rawConfig.getInt("caves.base-lava-level", -54);

            this.grassChance = rawConfig.getInt("flora.grass-chance", 0);
            this.flowerChance = rawConfig.getInt("flora.flowers.chance", 0);
            this.flowerTypes = rawConfig.getStringList("flora.flowers.types");
            this.treesEnabled = rawConfig.getBoolean("schematics.trees.enabled", false);
            this.treeChance = rawConfig.getInt("schematics.trees.chance", 0);
            this.treeFiles = rawConfig.getStringList("schematics.trees.files");

            this.seagrassChance = rawConfig.getInt("flora.water-flora.seagrass-chance", 0);
            this.kelpChance = rawConfig.getInt("flora.water-flora.kelp-chance", 0);
            this.sugarcaneChance = rawConfig.getInt("flora.coastal-flora.sugarcane-chance", 0);
            this.oceanFloorEnabled = rawConfig.getBoolean("ocean-floor.enabled", false);
            this.oceanFloorChance = rawConfig.getInt("ocean-floor.chance", 0);
            this.oceanFloorBlocks = rawConfig.getStringList("ocean-floor.blocks");
        }
    }
}