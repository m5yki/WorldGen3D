package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.manager.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WorldCommand implements SubCommand {

    private final WorldGen3D plugin;

    public WorldCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "world"; }

    @Override
    public String getDescription() { return "- Bir test dunyasina isinlanir veya guvenlice siler."; }

    @Override
    public String getSyntax() { return "/wgen world <dunya> <teleport|delete>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 3) {
            player.sendMessage(prefix + "§cKullanım: §e" + getSyntax());
            return;
        }

        String targetWorldName = args[1];
        World targetWorld = Bukkit.getWorld(targetWorldName);
        String action = args[2].toLowerCase();

        if (targetWorld == null) {
            player.sendMessage(prefix + "§c" + targetWorldName + " adında bir dünya bulunamadı!");
            return;
        }

        if (action.equals("teleport")) {
            int highestY = targetWorld.getHighestBlockYAt(0, 0);
            player.teleport(new Location(targetWorld, 0.5, highestY + 2, 0.5));
            player.sendMessage(prefix + "§a" + targetWorldName + " dünyasına ışınlandın!");
        }
        else if (action.equals("delete")) {
            // İÇERİDE BİRİ VAR MI KONTROLÜ
            if (!targetWorld.getPlayers().isEmpty()) {
                player.sendMessage(prefix + "§cBu dünya silinemez! İçeride " + targetWorld.getPlayers().size() + " aktif oyuncu var.");
                return;
            }

            // GÜVENLİK KODU ÜRETME (123 ile 987 arası)
            int randomCode = 123 + new Random().nextInt(865);
            CommandManager.pendingDeletions.put(player, new CommandManager.PendingDelete(targetWorldName, randomCode));

            player.sendMessage(prefix + "§eDİKKAT! §c" + targetWorldName + " §edünyasını tamamen silmek üzeresin!");
            player.sendMessage(prefix + "§eOnaylamak için sohbete şunu yaz: §a/wgen confirm " + randomCode);
        } else {
            player.sendMessage(prefix + "§cGeçersiz işlem. Sadece 'teleport' veya 'delete' kullanabilirsin.");
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().startsWith("wg3d_test_")) worlds.add(w.getName());
            }
            return worlds;
        } else if (args.length == 3) {
            return Arrays.asList("teleport", "delete");
        }
        return new ArrayList<>();
    }
}