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
import me.maykitron.worldgen3d.shaper.*;
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

    // ==========================================================
    // GLOBAL SU SEVİYESİ
    // Tüm dünyada tek bir su referansı. Kara biyomlar bu
    // seviyenin altına inmez — YML'deki waterLevel artık
    // sadece nehir/okyanus hesaplarında kullanılır, kara
    // yüzeyi asla bu seviyenin altında üretilmez.
    // ==========================================================
    public static final int GLOBAL_SEA_LEVEL = 62;

    private final WorldGen3D plugin;
    private final CustomBiomeProvider biomeProvider;
    private final Map<String, TerrainData> terrainDataMap = new HashMap<>();
    private TerrainData fallbackData;
    private final BiomeBlender biomeBlender;

    // --- Noise motorları ---
    private SimplexOctaveGenerator heightNoise;
    private SimplexOctaveGenerator detailNoise;
    private SimplexOctaveGenerator caveNoise;
    private SimplexOctaveGenerator cave2Noise;
    private SimplexOctaveGenerator riverNoise;
    private SimplexOctaveGenerator mountainNoise;
    private SimplexOctaveGenerator canyonNoise;
    private SimplexOctaveGenerator volcanoNoise;
    private SimplexOctaveGenerator floatingNoise;
    private SimplexOctaveGenerator trenchNoise;
    private SimplexOctaveGenerator glacierNoise;

    // --- Shaper'lar ---
    private RiverShaper        riverShaper;
    private MountainShaper     mountainShaper;
    private OceanShaper        oceanShaper;
    private BeachShaper        beachShaper;
    private CaveShaper         caveShaper;
    private CanyonShaper       canyonShaper;
    private VolcanoShaper      volcanoShaper;
    private FloatingIslandShaper floatingIslandShaper;
    private OceanTrenchShaper  oceanTrenchShaper;
    private GlacierShaper      glacierShaper;

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
        long seed = worldInfo.getSeed();

        heightNoise   = new SimplexOctaveGenerator(new Random(seed),           10);
        detailNoise   = new SimplexOctaveGenerator(new Random(seed * 2L + 31),  4);
        caveNoise     = new SimplexOctaveGenerator(new Random(seed * 3L + 7),   4);
        cave2Noise    = new SimplexOctaveGenerator(new Random(seed * 7L + 13),  4);
        riverNoise    = new SimplexOctaveGenerator(new Random(seed * 5L + 19),  4);
        mountainNoise = new SimplexOctaveGenerator(new Random(seed * 6L + 23),  6);
        canyonNoise   = new SimplexOctaveGenerator(new Random(seed * 9L + 37),  4);
        volcanoNoise  = new SimplexOctaveGenerator(new Random(seed * 11L + 41), 4);
        floatingNoise = new SimplexOctaveGenerator(new Random(seed * 13L + 53), 4);
        trenchNoise   = new SimplexOctaveGenerator(new Random(seed * 15L + 59), 4);
        glacierNoise  = new SimplexOctaveGenerator(new Random(seed * 17L + 61), 4);

        riverShaper        = new RiverShaper(riverNoise);
        mountainShaper     = new MountainShaper(mountainNoise);
        oceanShaper        = new OceanShaper();
        beachShaper        = new BeachShaper();
        caveShaper         = new CaveShaper(caveNoise, cave2Noise);
        canyonShaper       = new CanyonShaper(canyonNoise);
        volcanoShaper      = new VolcanoShaper(volcanoNoise);
        floatingIslandShaper = new FloatingIslandShaper(floatingNoise);
        oceanTrenchShaper  = new OceanTrenchShaper(trenchNoise);
        glacierShaper      = new GlacierShaper(glacierNoise);

        biomeProvider.ensureInitialized(worldInfo);
        isNoiseInitialized = true;
    }

    public TerrainData getTerrainData(String biomeName) {
        return terrainDataMap.getOrDefault(biomeName, fallbackData);
    }

    public SimplexOctaveGenerator getDetailNoise() { return detailNoise; }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        initializeNoise(worldInfo);

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        BiomeBlender.BiomeCache biomeCache = new BiomeBlender.BiomeCache(startX, startZ, biomeProvider);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = startX + x;
                int realZ = startZ + z;

                String biomeName = biomeCache.getBiome(realX, realZ);
                TerrainData tData = getTerrainData(biomeName);

                double baseHeight = biomeBlender.getBlendedHeight(
                        realX, realZ, heightNoise, detailNoise, biomeCache);

                // =====================================================
                // SU SEVİYESİ KORUMASI
                // Kara biyomlar asla GLOBAL_SEA_LEVEL'ın altına inmez.
                // Su biyomları (okyanus, plaj) bu kontrol dışındadır.
                // =====================================================
                boolean isWaterBiome = tData.isWaterBiome;
                if (!isWaterBiome) {
                    // Kara yüzeyi en az deniz seviyesi + 2 blok yukarıda olmalı
                    baseHeight = Math.max(baseHeight, GLOBAL_SEA_LEVEL + 2);
                }

                double slopeX = Math.abs(baseHeight - biomeBlender.getBlendedHeight(
                        realX + 1, realZ, heightNoise, detailNoise, biomeCache));
                double slopeZ = Math.abs(baseHeight - biomeBlender.getBlendedHeight(
                        realX, realZ + 1, heightNoise, detailNoise, biomeCache));
                double currentSlope = Math.max(slopeX, slopeZ);

                int solidDepth = 0;
                boolean hasHitSurface = false;

                for (int y = 319; y >= worldInfo.getMinHeight(); y--) {
                    double density = baseHeight - y;

                    // ==============================================
                    // SHAPER SIRASI (büyük form → küçük form → boşluk)
                    // ==============================================
                    density = mountainShaper.apply(realX, y, realZ, density, tData);
                    density = volcanoShaper.apply(realX, y, realZ, density, tData);
                    density = canyonShaper.apply(realX, y, realZ, density, tData);
                    density = glacierShaper.apply(realX, y, realZ, density, tData);
                    density = riverShaper.apply(realX, y, realZ, density, tData);
                    density = oceanTrenchShaper.apply(realX, y, realZ, density, tData);
                    density = floatingIslandShaper.apply(realX, y, realZ, density, tData);
                    density = caveShaper.apply(realX, y, realZ, density, tData);
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
                        ctx.isNearWater = (y >= GLOBAL_SEA_LEVEL - 1 && y <= GLOBAL_SEA_LEVEL + 1);
                        ctx.isUnderground = (y < baseHeight - 8);
                        ctx.biome = tData;
                        ctx.random = random;

                        Material blockToPlace;
                        if (solidDepth == 1) {
                            blockToPlace = ctx.isUnderground
                                    ? tData.deepBlock.getRandom(random)
                                    : tData.surfaceLayer.getBlock(ctx);
                        } else if (solidDepth <= tData.subDepth) {
                            blockToPlace = ctx.isUnderground
                                    ? tData.deepBlock.getRandom(random)
                                    : tData.subLayer.getBlock(ctx);
                        } else {
                            blockToPlace = tData.deepBlock.getRandom(random);
                        }

                        if (y == worldInfo.getMinHeight()) blockToPlace = Material.BEDROCK;
                        chunkData.setBlock(x, y, z, blockToPlace);

                    } else {
                        solidDepth = 0;
                        // Su sadece deniz seviyesine kadar doldurulur
                        if (!hasHitSurface && y <= GLOBAL_SEA_LEVEL) {
                            chunkData.setBlock(x, y, z, Material.WATER);
                        } else if (hasHitSurface && y <= tData.baseLavaLevel) {
                            chunkData.setBlock(x, y, z, Material.LAVA);
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
        private final List<Integer> weights    = new ArrayList<>();
        private int totalWeight = 0;

        public RandomBlockSelector(Object obj, Material defaultMat) {
            List<String> list = new ArrayList<>();
            if (obj instanceof List)       { list = (List<String>) obj; }
            else if (obj instanceof String){ list.add((String) obj); }
            if (list.isEmpty()) {
                materials.add(defaultMat); weights.add(100); totalWeight = 100; return;
            }
            for (String s : list) {
                String[] parts = s.split(":");
                Material mat = null; int weight = 100;
                try {
                    String cleanName = parts[0].replace("minecraft:", "").toUpperCase();
                    if (parts.length == 3) {
                        cleanName = parts[1].replace("minecraft:", "").toUpperCase();
                        weight = Integer.parseInt(parts[2]);
                    } else if (parts.length == 2 && !s.startsWith("minecraft:")) {
                        weight = Integer.parseInt(parts[1]);
                    }
                    mat = Material.matchMaterial(cleanName);
                } catch (Exception ignored) {}
                if (mat == null) mat = defaultMat;
                materials.add(mat); weights.add(weight); totalWeight += weight;
            }
        }

        public Material getRandom(Random random) {
            if (totalWeight <= 0) return materials.get(0);
            int r = random.nextInt(totalWeight), count = 0;
            for (int i = 0; i < materials.size(); i++) {
                count += weights.get(i);
                if (r < count) return materials.get(i);
            }
            return materials.get(0);
        }

        public boolean contains(Material mat) { return materials.contains(mat); }
    }

    // ==========================================================
    // BİYOM VERİ OBJESİ
    // ==========================================================
    public static class TerrainData {
        public final String  name;
        public final int     baseHeight;
        public final int     heightVariation;
        public final double  roughness;
        public final int     waterLevel;   // nehir/okyanus iç hesapları için
        public final int     subDepth;
        public final boolean isWaterBiome; // true → su seviyesi koruması devredışı

        public final String caveMode;
        public final double caveFreq;
        public final double caveThreshold;
        public final double spaghettiThreshold;
        public final int    baseLavaLevel;

        public final boolean riverEnabled;
        public final double  riverWidth;
        public final int     riverDepth;
        public final String  terrainType;

        // Kanyon
        public final boolean canyonEnabled;
        public final double  canyonFreq;
        public final double  canyonWidth;
        public final int     canyonDepth;

        // Yanardağ
        public final boolean volcanoActive;
        public final int     volcanoLavaLevel;
        public final int     volcanoConeRadius;
        public final int     volcanoCraterRadius;

        // Yüzer ada
        public final boolean floatingIslandsEnabled;
        public final int     floatingIslandMinY;
        public final int     floatingIslandMaxY;

        // Okyanus hendeği
        public final boolean trenchEnabled;
        public final int     trenchExtraDepth;

        // Buzul
        public final boolean glacierEnabled;

        public final BlockLayer surfaceLayer;
        public final BlockLayer subLayer;
        public final RandomBlockSelector deepBlock;

        public final OreManager.OreProfile  oreProfile;
        public final TreeManager.TreeProfile treeProfile;
        public final YamlConfiguration rawConfig;

        public TerrainData(YamlConfiguration cfg, WorldGen3D plugin) {
            this.rawConfig = cfg;
            this.name           = cfg.getString("name", "Flat_Fallback");
            this.baseHeight     = cfg.getInt("terrain.base-height", 68);
            this.heightVariation= cfg.getInt("terrain.height-variation", 28);
            this.roughness      = cfg.getDouble("terrain.roughness", 0.010);
            this.terrainType    = cfg.getString("terrain.type", "NORMAL").toUpperCase();
            this.waterLevel     = cfg.getInt("terrain.water-level", GLOBAL_SEA_LEVEL);
            this.subDepth       = cfg.getInt("blocks.sub-depth", 4);

            // Su biyomu tespiti: isim veya açık flag
            String lower = this.name.toLowerCase();
            this.isWaterBiome = cfg.getBoolean("terrain.is-water-biome",
                    lower.contains("ocean") || lower.contains("beach"));

            this.riverEnabled  = cfg.getBoolean("river.enabled", false);
            this.riverWidth    = cfg.getDouble("river.width", 0.06);
            this.riverDepth    = cfg.getInt("river.depth", 14);

            this.caveMode            = cfg.getString("caves.mode", "BOTH");
            this.caveFreq            = cfg.getDouble("caves.noise-frequency", 0.045);
            this.caveThreshold       = cfg.getDouble("caves.threshold", 0.58);
            this.spaghettiThreshold  = cfg.getDouble("caves.spaghetti-threshold", 0.025);
            this.baseLavaLevel       = cfg.getInt("caves.base-lava-level", -54);

            // Kanyon
            this.canyonEnabled = cfg.getBoolean("canyon.enabled", false);
            this.canyonFreq    = cfg.getDouble("canyon.noise-frequency", 0.0018);
            this.canyonWidth   = cfg.getDouble("canyon.width", 0.055);
            this.canyonDepth   = cfg.getInt("canyon.depth", 60);

            // Yanardağ
            this.volcanoActive       = cfg.getBoolean("volcano.active", true);
            this.volcanoLavaLevel    = cfg.getInt("volcano.lava-level", 140);
            this.volcanoConeRadius   = cfg.getInt("volcano.cone-radius", 60);
            this.volcanoCraterRadius = cfg.getInt("volcano.crater-radius", 12);

            // Yüzer ada
            this.floatingIslandsEnabled = cfg.getBoolean("floating-islands.enabled", false);
            this.floatingIslandMinY     = cfg.getInt("floating-islands.min-y", 150);
            this.floatingIslandMaxY     = cfg.getInt("floating-islands.max-y", 220);

            // Okyanus hendeği
            this.trenchEnabled    = cfg.getBoolean("trench.enabled", false);
            this.trenchExtraDepth = cfg.getInt("trench.extra-depth", 45);

            // Buzul
            this.glacierEnabled = cfg.getBoolean("glacier.enabled", false);

            RandomBlockSelector surf  = new RandomBlockSelector(cfg.get("blocks.surface"), Material.GRASS_BLOCK);
            RandomBlockSelector rocky = new RandomBlockSelector(cfg.get("blocks.rocky"),   Material.STONE);
            this.surfaceLayer = new StandardLayer(surf, rocky);

            RandomBlockSelector sub = new RandomBlockSelector(cfg.get("blocks.sub"), Material.DIRT);
            this.subLayer   = new StandardLayer(sub, sub);
            this.deepBlock  = new RandomBlockSelector(cfg.get("blocks.deep"), Material.STONE);

            this.oreProfile  = plugin.getOreManager().getProfile(cfg.getString("ore-profile", "default_ores"));
            this.treeProfile = plugin.getTreeManager().getProfile(cfg.getString("tree-profile", "none"));
        }
    }
}
