package dev.einselbst.ghostdash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GhostDashPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private GhostDashManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new GhostDashManager(this, GhostDashSettings.from(getConfig()));
        getServer().getPluginManager().registerEvents(new GhostDashListener(manager), this);
        if (getCommand("ghostdash") != null) {
            getCommand("ghostdash").setExecutor(this);
            getCommand("ghostdash").setTabCompleter(this);
        }
        manager.startTicker();
        getLogger().info("GhostDash enabled for Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (subcommand) {
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("GhostDash is running. Active sessions: " + manager.activeCount());
                    return true;
                }
                player.sendMessage(manager.statusMessage(player));
                return true;
            }
            case "cancel" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can cancel their own Ghost Dash.");
                    return true;
                }
                if (!manager.cancel(player, true, "Ghost Dash abgebrochen.")) {
                    player.sendMessage(Component.text("Du bist gerade nicht im Ghost-Zustand.", NamedTextColor.GRAY));
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ghostdash.admin")) {
                    sender.sendMessage(Component.text("Dafür fehlt dir die Berechtigung.", NamedTextColor.RED));
                    return true;
                }
                manager.cancelAll(true, "Ghost Dash wurde neu geladen.");
                reloadConfig();
                manager.updateSettings(GhostDashSettings.from(getConfig()));
                sender.sendMessage(Component.text("GhostDash-Konfiguration neu geladen.", NamedTextColor.GREEN));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Benutzung: /ghostdash <status|cancel|reload>", NamedTextColor.YELLOW));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return sender.hasPermission("ghostdash.admin")
                ? List.of("status", "cancel", "reload")
                : List.of("status", "cancel");
    }
}
