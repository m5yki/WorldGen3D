package me.maykitron.worldgen3d.manager;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.commands.ConfirmCommand;
import me.maykitron.worldgen3d.commands.CreatorCommand;
import me.maykitron.worldgen3d.commands.InfoCommand;
import me.maykitron.worldgen3d.commands.LocateCommand;
import me.maykitron.worldgen3d.commands.ReloadCommand;
import me.maykitron.worldgen3d.commands.SaveCommand;
import me.maykitron.worldgen3d.commands.SchematicCommand;
import me.maykitron.worldgen3d.commands.SubCommand;
import me.maykitron.worldgen3d.commands.TestCommand;
import me.maykitron.worldgen3d.commands.WorldCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    // Komutları hafızada tutan listemiz
    private final ArrayList<SubCommand> subCommands = new ArrayList<>();
    private final WorldGen3D plugin;

    public CommandManager(WorldGen3D plugin) {
        this.plugin = plugin;

        // Eski Komutlar
        subCommands.add(new InfoCommand(plugin));
        subCommands.add(new ReloadCommand(plugin));
        subCommands.add(new LocateCommand(plugin));
        subCommands.add(new SchematicCommand(plugin));
        subCommands.add(new WorldCommand(plugin));
        subCommands.add(new ConfirmCommand(plugin));
        subCommands.add(new TestCommand(plugin));

        // YENİ EKLENEN LABORATUVAR KOMUTLARI
        subCommands.add(new CreatorCommand(plugin));
        subCommands.add(new SaveCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            for (SubCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.getName())) {
                    subCommand.perform(sender, args);
                    return true;
                }
            }
        }

        // Eğer komut yanlış girilirse veya sadece /wgen yazılırsa Yardım Menüsünü göster
        sender.sendMessage(plugin.getLangManager().getMessage("prefix") + "§eMevcut Komutlar:");
        for (SubCommand subCommand : subCommands) {
            sender.sendMessage("§7- §a" + subCommand.getSyntax() + " §8| §7" + subCommand.getDescription());
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        // İlk argüman (/wgen <komut>)
        if (args.length == 1) {
            List<String> subCommandNames = new ArrayList<>();
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    subCommandNames.add(subCommand.getName());
                }
            }
            return subCommandNames;
        }
        // İkinci ve sonraki argümanlar alt komutların kendi içindeki Tab metoduna gönderilir
        else if (args.length > 1) {
            for (SubCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.getName())) {
                    return subCommand.getSubcommandArguments(sender, args);
                }
            }
        }
        return new ArrayList<>();
    }
}