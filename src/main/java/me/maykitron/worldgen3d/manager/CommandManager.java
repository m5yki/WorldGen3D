package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.commands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final WorldGen3D plugin;
    private final List<SubCommand> subcommands = new ArrayList<>();

    // YENİ: Bekleyen Onay Kodları Hafızası (Oyuncu -> Kod ve Dünya İsmi)
    public static final Map<Player, PendingDelete> pendingDeletions = new HashMap<>();

    public static class PendingDelete {
        public final String worldName;
        public final int code;
        public PendingDelete(String worldName, int code) {
            this.worldName = worldName;
            this.code = code;
        }
    }

    public CommandManager(WorldGen3D plugin) {
        this.plugin = plugin;

        subcommands.add(new InfoCommand(plugin));
        subcommands.add(new ReloadCommand(plugin));
        subcommands.add(new LocateCommand(plugin));
        subcommands.add(new SchematicCommand(plugin));
        subcommands.add(new TestCommand(plugin));
        subcommands.add(new WorldCommand(plugin));
        subcommands.add(new ConfirmCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // YENİ: Dinamik Yardım Menüsü! Eklenen her komut buraya otomatik yansır.
            sender.sendMessage("§b§lWorldGen3D §8- §7Yönetim Paneli");
            for (SubCommand subCommand : subcommands) {
                sender.sendMessage("§e" + subCommand.getSyntax() + " §8" + subCommand.getDescription());
            }
            sender.sendMessage("§8§m--------------------------------------------------");
            return true;
        }

        for (SubCommand subCommand : subcommands) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                subCommand.perform(sender, args);
                return true;
            }
        }

        sender.sendMessage(plugin.getLangManager().getMessage("prefix") + "§cBöyle bir komut bulunamadı.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommandNames = new ArrayList<>();
            for (SubCommand subCommand : subcommands) {
                subCommandNames.add(subCommand.getName());
            }
            return subCommandNames;
        }

        for (SubCommand subCommand : subcommands) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                return subCommand.getSubcommandArguments(sender, args);
            }
        }
        return new ArrayList<>();
    }
}