package me.maykitron.worldgen3d.generator;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.context.BlockContext;
import me.maykitron.worldgen3d.layer.BlockLayer;
import me.maykitron.worldgen3d.layer.StandardLayer;
import me.maykitron.worldgen3d.manager.OreManager;
import me.maykitron.worldgen3d.manager.TreeManager;
import me.maykitron.worldgen3d.populator.FloraPopulator;
import me.maykitron.worldgen3d.populator.OrePopulator;
import me.maykitron.worldgen3d.populator.SchematicPopulator;
import me.maykitron.worldgen3d.shaper.BeachShaper;
import me.maykitron.worldgen3d.shaper.CaveShaper;
import me.maykitron.worldgen3d.shaper.MountainShaper;
import me.maykitron.worldgen3d.shaper.OceanShaper;
import me.maykitron.worldgen3d.shaper.RiverShaper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CustomChunkGenerator extends ChunkGenerator {

    private final WorldGen3D plugin;
    private final CustomBiomeProvider biomeProvider;
    private final Map<String, TerrainData> terrainDataMap = new HashMap<>();
    private TerrainData fallbackData;

    private final BiomeBlender biomeBlender;

    // 3D Noise Motorları
    private SimplexOctaveGenerator heightNoise;
    private SimplexOctaveGenerator caveNoise;
    private SimplexOctaveGenerator riverNoise;
    private SimplexOctaveGenerator mountainNoise;

    // Şekillendiriciler (Shapers)
    private RiverShaper riverShaper;
    private MountainShaper mountainShaper;
    private OceanShaper oceanShaper;
    private BeachShaper beachShaper;
    private CaveShaper caveShaper;

    private boolean isNoiseInitialized = false;

    public CustomChunkGenerator(WorldGen3D plugin, CustomBiomeProvider biomeProvider) {
        this.plugin = plugin;
        this.biomeProvider = biomeProvider;
        this.biomeBlender = new BiomeBlender(this);
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

    private void loadTerrainDataFromYML() {
        terrainDataMap.clear();
        for (File file : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            TerrainData data = new TerrainData(config, plugin);
            terrainDataMap.put(data.name, data);
            if (fallbackData == null) fallbackData = data;
        }
    }

    private void initializeNoise(WorldInfo worldInfo) {
        if (isNoiseInitialized) return;
        Random random = new Random(worldInfo.getSeed());

        heightNoise = new SimplexOctaveGenerator(random, 8);
        caveNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 3L), 4);
        riverNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 5L), 4);
        mountainNoise = new SimplexOctaveGenerator(new Random(worldInfo.getSeed() * 6L), 4);

        riverShaper = new RiverShaper(riverNoise);
        mountainShaper = new MountainShaper(mountainNoise);
        oceanShaper = new OceanShaper();
        beachShaper = new BeachShaper();
        caveShaper = new CaveShaper(caveNoise);

        biomeProvider.ensureInitialized(worldInfo);
        isNoiseInitialized = true;
    }

    public TerrainData getTerrainData(String biomeName) {
        return terrainDataMap.getOrDefault(biomeName, fallbackData);
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        initializeNoise(worldInfo);

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        BiomeBlender.BiomeCache biomeCache = new BiomeBlender.BiomeCache(startX, startZ, biomeProvider);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = startX + x;
                int realZ = startZ + z;

                TerrainData tData = getTerrainData(biomeCache.getBiome(realX, realZ));

                double baseHeight = biomeBlender.getBlendedHeight(realX, realZ, heightNoise, biomeCache);
                double slopeX = Math.abs(baseHeight - biomeBlender.getBlendedHeight(realX + 1, realZ, heightNoise, biomeCache));
                double slopeZ = Math.abs(baseHeight - biomeBlender.getBlendedHeight(realX, realZ + 1, heightNoise, biomeCache));
                double currentSlope = Math.max(slopeX, slopeZ);

                int solidDepth = 0;
                boolean hasHitSurface = false;

                // 319'dan aşağıya inen Top-Down Voxel Tarama Motoru
                for (int y = 319; y >= worldInfo.getMinHeight(); y--) {
                    double density = baseHeight - y;

                    // Şekillendiriciler (Shapers)
                    density = mountainShaper.apply(realX, y, realZ, density, tData);
                    density = caveShaper.apply(realX, y, realZ, density, tData);
                    density = riverShaper.apply(realX, y, realZ, density, tData);
                    density = oceanShaper.apply(y, density, tData);
                    density = beachShaper.apply(y, density, tData);

                    if (density > 0) {
                        hasHitSurface = true;
                        solidDepth++;

                        BlockContext ctx = new BlockContext();
                        ctx.x = realX; ctx.y = y; ctx.z = realZ;
                        ctx.baseHeight = baseHeight;
                        ctx.slope = currentSlope;
                        ctx.density = density;
                        ctx.isNearWater = (y >= tData.waterLevel - 1 && y <= tData.waterLevel + 1);
                        ctx.isUnderground = (y < baseHeight - 8);
                        ctx.biome = tData;
                        ctx.random = random;

                        Material blockToPlace;

                        if (solidDepth == 1) {
                            if (ctx.isUnderground) blockToPlace = tData.deepBlock.getRandom(random);
                            else blockToPlace = tData.surfaceLayer.getBlock(ctx);
                        }
                        else if (solidDepth <= tData.subDepth) {
                            if (ctx.isUnderground) blockToPlace = tData.deepBlock.getRandom(random);
                            else blockToPlace = tData.subLayer.getBlock(ctx);
                        }
                        else {
                            blockToPlace = tData.deepBlock.getRandom(random);
                        }

                        if (y == worldInfo.getMinHeight()) blockToPlace = Material.BEDROCK;
                        chunkData.setBlock(x, y, z, blockToPlace);

                    } else {
                        solidDepth = 0;

                        if (!hasHitSurface && y <= tData.waterLevel) {
                            chunkData.setBlock(x, y, z, Material.WATER);
                        }
                        else if (hasHitSurface) {
                            if (y <= tData.baseLavaLevel) {
                                chunkData.setBlock(x, y, z, Material.LAVA);
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================================
    // RASTGELE BLOK SEÇİCİ
    // ==========================================================
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
                    String cleanName = parts[0].replace("minecraft:", "").toUpperCase();
                    if (parts.length == 3) { cleanName = parts[1].replace("minecraft:", "").toUpperCase(); weight = Integer.parseInt(parts[2]); }
                    else if (parts.length == 2 && !s.startsWith("minecraft:")) { weight = Integer.parseInt(parts[1]); }
                    mat = Material.matchMaterial(cleanName);
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

        // İŞTE EKSİK OLAN VE HATAYA SEBEP OLAN METOD BURADA!
        public boolean contains(Material mat) {
            return materials.contains(mat);
        }
    }

    // ==========================================================
    // BİYOM VERİ OBJESİ (MODÜLER)
    // ==========================================================
    public static class TerrainData {
        public final String name;
        public final int baseHeight;
        public final int heightVariation;
        public final double roughness;
        public final int waterLevel;
        public final int subDepth;

        public final String caveMode;
        public final double caveFreq;
        public final double caveThreshold;
        public final int baseLavaLevel;

        public final boolean riverEnabled;
        public final double riverWidth;
        public final int riverDepth;
        public final String terrainType;

        public final BlockLayer surfaceLayer;
        public final BlockLayer subLayer;
        public final RandomBlockSelector deepBlock;

        public final OreManager.OreProfile oreProfile;
        public final TreeManager.TreeProfile treeProfile;
        public final YamlConfiguration rawConfig;

        public TerrainData(YamlConfiguration rawConfig, WorldGen3D plugin) {
            this.rawConfig = rawConfig;
            this.name = rawConfig.getString("name", "Flat_Fallback");

            this.baseHeight = rawConfig.getInt("terrain.base-height", 65);
            this.heightVariation = rawConfig.getInt("terrain.height-variation", 15);
            this.roughness = rawConfig.getDouble("terrain.roughness", 0.015);
            this.terrainType = rawConfig.getString("terrain.type", "NORMAL").toUpperCase();

            this.riverEnabled = rawConfig.getBoolean("river.enabled", false);
            this.riverWidth = rawConfig.getDouble("river.width", 0.08);
            this.riverDepth = rawConfig.getInt("river.depth", 16);

            this.waterLevel = rawConfig.getInt("terrain.water-level", 62);
            this.subDepth = rawConfig.getInt("blocks.sub-depth", 4);

            this.caveMode = rawConfig.getString("caves.mode", "VANILLA");
            this.caveFreq = rawConfig.getDouble("caves.noise-frequency", 0.035);
            this.caveThreshold = rawConfig.getDouble("caves.threshold", 0.65);
            this.baseLavaLevel = rawConfig.getInt("caves.base-lava-level", -54);

            RandomBlockSelector surfaceBlocks = new RandomBlockSelector(rawConfig.get("blocks.surface"), Material.GRASS_BLOCK);
            RandomBlockSelector rockyBlocks = new RandomBlockSelector(rawConfig.get("blocks.rocky"), Material.STONE);
            this.surfaceLayer = new StandardLayer(surfaceBlocks, rockyBlocks);

            RandomBlockSelector subBlocks = new RandomBlockSelector(rawConfig.get("blocks.sub"), Material.DIRT);
            this.subLayer = new StandardLayer(subBlocks, subBlocks);

            this.deepBlock = new RandomBlockSelector(rawConfig.get("blocks.deep"), Material.STONE);

            String oreId = rawConfig.getString("ore-profile", "default_ores");
            this.oreProfile = plugin.getOreManager().getProfile(oreId);

            String treeId = rawConfig.getString("tree-profile", "none");
            this.treeProfile = plugin.getTreeManager().getProfile(treeId);
        }
    }
}