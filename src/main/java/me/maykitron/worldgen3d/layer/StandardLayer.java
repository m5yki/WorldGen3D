package me.maykitron.worldgen3d.layer;

import me.maykitron.worldgen3d.context.BlockContext;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator.RandomBlockSelector;
import org.bukkit.Material;

public class StandardLayer implements BlockLayer {

    private final RandomBlockSelector defaultBlocks;
    private final RandomBlockSelector rockyBlocks;

    public StandardLayer(RandomBlockSelector defaultBlocks, RandomBlockSelector rockyBlocks) {
        this.defaultBlocks = defaultBlocks;
        this.rockyBlocks = rockyBlocks;
    }

    @Override
    public Material getBlock(BlockContext ctx) {
        // Eğer yer çok dikse (dağ yamacıysa) çimen veya kum tutunamaz, taşlık olur!
        if (ctx.slope > 1.3 && rockyBlocks != null) {
            return rockyBlocks.getRandom(ctx.random);
        }

        // Eğer su seviyesindeyse kumsal (kum/çakıl) olsun
        if (ctx.isNearWater) {
            return ctx.random.nextBoolean() ? Material.SAND : Material.GRAVEL;
        }

        // Normal bir yerse, YML'den okunan varsayılan blokları (örn: çimen) ver
        return defaultBlocks.getRandom(ctx.random);
    }
}