package me.maykitron.worldgen3d.generator;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    // İçini tamamen boş bırakıyoruz ki dünya sadece HAVADAN oluşsun!
    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // No blocks, just pure void.
    }
}