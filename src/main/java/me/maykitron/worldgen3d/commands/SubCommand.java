package me.maykitron.worldgen3d.commands;

import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * WorldGen3D - Alt Komut Şablonu
 * Tüm komutlar (info, reload, debug) bu şablondan türetilecek.
 */
public interface SubCommand {

    String getName(); // Komutun adı (örn: info)

    String getDescription(); // Komutun ne işe yaradığı

    String getSyntax(); // Kullanım şekli (örn: /wgen info <biyom>)

    void perform(CommandSender sender, String[] args); // Komut çalışınca ne olacak?

    List<String> getSubcommandArguments(CommandSender sender, String[] args); // Tab'a basınca ne önerecek?
}