package me.maykitron.worldgen3d.layer;

import me.maykitron.worldgen3d.context.BlockContext;
import org.bukkit.Material;

public interface BlockLayer {
    Material getBlock(BlockContext ctx);
}