package me.maykitron.worldgen3d;

import me.maykitron.worldgen3d.generator.CustomBiomeProvider;
import me.maykitron.worldgen3d.generator.CustomChunkGenerator;
import me.maykitron.worldgen3d.generator.SingleBiomeProvider;
import me.maykitron.worldgen3d.manager.CommandManager;
import me.maykitron.worldgen3d.manager.LangManager;
import me.maykitron.worldgen3d.manager.PackManager;
import me.maykitron.worldgen3d.manager.StructureManager;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGen3D extends JavaPlugin {

    // Eklenti Sistem Değişkenleri
    private boolean hasItemsAdder = false;
    private boolean hasMultiverse = false; // YENİ: MV5 Kontrolcüsü
    private LangManager langManager;
    private StructureManager structureManager;
    private PackManager packManager;
    private ConsoleCommandSender console;

    @Override
    public void onEnable() {
        this.console = Bukkit.getConsoleSender();

        saveDefaultConfig();

        this.langManager = new LangManager(this);
        this.structureManager = new StructureManager(this);
        this.packManager = new PackManager(this);

        printAsciiArt();

        if (!checkDependencies()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        sendConsoleMsg("engine-starting");

        CommandManager cmdManager = new CommandManager(this);
        getCommand("worldgen3d").setExecutor(cmdManager);
        getCommand("worldgen3d").setTabCompleter(cmdManager);

        console.sendMessage("§b==================================================================================");
        sendConsoleMsg("engine-ready");
    }

    @Override
    public void onDisable() {
        if (langManager != null) {
            sendConsoleMsg("engine-disabled");
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        // EĞER MV5 veya Bukkit'ten "test_" ID'si gelirse İZOLE LABORATUVAR devreye girer!
        if (id != null && id.startsWith("test_")) {
            String targetBiome = id.replace("test_", "").replace("_", " ");
            SingleBiomeProvider singleBiome = new SingleBiomeProvider(this, targetBiome);
            return new CustomChunkGenerator(this, singleBiome);
        }

        // Normal bir dünya ise varsayılan motor çalışır
        CustomBiomeProvider biomeProvider = new CustomBiomeProvider(this);
        return new CustomChunkGenerator(this, biomeProvider);
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            sendConsoleMsg("error-missing-fawe");
            return false;
        } else {
            sendConsoleMsg("fawe-hook-success");
        }

        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            hasItemsAdder = true;
            sendConsoleMsg("itemsadder-hook-success");
        } else {
            sendConsoleMsg("error-missing-itemsadder");
        }

        // YENİ: MV5 Kontrolü
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
            hasMultiverse = true;
            console.sendMessage("§a[WorldGen3D] Multiverse-Core bulundu! Dunyalar senkronize edilecek.");
        } else {
            console.sendMessage("§e[WorldGen3D] Multiverse-Core bulunamadi. Bukkit Standart Modu devrede.");
        }

        return true;
    }

    private void sendConsoleMsg(String path) {
        String prefix = langManager.getMessage("prefix");
        String msg = langManager.getMessage(path);
        console.sendMessage(prefix + msg);
    }

    private void printAsciiArt() {
        String[] art = {
                "",
                " ██╗    ██╗ ██████╗ ██████╗ ██╗     ██████╗  ██████╗ ███████╗███╗   ██╗ ██████╗ ██████╗ ",
                " ██║    ██║██╔═══██╗██╔══██╗██║     ██╔══██╗██╔════╝ ██╔════╝████╗  ██║ ╚════██╗██╔══██╗",
                " ██║ █╗ ██║██║   ██║██████╔╝██║     ██║  ██║██║  ███╗█████╗  ██╔██╗ ██║  █████╔╝██║  ██║",
                " ██║███╗██║██║   ██║██╔══██╗██║     ██║  ██║██║   ██║██╔══╝  ██║╚██╗██║  ╚═══██╗██║  ██║",
                " ╚███╔███╔╝╚██████╔╝██║  ██║███████╗██████╔╝╚██████╔╝███████╗██║ ╚████║ ██████╔╝██████╔╝",
                "  ╚══╝╚══╝  ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═════╝  ╚═════╝ ╚══════╝╚═╝  ╚═══╝ ╚═════╝ ╚═════╝ ",
                "                               v" + getDescription().getVersion() + " - By Maykitron",
                "=================================================================================="
        };

        for (String line : art) {
            console.sendMessage("§b" + line);
        }
    }

    public LangManager getLangManager() { return langManager; }
    public StructureManager getStructureManager() { return structureManager; }
    public PackManager getPackManager() { return packManager; }

    public boolean isItemsAdderHooked() { return hasItemsAdder; }
    public boolean isMultiverseHooked() { return hasMultiverse; }

    public void reloadEngine() {
        this.langManager = new LangManager(this);
        this.packManager = new PackManager(this);
        getLogger().info("[WorldGen3D] Motor ayarlari basariyla yenilendi!");
    }
}