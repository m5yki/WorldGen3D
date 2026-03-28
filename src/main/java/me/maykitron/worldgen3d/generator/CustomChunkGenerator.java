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
    private SimplexOctaveGenerator noodleNoise;
    private SimplexOctaveGenerator lavaNoise;
    private SimplexOctaveGenerator aquiferNoise;
    private SimplexOctaveGenerator riverNoise;
    private SimplexOctaveGenerator elevationNoise;
    private SimplexOctaveGenerator cliffNoise;
    private SimplexOctaveGenerator canyonNoise;
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
        riverNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 5L), 4);
        elevationNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 6L), 4);
        noodleNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 7L), 4);
        aquiferNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 8L), 2);
        cliffNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 9L), 2);
        canyonNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 10L), 4);
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

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        boolean useVanillaBlending = plugin.getConfig().getBoolean("settings.vanilla-terrain-blending", true);

        double[][] rawHeightMap = new double[18][18];

        for (int nx = -1; nx <= 16; nx++) {
            for (int nz = -1; nz <= 16; nz++) {
                int realX = startX + nx;
                int realZ = startZ + nz;

                TerrainData tData = getTerrainData(biomeProvider.getCustomBiomeName(realX, realZ));

                // 1. Base Terrain
                double h = biomeBlender.getBlendedHeight(realX, realZ, heightNoise);

                // 2. Mountain Noise
                if (useVanillaBlending) {
                    double eNoise = elevationNoise.noise(realX * 0.0015, realZ * 0.0015, 0.5, 0.5, true);
                    h += (eNoise * 10);
                }

                // 3. Terrain Type (Fantastik Biyomlar)
                switch (tData.terrainType) {
                    case "JAGGED":
                        double sharp = Math.abs(cliffNoise.noise(realX * 0.015, realZ * 0.015, 0.5, 0.5, true));
                        h += (sharp * 30);
                        break;
                    case "SMOOTH":
                        h = (h * 0.5) + (tData.baseHeight * 0.5);
                        break;
                    case "CANYON":
                        double cNoise = Math.abs(canyonNoise.noise(realX * 0.004, realZ * 0.004, 0.5, 0.5, true));
                        if (cNoise < 0.12) {
                            double carve = (0.12 - cNoise) / 0.12;
                            h -= (carve * carve) * 45;
                        }
                        break;
                    case "AMPLIFIED":
                        if (h > tData.waterLevel) {
                            h += (h - tData.waterLevel) * 0.85;
                        }
                        break;
                    case "NORMAL":
                    default:
                        break;
                }

                // ==========================================================
                // 4. BİYOMA ÖZEL NEHİR SİSTEMİ (Artık configten okunuyor!)
                // Nehir sadece YML'de 'enabled: true' ise oluşur.
                // ==========================================================
                if (tData.riverEnabled) {
                    double rNoise = riverNoise.noise(realX * 0.001, realZ * 0.001, 0.5, 0.5, true);
                    double riverValley = Math.abs(rNoise);

                    if (riverValley < tData.riverWidth) {
                        double carveForce = (tData.riverWidth - riverValley) / tData.riverWidth;
                        h -= (carveForce * carveForce) * tData.riverDepth;
                    }
                }

                rawHeightMap[nx + 1][nz + 1] = h;
            }
        }

        // Slope Limiter (Eğim Zımparası)
        for (int pass = 0; pass < 2; pass++) {
            for (int x = 1; x <= 16; x++) {
                for (int z = 1; z <= 16; z++) {
                    double center = rawHeightMap[x][z];
                    double left   = rawHeightMap[x - 1][z];
                    double right  = rawHeightMap[x + 1][z];
                    double up     = rawHeightMap[x][z - 1];
                    double down   = rawHeightMap[x][z + 1];

                    if (Math.abs(center - left) > 3.0 || Math.abs(center - right) > 3.0 ||
                            Math.abs(center - up) > 3.0 || Math.abs(center - down) > 3.0) {
                        rawHeightMap[x][z] = (center * 2 + left + right + up + down) / 6.0;
                    }
                }
            }
        }

        // Dünyayı İnşa Etme
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = startX + x;
                int realZ = startZ + z;

                double finalHeightDouble = rawHeightMap[x + 1][z + 1];
                int finalHeight = (int) Math.round(finalHeightDouble);

                TerrainData tData = getTerrainData(biomeProvider.getCustomBiomeName(realX, realZ));

                Material actualSurface = tData.surfaceBlock.getRandom(random);
                Material actualSub = tData.subBlock.getRandom(random);

                double slopeX = Math.abs(rawHeightMap[x + 1][z + 1] - rawHeightMap[x][z + 1]);
                double slopeZ = Math.abs(rawHeightMap[x + 1][z + 1] - rawHeightMap[x + 1][z]);
                double currentSlope = Math.max(slopeX, slopeZ);

                double cVal = (cliffNoise.noise(realX * 0.05, realZ * 0.05, 0.5, 0.5, true) + 1.0) / 2.0;
                if (currentSlope > 1.2 && cVal > 0.35) {
                    actualSurface = tData.rockyBlock.getRandom(random);
                }

                if (finalHeight < tData.waterLevel) {
                    actualSurface = random.nextBoolean() ? Material.SAND : Material.GRAVEL;
                    actualSub = actualSurface;
                }

                chunkData.setBlock(x, finalHeight, z, actualSurface);
                for (int y = finalHeight - 1; y >= finalHeight - tData.subDepth; y--) {
                    chunkData.setBlock(x, y, z, actualSub);
                }
                for (int y = finalHeight - tData.subDepth - 1; y > worldInfo.getMinHeight(); y--) {
                    chunkData.setBlock(x, y, z, tData.deepBlock.getRandom(random));
                }
                chunkData.setBlock(x, worldInfo.getMinHeight(), z, Material.BEDROCK);
                for (int y = finalHeight + 1; y <= tData.waterLevel; y++) {
                    chunkData.setBlock(x, y, z, Material.WATER);
                }

                // Mağara Sistemi
                if (tData.caveMode.equalsIgnoreCase("CUSTOM")) {
                    int maxCaveY = finalHeight - 12;
                    if (finalHeight < tData.waterLevel + 5) {
                        maxCaveY = Math.min(maxCaveY, tData.waterLevel - 12);
                    }

                    for (int y = worldInfo.getMinHeight() + 2; y < maxCaveY; y++) {
                        double cheese = (caveNoise.noise(realX * tData.caveFreq, y * tData.caveFreq, realZ * tData.caveFreq, 0.5, 0.5, true) + 1.0) / 2.0;
                        double n1 = noodleNoise.noise(realX * 0.015, y * 0.015, realZ * 0.015, 0.5, 0.5, true);
                        double n2 = caveNoise.noise(realX * 0.015, y * 0.015, realZ * 0.015, 0.5, 0.5, true);
                        boolean isNoodle = Math.abs(n1) < 0.05 && Math.abs(n2) < 0.05;

                        double depthRatio = (double) (y - worldInfo.getMinHeight()) / (maxCaveY - worldInfo.getMinHeight());
                        double surfaceFalloff = Math.pow(depthRatio, 4) * 0.35;
                        double finalCaveThreshold = (tData.caveThreshold - 0.08) + surfaceFalloff;

                        if (cheese > finalCaveThreshold || isNoodle) {
                            Material currentBlock = chunkData.getBlockData(x, y, z).getMaterial();
                            if (currentBlock == Material.WATER || currentBlock == Material.BEDROCK) continue;

                            int localLavaLevel = tData.baseLavaLevel;
                            if (tData.dynamicLava) {
                                double lNoise = lavaNoise.noise(realX * 0.01, realZ * 0.01, 0.5, 0.5, true);
                                localLavaLevel += (int) (lNoise * 10);
                            }

                            double aqNoise = aquiferNoise.noise(realX * 0.02, realZ * 0.02, 0.5, 0.5, true);
                            boolean isAquifer = (y > localLavaLevel && y < 30 && aqNoise > 0.45);

                            if (y <= localLavaLevel) {
                                chunkData.setBlock(x, y, z, Material.LAVA);
                                if (y == localLavaLevel && random.nextInt(100) < 8) {
                                    chunkData.setBlock(x, y - 1, z, Material.MAGMA_BLOCK);
                                }
                            } else if (isAquifer) {
                                chunkData.setBlock(x, y, z, Material.WATER);
                            } else {
                                chunkData.setBlock(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static class RandomBlockSelector {
        private final List<Material> materials = new ArrayList<>();
        private final List<Integer> weights = new ArrayList<>();
        private int totalWeight = 0;

        public RandomBlockSelector(Object obj, Material defaultMat) {
            List<String> list = new ArrayList<>();
            if (obj instanceof List) { list = (List<String>) obj; } else if (obj instanceof String) { list.add((String) obj); }
            if (list.isEmpty()) { materials.add(defaultMat); weights.add(100); totalWeight = 100; return; }

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
            for (int i = 0; i < materials.size(); i++) { count += weights.get(i); if (r < count) return materials.get(i); }
            return materials.get(0);
        }
        public boolean contains(Material mat) { return materials.contains(mat); }
    }

    public static class TerrainData {
        public final String name;
        public final String terrainType;
        public final int baseHeight;
        public final int heightVariation;
        public final double roughness;
        public final int waterLevel;
        public final int subDepth;

        // YENİ: Nehir Ayarları
        public final boolean riverEnabled;
        public final double riverWidth;
        public final int riverDepth;

        public final RandomBlockSelector surfaceBlock;
        public final RandomBlockSelector subBlock;
        public final RandomBlockSelector deepBlock;
        public final RandomBlockSelector rockyBlock;

        public final String caveMode;
        public final double caveFreq;
        public final double caveThreshold;
        public final boolean dynamicLava;
        public final int baseLavaLevel;
        public final YamlConfiguration rawConfig;

        public final int grassChance, flowerChance, treeChance;
        public final List<String> flowerTypes, treeFiles;
        public final boolean treesEnabled;

        public final int seagrassChance, kelpChance, sugarcaneChance;
        public final boolean oceanFloorEnabled;
        public final int oceanFloorChance;
        public final List<String> oceanFloorBlocks;

        public TerrainData(YamlConfiguration rawConfig) {
            this.rawConfig = rawConfig;
            this.name = rawConfig.getString("name", "Bilinmeyen");
            this.terrainType = rawConfig.getString("terrain.type", "NORMAL").toUpperCase();

            this.baseHeight = rawConfig.getInt("terrain.base-height", 65);
            this.heightVariation = rawConfig.getInt("terrain.height-variation", 15);
            this.roughness = rawConfig.getDouble("terrain.roughness", 0.015);
            this.waterLevel = rawConfig.getInt("terrain.water-level", 62);
            this.subDepth = rawConfig.getInt("blocks.sub-depth", 4);

            // Nehir Ayarları (Eğer YML'de yazmıyorsa nehir varsayılan olarak KAPALI olur)
            this.riverEnabled = rawConfig.getBoolean("river.enabled", false);
            this.riverWidth = rawConfig.getDouble("river.width", 0.08);
            this.riverDepth = rawConfig.getInt("river.depth", 16);

            this.surfaceBlock = new RandomBlockSelector(rawConfig.get("blocks.surface"), Material.GRASS_BLOCK);
            this.subBlock = new RandomBlockSelector(rawConfig.get("blocks.sub"), Material.DIRT);
            this.deepBlock = new RandomBlockSelector(rawConfig.get("blocks.deep"), Material.STONE);
            this.rockyBlock = new RandomBlockSelector(rawConfig.get("blocks.rocky"), Material.STONE);

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