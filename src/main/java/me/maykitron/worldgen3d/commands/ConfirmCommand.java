package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ConfirmCommand implements SubCommand {

    private final WorldGen3D plugin;

    public ConfirmCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "confirm"; }

    @Override
    public String getDescription() { return "- Dunya silme islemini onaylar."; }

    @Override
    public String getSyntax() { return "/wgen confirm <kod>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 2) {
            player.sendMessage(prefix + "§cKullanim: §e" + getSyntax());
            return;
        }

        String inputCode = args[1];

        // WorldCommand sınıfındaki hafızadan oyuncunun verisini çekiyoruz
        WorldCommand.PendingDelete pending = WorldCommand.pendingDeletions.get(player.getUniqueId());

        if (pending == null) {
            player.sendMessage(prefix + "§cOnay bekleyen bir islem bulunamadi.");
            return;
        }

        if (!pending.code.equals(inputCode)) {
            player.sendMessage(prefix + "§cHatalı onay kodu girdiniz!");
            return;
        }

        String worldName = pending.worldName;

        // İşlem onaylandığı için hafızadan temizliyoruz
        WorldCommand.pendingDeletions.remove(player.getUniqueId());

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // İçerideki oyuncuları güvenli bir yere tahliye et
            World mainWorld = Bukkit.getWorlds().get(0);
            for (Player p : world.getPlayers()) {
                p.teleport(mainWorld.getSpawnLocation());
                p.sendMessage(prefix + "§cBulundugunuz dunya silindigi icin ana dunyaya isinlandiniz!");
            }
            // Dünyayı RAM'den çıkart
            Bukkit.unloadWorld(world, false);
        }

        // ==========================================================
        // DÜNYAYI DİSKTEN (FİZİKSEL OLARAK) SİL
        // ==========================================================
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (deleteDirectory(worldFolder)) {
            player.sendMessage(prefix + "§a" + worldName + " dunyasi diskten basariyla silindi!");
        } else {
            player.sendMessage(prefix + "§cDunya klasoru silinirken bir hata olustu. Dosyalar isletim sistemi tarafindan kilitli olabilir.");
        }

        // Multiverse-Core yüklüyse, onun sisteminden de kaydını düş
        if (plugin.isMultiverseHooked()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + worldName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
        }
    }

    // Klasörleri içindeki dosyalarla birlikte silmeye yarayan tehlikeli ama gerekli metod
    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return path.delete();
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}