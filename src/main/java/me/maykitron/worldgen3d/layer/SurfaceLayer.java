package me.maykitron.worldgen3d.layer;

import me.maykitron.worldgen3d.context.BlockContext;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator.RandomBlockSelector;
import org.bukkit.Material;

public class SurfaceLayer implements BlockLayer {

    private final RandomBlockSelector defaultSurface;
    private final RandomBlockSelector rockySurface;

    public SurfaceLayer(RandomBlockSelector defaultSurface, RandomBlockSelector rockySurface) {
        this.defaultSurface = defaultSurface;
        this.rockySurface = rockySurface;
    }

    @Override
    public Material getBlock(BlockContext ctx) {
        // Eğer aşırı dik bir yamaçsa, çimen tutunamaz, taşlık olsun!
        if (ctx.slope > 1.3) {
            return rockySurface.getRandom(ctx.random);
        }

        // Eğer su seviyesindeyse kumsal (kum/çakıl) olsun
        if (ctx.isNearWater) {
            return ctx.random.nextBoolean() ? Material.SAND : Material.GRAVEL;
        }

        // Hiçbir ekstrem durum yoksa, biyomun normal yüzey bloğunu ver
        return defaultSurface.getRandom(ctx.random);
    }
}