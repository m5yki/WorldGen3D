package me.maykitron.worldgen3d.commands;

import me.maykitron.worldgen3d.WorldGen3D;
import me.maykitron.worldgen3d.generator.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CreatorCommand implements SubCommand {

    private final WorldGen3D plugin;

    public CreatorCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "creator"; }

    @Override
    public String getDescription() { return "- Ozel yapi insa etmek icin bos bir laboratuvara isinlar."; }

    @Override
    public String getSyntax() { return "/wgen creator"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        String worldName = "wg3d_creator";
        World creatorWorld = Bukkit.getWorld(worldName);

        // Eğer dünya daha önce oluşturulmadıysa, oluştur!
        if (creatorWorld == null) {
            player.sendMessage(prefix + "§eLaboratuvar (Void) dunya olusturuluyor, lutfen bekle...");
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidGenerator());
            creatorWorld = wc.createWorld();

            if (creatorWorld != null) {
                // Oyuncunun uzaklaşmasını engellemek için Dünya Sınırı (WorldBorder)
                WorldBorder border = creatorWorld.getWorldBorder();
                border.setCenter(0.5, 0.5);
                border.setSize(256); // 16x16 Chunk alan
            }
        }

        if (creatorWorld == null) {
            player.sendMessage(prefix + "§cLaboratuvar olusturulamadi!");
            return;
        }

        // ==========================================================
        // 0, 0, 0 NOKTASINA PİVOT (MERKEZ) PLATFORMU İNŞA ET
        // ==========================================================
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                creatorWorld.getBlockAt(x, 0, z).setType(Material.WHITE_STAINED_GLASS);
            }
        }
        // Tam merkeze (0,0,0) dikkat çekici bir elmas blok koyuyoruz!
        creatorWorld.getBlockAt(0, 0, 0).setType(Material.DIAMOND_BLOCK);

        // Oyuncuyu merkezin hemen üstüne (0.5, 1, 0.5) ışınla
        player.teleport(new Location(creatorWorld, 0.5, 1, 0.5));
        player.sendMessage(prefix + "§aLaboratuvara hos geldin! Yapiyi tam olarak §eElmas Blogun §auzerine insa etmelisin.");
        player.sendMessage(prefix + "§7(Kaydetmek icin WorldEdit baltasiyla secim yapip §e/wgen save <format> <isim> §7yaz)");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}