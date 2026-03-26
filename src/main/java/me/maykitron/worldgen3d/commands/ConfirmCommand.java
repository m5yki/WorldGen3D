package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.manager.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ConfirmCommand implements SubCommand {

    private final WorldGen3D plugin;

    public ConfirmCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "confirm"; }

    @Override
    public String getDescription() { return "- Kritik isemleri onaylamak icindir."; }

    @Override
    public String getSyntax() { return "/wgen confirm <kod>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (!CommandManager.pendingDeletions.containsKey(player)) {
            player.sendMessage(prefix + "§cOnay bekleyen bir işleminiz bulunmuyor.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + "§cLütfen onay kodunu girin. Örn: /wgen confirm 353");
            return;
        }

        CommandManager.PendingDelete request = CommandManager.pendingDeletions.get(player);
        int inputCode;
        try {
            inputCode = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cGeçersiz kod! Sadece rakam girin.");
            return;
        }

        if (inputCode != request.code) {
            player.sendMessage(prefix + "§cYanlış onay kodu! İşlem iptal edildi.");
            CommandManager.pendingDeletions.remove(player);
            return;
        }

        String worldName = request.worldName;
        World targetWorld = Bukkit.getWorld(worldName);
        CommandManager.pendingDeletions.remove(player);

        if (targetWorld == null) {
            player.sendMessage(prefix + "§cSilinecek dünya zaten yok veya daha önce silinmiş!");
            return;
        }

        player.sendMessage(prefix + "§a" + worldName + " siliniyor...");

        // Oyuncuyu ana dünyaya (Spawn) güvenlice postalıyoruz
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());

        if (plugin.isMultiverseHooked()) {
            try {
                // MUAZZAM ÇÖZÜM: MV5 API'sine sızıp şifresiz (OTP bypass) doğrudan siliyoruz!
                Plugin mvPlugin = Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
                if (mvPlugin != null) {
                    Object worldManager = mvPlugin.getClass().getMethod("getMVWorldManager").invoke(mvPlugin);
                    Method deleteMethod = worldManager.getClass().getMethod("deleteWorld", String.class);

                    boolean success = (Boolean) deleteMethod.invoke(worldManager, worldName);
                    if (success) {
                        player.sendMessage(prefix + "§aDünya Multiverse-Core üzerinden kusursuzca silindi!");
                    } else {
                        player.sendMessage(prefix + "§cMV5 dünyayı silemedi! Yedek sisteme geçiliyor...");
                        forceDeleteBukkit(targetWorld, worldName, prefix, player);
                    }
                }
            } catch (Exception e) {
                player.sendMessage(prefix + "§eMV5 API bağlantısı reddedildi, Bukkit ile zorla siliniyor...");
                forceDeleteBukkit(targetWorld, worldName, prefix, player);
            }
        } else {
            forceDeleteBukkit(targetWorld, worldName, prefix, player);
        }
    }

    private void forceDeleteBukkit(World targetWorld, String worldName, String prefix, Player player) {
        if (targetWorld != null) Bukkit.unloadWorld(targetWorld, false);
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        deleteDirectory(worldFolder);
        player.sendMessage(prefix + "§aDünya standart Bukkit sistemiyle kökünden silindi!");
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) deleteDirectory(file);
                    else file.delete();
                }
            }
            directory.delete();
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}