package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WorldCommand implements SubCommand {

    private final WorldGen3D plugin;

    // ==========================================================
    // YENİ: Bekleyen İşlemler Havuzu (Hata veren kısımlar burasıydı)
    // ==========================================================
    public static final Map<UUID, PendingDelete> pendingDeletions = new HashMap<>();

    public WorldCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "world"; }

    @Override
    public String getDescription() { return "- Dunya yonetimi (Olustur, Sil, Isinlan)."; }

    @Override
    public String getSyntax() { return "/wgen world <dunya_ismi> <tp|delete>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 3) {
            player.sendMessage(prefix + "§cKullanim: §e" + getSyntax());
            return;
        }

        String targetWorldName = args[1];
        String action = args[2].toLowerCase();
        World targetWorld = Bukkit.getWorld(targetWorldName);

        if (action.equals("tp")) {
            if (targetWorld == null) {
                player.sendMessage(prefix + "§c" + targetWorldName + " adinda yuklu bir dunya bulunamadi!");
                return;
            }
            player.teleport(targetWorld.getSpawnLocation());
            player.sendMessage(prefix + "§a" + targetWorldName + " dunyasina isinlandin.");
        }
        else if (action.equals("delete")) {
            // 4 Haneli rastgele güvenlik kodu üretiliyor
            String code = String.format("%04d", new Random().nextInt(10000));

            // Oyuncunun isteği hafızaya alınıyor
            pendingDeletions.put(player.getUniqueId(), new PendingDelete(targetWorldName, code));

            player.sendMessage(prefix + "§cDIKKAT: §e" + targetWorldName + " §cdunyasini fiziksel olarak silmek uzeresin!");
            player.sendMessage(prefix + "§7Onaylamak icin su komutu gir: §a/wgen confirm " + code);
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                worlds.add(w.getName());
            }
            return worlds;
        }
        if (args.length == 3) {
            return Arrays.asList("tp", "delete");
        }
        return new ArrayList<>();
    }

    // ==========================================================
    // YENİ: Hafızada tutulacak obje sınıfı
    // ==========================================================
    public static class PendingDelete {
        public final String worldName;
        public final String code;

        public PendingDelete(String worldName, String code) {
            this.worldName = worldName;
            this.code = code;
        }
    }
}