package me.maykitron.worldgen3d.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SaveCommand implements SubCommand {

    private final WorldGen3D plugin;

    public SaveCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "save"; }

    @Override
    public String getDescription() { return "- WorldEdit secimini custom sematik/nbt olarak kaydeder."; }

    @Override
    public String getSyntax() { return "/wgen save <schematic|schem|nbt> <isim>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 3) {
            player.sendMessage(prefix + "§cKullanim: §e" + getSyntax());
            return;
        }

        String formatStr = args[1].toLowerCase();
        String fileName = args[2].toLowerCase().replace(" ", "_");

        // WorldEdit API ile oyuncunun secimini al
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        Region region;
        try {
            region = session.getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            player.sendMessage(prefix + "§cHATA: Önce WorldEdit baltası ile kaydedilecek alanı seçmelisin!");
            return;
        }

        // ==========================================================
        // PANOPYA (CLIPBOARD) KOPYALAMA VE PİVOT (0,0,0) MERKEZLEME
        // ==========================================================
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        // EFSANE DETAY: Yapının neresinde olursan ol, yapının merkez (orijin) noktasını
        // kesin ve net bir şekilde 0, 0, 0 (Elmas blok) olarak sabitliyoruz!
        clipboard.setOrigin(BlockVector3.at(0, 0, 0));

        ForwardExtentCopy copy = new ForwardExtentCopy(
                WorldEdit.getInstance().newEditSession(wePlayer.getWorld()),
                region, clipboard, region.getMinimumPoint()
        );
        copy.setCopyingEntities(false); // Canlıları kaydetme

        try {
            Operations.complete(copy);
        } catch (Exception e) {
            player.sendMessage(prefix + "§cKopyalama sirasinda bir hata olustu!");
            return;
        }

        // Hedef klasör: plugins/WorldGen3D/data/schematics/custom
        File folder = new File(plugin.getDataFolder(), "data/schematics/custom");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, fileName + "." + formatStr);

        ClipboardFormat format = ClipboardFormats.findByAlias(formatStr);
        if (format == null) format = ClipboardFormats.findByAlias("schematic"); // Varsayılan

        // Diske Yazdırma İşlemi
        try (ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
            player.sendMessage(prefix + "§aYapi basariyla kaydedildi: §e" + "custom/" + file.getName());
            player.sendMessage(prefix + "§7(Artik bu yolu ağaç/yapi profillerinde kullanabilirsin!)");
        } catch (Exception e) {
            player.sendMessage(prefix + "§cDosya kaydedilirken fiziksel bir hata olustu!");
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) return Arrays.asList("schematic", "schem", "nbt");
        if (args.length == 3) return Arrays.asList("<dosya_ismi>");
        return Collections.emptyList();
    }
}