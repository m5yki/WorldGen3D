package me.maykitron.worldgen3d.commands; // Kendi klasör yoluna göre burayı kontrol et

import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final WorldGen3D plugin;

    public ReloadCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Motorun YML, Dil ve Biyom ayarlarini sunucuyu kapatmadan yeniler.";
    }

    @Override
    public String getSyntax() {
        return "/wgen reload";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        // Dil dosyasından prefix'i çekip oyuncuya haber veriyoruz
        String prefix = plugin.getLangManager().getMessage("prefix");
        sender.sendMessage(prefix + "§eMotor ayarlari yenileniyor, lutfen bekleyin...");

        // 1. Ana config.yml dosyasını yenile
        plugin.reloadConfig();

        // 2. Özel motor fonksiyonumuzu çağırarak RAM'deki biyomları temizleyip baştan oku
        plugin.reloadEngine();

        sender.sendMessage(prefix + "§aWorldGen3D motoru ve YML dosyalari basariyla yenilendi!");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        // Tab tuşuna basınca ekstra bir kelime önermesine gerek yok
        return new ArrayList<>();
    }
}