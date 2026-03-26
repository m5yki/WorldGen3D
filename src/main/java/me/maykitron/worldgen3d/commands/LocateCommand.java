package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocateCommand implements SubCommand {

    private final WorldGen3D plugin;

    public LocateCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public String getDescription() {
        return "En yakin Biyomu veya Yapiyi (Structure) bulur.";
    }

    @Override
    public String getSyntax() {
        return "/wgen locate <biome/structure> <isim>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cBu komutu sadece oyuncular kullanabilir.");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage("§cKullanim: §e" + getSyntax());
            return;
        }

        String type = args[1].toLowerCase();
        String targetName = args[2].toLowerCase();
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (type.equals("biome")) {
            player.sendMessage(prefix + "§eRadar devrede! §a" + targetName + " §ebiyomu araniyor...");

            // Asenkron Radar (Sunucu cökmesin diye arka planda arar)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                CustomBiomeProvider provider = new CustomBiomeProvider(plugin);
                int px = player.getLocation().getBlockX();
                int pz = player.getLocation().getBlockZ();

                // Sarmal (Spiral) Arama Algoritmasi
                int maxRadius = 10000; // Maksimum 10.000 blok uzaga bakar
                int step = 32; // Her 32 blokta bir numune alir (Hizli arama)

                for (int radius = 0; radius <= maxRadius; radius += step) {
                    for (int x = -radius; x <= radius; x += step) {
                        for (int z = -radius; z <= radius; z += step) {
                            if (Math.abs(x) == radius || Math.abs(z) == radius) {
                                int searchX = px + x;
                                int searchZ = pz + z;

                                String foundBiome = provider.getCustomBiomeName(searchX, searchZ);
                                if (foundBiome.replace(" ", "").equalsIgnoreCase(targetName) ||
                                        foundBiome.toLowerCase().contains(targetName)) {

                                    int finalX = searchX;
                                    int finalZ = searchZ;
                                    double distance = player.getLocation().distance(new Location(player.getWorld(), finalX, player.getLocation().getY(), finalZ));

                                    // Sonucu ana ekrana yolla
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        player.sendMessage(prefix + "§aBiyom Bulundu! §7(" + (int)distance + " blok uzakta)");
                                        player.sendMessage("§7Kordinatlar: §eX: " + finalX + " §7| §eZ: " + finalZ);
                                        player.sendMessage("§8İpucu: /tp " + finalX + " ~ " + finalZ);
                                    });
                                    return; // Bulduk, radari kapat
                                }
                            }
                        }
                    }
                }

                // Bulamazsa:
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(prefix + "§c10.000 blok capinda '" + targetName + "' bulunamadi!");
                });
            });

        } else if (type.equals("structure")) {
            player.sendMessage(prefix + "§cStructure radar sistemi yakinda eklenecek!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("biome", "structure");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("biome")) {
            List<String> biomes = new ArrayList<>();
            for (File file : plugin.getPackManager().getLoadedBiomes()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                biomes.add(config.getString("name", file.getName().replace(".yml", "")).replace(" ", ""));
            }
            return biomes;
        }
        return new ArrayList<>();
    }
}