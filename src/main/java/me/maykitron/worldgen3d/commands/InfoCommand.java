package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InfoCommand implements SubCommand {

    private final WorldGen3D plugin;

    public InfoCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Belirtilen biyomun matematiksel ve YML verilerini gosterir.";
    }

    @Override
    public String getSyntax() {
        return "/wgen info <biyom>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        // Eğer adam sadece "/wgen info" yazdıysa uyar
        if (args.length < 2) {
            sender.sendMessage(plugin.getLangManager().getMessage("prefix") + "§cKullanim: §e" + getSyntax());
            return;
        }

        String targetBiome = args[1].toLowerCase();

        // RAM'e yüklenen tüm biyom dosyalarını gez
        for (File file : plugin.getPackManager().getLoadedBiomes()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String configName = config.getString("name", "Bilinmeyen").toLowerCase();
            String fileName = file.getName().replace(".yml", "").toLowerCase();

            // İster dosya adını (plains) ister YML içindeki adı (Ova) yazsın, buluruz!
            if (configName.equals(targetBiome) || fileName.equals(targetBiome)) {

                // Karizmatik Bilgi Tablosu
                sender.sendMessage("§8§m----------------------------------------");
                sender.sendMessage("§b§lBİYOM PROFİLİ: §f§l" + config.getString("name"));
                sender.sendMessage("§7Dosya: §e" + file.getName());
                sender.sendMessage("§7Renk Stili: §a" + config.getString("color-style", "NORMAL"));
                sender.sendMessage("");
                sender.sendMessage("§e[Matematik & Gürültü]");
                sender.sendMessage("§7  Sıcaklık: §c" + config.getDouble("temperature") + " §8| §7Nem: §b" + config.getDouble("humidity"));
                sender.sendMessage("§7  Temel Yükseklik: §f" + config.getInt("terrain.base-height"));
                sender.sendMessage("§7  Dalgalanma: §f" + config.getDouble("terrain.roughness"));
                sender.sendMessage("§7  Yükseklik Farkı: §f" + config.getInt("terrain.height-variation"));
                sender.sendMessage("");
                sender.sendMessage("§a[Bloklar & Yüzey]");
                sender.sendMessage("§7  Yüzey: §f" + config.getString("blocks.surface"));
                sender.sendMessage("§7  Toprak: §f" + config.getString("blocks.sub"));
                sender.sendMessage("§7  Zemin: §f" + config.getString("blocks.deep"));
                sender.sendMessage("§8§m----------------------------------------");
                return;
            }
        }

        // Bulamazsa hata ver
        sender.sendMessage(plugin.getLangManager().getMessage("prefix") + "§cBu isimde bir biyom bulunamadi: §e" + targetBiome);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        // Tab tuşuna basıldığında oyuncuya YML içindeki isimleri öneririz (Ova, Col, Orman vb.)
        if (args.length == 2) {
            List<String> biomeNames = new ArrayList<>();
            for (File file : plugin.getPackManager().getLoadedBiomes()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                biomeNames.add(config.getString("name", file.getName().replace(".yml", "")));
            }
            return biomeNames;
        }
        return null; // Başka argüman yok
    }
}