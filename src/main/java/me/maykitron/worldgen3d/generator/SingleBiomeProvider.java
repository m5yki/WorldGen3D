package me.maykitron.worldgen3d.generator;

import me.maykitron.worldgen3d.WorldGen3D;

/**
 * WorldGen3D - İzole Test Odası Biyom Sağlayıcısı
 * Karmaşık gürültüleri ezip, tüm dünyaya sadece tek bir biyomu dayatır!
 */
public class SingleBiomeProvider extends CustomBiomeProvider {

    private final String fixedBiome;

    public SingleBiomeProvider(WorldGen3D plugin, String fixedBiome) {
        super(plugin);
        this.fixedBiome = fixedBiome;
    }

    @Override
    public String getCustomBiomeName(int x, int z) {
        // Normalde burada matematiksel hesaplar olurdu,
        // ama test dünyasında nereye gidersek gidelim sadece hedef biyomu döndürüyoruz!
        return fixedBiome;
    }
}