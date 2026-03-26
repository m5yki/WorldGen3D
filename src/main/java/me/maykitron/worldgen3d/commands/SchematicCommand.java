package me.maykitron.worldgen3d.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.maykitron.worldgen3d.WorldGen3D;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.worldedit.math.Vector3;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchematicCommand implements SubCommand {

    private final WorldGen3D plugin;

    public SchematicCommand(WorldGen3D plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "schematic";
    }

    @Override
    public String getDescription() {
        return "Sematikleri listeler, inceler veya dunyaya dondurerek yapistirir.";
    }

    @Override
    public String getSyntax() {
        return "/wgen schematic <list/weight/load>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String prefix = plugin.getLangManager().getMessage("prefix");

        if (args.length < 2) {
            player.sendMessage("§cKullanim: §e/wgen schematic <list/weight/load>");
            return;
        }

        String action = args[1].toLowerCase();

        // 1. LİSTELEME
        if (action.equals("list")) {
            File schemFolder = new File(plugin.getDataFolder(), "data/schematics/vanilla");
            if (!schemFolder.exists() || schemFolder.listFiles() == null) {
                player.sendMessage(prefix + "§cHic sematik bulunamadi!");
                return;
            }
            player.sendMessage("§8§m----------------------------------");
            player.sendMessage("§b§lKAYITLI ŞEMATİKLER:");
            for (File f : schemFolder.listFiles()) {
                if (f.getName().endsWith(".schematic")) {
                    player.sendMessage("§7- §f" + f.getName());
                }
            }
            player.sendMessage("§8§m----------------------------------");
        }

        // 2. AĞIRLIK (BOYUT) ÖLÇME
        else if (action.equals("weight")) {
            if (args.length < 3) {
                player.sendMessage("§cKullanim: §e/wgen schematic weight <dosya_adi>");
                return;
            }
            Clipboard clipboard = getClipboard("vanilla/" + args[2]);
            if (clipboard == null) {
                player.sendMessage(prefix + "§cBu sematik okunamadi veya yok!");
                return;
            }

            BlockVector3 dims = clipboard.getDimensions();
            BlockVector3 origin = clipboard.getOrigin();

            player.sendMessage("§8§m----------------------------------");
            player.sendMessage("§b§lŞEMATİK ANALİZİ: §f" + args[2]);
            player.sendMessage("§7Boyutlar (X, Y, Z): §a" + dims.x() + "§8x§a" + dims.y() + "§8x§a" + dims.z());
            player.sendMessage("§7Merkez (Origin): §e" + origin.x() + "§8, §e" + origin.y() + "§8, §e" + origin.z());
            player.sendMessage("§7Toplam Blok: §f" + (dims.x() * dims.y() * dims.z()));
            player.sendMessage("§8§m----------------------------------");
        }

        // 3. GELİŞMİŞ YÜKLEME (Load / Paste)
        else if (action.equals("load")) {
            if (args.length < 5) {
                player.sendMessage("§cKullanim: §e/wgen schematic load <dosya> <0/90/180/270> <normal/mirror>");
                return;
            }

            String fileName = "vanilla/" + args[2];
            int rotation = Integer.parseInt(args[3]);
            boolean isMirror = args[4].equalsIgnoreCase("mirror");

            Clipboard clipboard = getClipboard(fileName);
            if (clipboard == null) {
                player.sendMessage(prefix + "§cŞematik bulunamadı!");
                return;
            }

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);

                // Mimarın Karizması: Döndürme ve Aynalama Matematiği (AffineTransform)
                AffineTransform transform = new AffineTransform();
                transform = transform.rotateY(-rotation); // Sağa doğru dönüş

                if (isMirror) {
                    // YENİ: BlockVector3 yerine Vector3 kullanıyoruz!
                    transform = transform.scale(Vector3.at(-1, 1, 1));
                }

                holder.setTransform(holder.getTransform().combine(transform));

                Operation operation = holder.createPaste(editSession)
                        .to(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()))
                        .ignoreAirBlocks(true)
                        .build();

                Operations.complete(operation);
                player.sendMessage(prefix + "§aŞematik başarıyla yapıştırıldı! §7(Açı: " + rotation + " | Ayna: " + isMirror + ")");
            } catch (Exception e) {
                player.sendMessage(prefix + "§cYapıştırırken hata oluştu: " + e.getMessage());
            }
        }
    }

    private Clipboard getClipboard(String path) {
        File file = plugin.getStructureManager().getSchematicFile(path);
        if (file == null || !file.exists()) return null;
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return null;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("list", "weight", "load");
        }
        else if (args.length == 3 && (args[1].equalsIgnoreCase("weight") || args[1].equalsIgnoreCase("load"))) {
            // Şematik klasöründeki dosyaları öner
            List<String> files = new ArrayList<>();
            File schemFolder = new File(plugin.getDataFolder(), "data/schematics/vanilla");
            if (schemFolder.exists() && schemFolder.listFiles() != null) {
                for (File f : schemFolder.listFiles()) {
                    if (f.getName().endsWith(".schematic")) files.add(f.getName());
                }
            }
            return files;
        }
        else if (args.length == 4 && args[1].equalsIgnoreCase("load")) {
            return Arrays.asList("0", "90", "180", "270"); // Açı önerisi
        }
        else if (args.length == 5 && args[1].equalsIgnoreCase("load")) {
            return Arrays.asList("normal", "mirror"); // Ayna önerisi
        }
        return new ArrayList<>();
    }
}