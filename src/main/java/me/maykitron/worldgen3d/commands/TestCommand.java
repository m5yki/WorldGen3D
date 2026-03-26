package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.generator.SingleBiomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCommand implements SubCommand {

    private final WorldGen3D plugin;

    public TestCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "test"; }

    @Override
    public String getDescription() { return "- 16x16 chunkluk izole bir biyom laboratuvari yaratir."; }

    @Override
    public String getSyntax() { return "/wgen test create <biyom>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 3 || !args[1].equalsIgnoreCase("create")) {
            player.sendMessage(prefix + "§cKullanım: §e" + getSyntax());
            return;
        }

        String targetBiome = args[2];
        String exactBiomeName = null;

        for (File f : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String bName = cfg.getString("name", "");
            if (bName.replace(" ", "").equalsIgnoreCase(targetBiome)) {
                exactBiomeName = bName;
                break;
            }
        }

        if (exactBiomeName == null) {
            player.sendMessage(prefix + "§c" + targetBiome + " adında bir biyom YML dosyalarında bulunamadı!");
            return;
        }

        String worldName = "wg3d_test_" + exactBiomeName.toLowerCase().replace(" ", "_");

        if (Bukkit.getWorld(worldName) != null || new File(Bukkit.getWorldContainer(), worldName).exists()) {
            player.sendMessage(prefix + "§cBu biyomun test dünyası zaten var! Önce §e/wgen world " + worldName + " delete §ccomutuyla silmelisin.");
            return;
        }

        player.sendMessage(prefix + "§a" + exactBiomeName + " §eiçin Laboratuvar oluşturuluyor... Lütfen bekleyin!");

        // %100 ÇÖKMEYEN YÖNTEM: KONSOL TETİKLEYİCİSİ
        if (plugin.isMultiverseHooked()) {
            // Sunucu konsoluna gizlice MV komutu yazdırıyoruz.
            String command = "mv create " + worldName + " normal -g WorldGen3D:test_" + exactBiomeName.replace(" ", "_");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new CustomChunkGenerator(plugin, new SingleBiomeProvider(plugin, exactBiomeName)));
            Bukkit.createWorld(creator);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World targetWorld = Bukkit.getWorld(worldName);
            if (targetWorld != null) {
                WorldBorder border = targetWorld.getWorldBorder();
                border.setCenter(0.5, 0.5);
                border.setSize(256);
                border.setWarningDistance(5);
                border.setDamageAmount(0);

                int highestY = targetWorld.getHighestBlockYAt(0, 0);
                player.teleport(new Location(targetWorld, 0.5, highestY + 2, 0.5));

                if (plugin.isMultiverseHooked()) {
                    player.sendMessage(prefix + "§aLaboratuvara hoş geldin! MV5 ile senkronize edildi.");
                } else {
                    player.sendMessage(prefix + "§aLaboratuvara hoş geldin! Standart mod aktif.");
                }
            } else {
                player.sendMessage(prefix + "§cTest dünyası oluşturulurken gecikme yaşandı!");
            }
        }, 50L); // 2.5 saniye bekleme (MV5'in dünyayı tam oluşturması için güvenli süre)
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) return Arrays.asList("create");
        else if (args.length == 3 && args[1].equalsIgnoreCase("create")) {
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