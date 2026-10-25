package com.supernulled.supastaff;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupaStaff extends JavaPlugin implements Listener {
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> godMode = new HashSet<>();
    private final Set<UUID> staffChat = new HashSet<>();
    private static final Pattern HEX = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPlayerStates();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SupaStaff enabled.");
    }

    @Override
    public void onDisable() {
        savePlayerStates();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer != player) viewer.showPlayer(this, player);
            }
        }
    }

    private void loadPlayerStates() {
        if (getConfig().getBoolean("actions.remember-vanish-on-restart", true)) {
            for (String id : getConfig().getStringList("data.vanished")) {
                try { vanished.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) {}
            }
        }
        if (getConfig().getBoolean("actions.remember-godmode-on-restart", true)) {
            for (String id : getConfig().getStringList("data.godmode")) {
                try { godMode.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void savePlayerStates() {
        getConfig().set("data.vanished", getConfig().getBoolean("actions.remember-vanish-on-restart", true)
                ? vanished.stream().map(UUID::toString).toList() : Collections.emptyList());
        getConfig().set("data.godmode", getConfig().getBoolean("actions.remember-godmode-on-restart", true)
                ? godMode.stream().map(UUID::toString).toList() : Collections.emptyList());
        saveConfig();
    }

    private String color(String input) {
        if (input == null) return "";
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(0)
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(1)
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(2)
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(3)
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(4)
                    + ChatColor.COLOR_CHAR + matcher.group(1).charAt(5));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String msg(String key) {
        return color(getConfig().getString("messages." + key, ""));
    }

    private String format(String key, String... replacements) {
        String value = msg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return value;
    }

    private void tell(CommandSender sender, String message) {
        sender.sendMessage(message);
        if (sender instanceof Player player && getConfig().getBoolean("actions.send-action-bars", true)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(msg("no-permission"));
        return false;
    }

    private Player onlinePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) sender.sendMessage(msg("player-offline"));
        return target;
    }

    private boolean isStaff(Player player, String permission) {
        return player.hasPermission(permission);
    }

    private void hideFromNonStaff(Player subject) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(subject)) continue;
            if (isStaff(viewer, "vanish.use")) viewer.showPlayer(this, subject);
            else viewer.hidePlayer(this, subject);
        }
    }

    private void announceVanish(Player subject, boolean isVanished) {
        if (!getConfig().getBoolean("actions.notify-staff-about-vanish", true)) return;
        String key = isVanished ? "vanish-on" : "vanish-off";
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(subject) && viewer.hasPermission("vanish.use")) {
                viewer.sendMessage(format(key, "player", subject.getName()));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (vanished.contains(player.getUniqueId())) {
            hideFromNonStaff(player);
            player.sendMessage(msg("vanish-reminder"));
        }
        if (godMode.contains(player.getUniqueId())) player.sendMessage(msg("god-reminder"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        staffChat.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!staffChat.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(this, () -> {
            String formatted = format("staffchat-format", "player", player.getName(), "message", message);
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (recipient.hasPermission("staffchat.use")) recipient.sendMessage(formatted);
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!allowed(sender, command.getPermission() == null ? "" : command.getPermission())) return true;

        switch (name) {
            case "vanish" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                UUID id = player.getUniqueId();
                if (vanished.remove(id)) {
                    for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, player);
                    tell(player, msg("vanish-off"));
                    announceVanish(player, false);
                } else {
                    vanished.add(id);
                    hideFromNonStaff(player);
                    tell(player, msg("vanish-on"));
                    announceVanish(player, true);
                }
                savePlayerStates();
            }
            case "staffchat" -> {
                if (args.length > 0) {
                    String message = String.join(" ", args);
                    String formatted = format("staffchat-format", "player", sender.getName(), "message", message);
                    for (Player recipient : Bukkit.getOnlinePlayers()) {
                        if (recipient.hasPermission("staffchat.use")) recipient.sendMessage(formatted);
                    }
                } else if (sender instanceof Player player) {
                    if (staffChat.add(player.getUniqueId())) tell(player, msg("staffchat-on"));
                    else { staffChat.remove(player.getUniqueId()); tell(player, msg("staffchat-off")); }
                } else sender.sendMessage("Usage: /staffchat <message>");
            }
            case "teleport" -> {
                if (args.length < 1 || args.length > 2) { sender.sendMessage(format("usage", "usage", "/" + label + " <player> [target]")); return true; }
                Player first = onlinePlayer(sender, args[0]);
                if (first == null) return true;
                if (args.length == 1) {
                    if (!(sender instanceof Player player)) { sender.sendMessage("Console must specify a target player."); return true; }
                    player.teleport(first);
                    tell(player, format("teleport-self", "target", first.getName()));
                } else {
                    Player target = onlinePlayer(sender, args[1]);
                    if (target == null) return true;
                    first.teleport(target);
                    tell(sender, format("teleport-other", "player", first.getName(), "target", target.getName()));
                }
            }
            case "tphere" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                if (args.length != 1) { sender.sendMessage(format("usage", "usage", "/" + label + " <player>")); return true; }
                Player target = onlinePlayer(sender, args[0]);
                if (target != null) { target.teleport(player); tell(player, format("teleport-here", "target", target.getName())); }
            }
            case "gmc", "gms", "gma", "gmsp", "gamemode" -> {
                GameMode mode;
                String modeText;
                String modeArg = name.equals("gmc") ? "creative" : name.equals("gms") ? "survival" :
                        name.equals("gma") ? "adventure" : name.equals("gmsp") ? "spectator" :
                        (args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "");
                mode = switch (modeArg) {
                    case "0", "s", "survival" -> GameMode.SURVIVAL;
                    case "1", "c", "creative" -> GameMode.CREATIVE;
                    case "2", "a", "adventure" -> GameMode.ADVENTURE;
                    case "3", "sp", "spectator" -> GameMode.SPECTATOR;
                    default -> null;
                };
                if (mode == null) { sender.sendMessage(format("usage", "usage", "/" + label + " <survival|creative|adventure|spectator> [player]")); return true; }
                modeText = mode.name().toLowerCase(Locale.ROOT);
                String targetName = name.equals("gamemode") && args.length > 1 ? args[1] :
                        (!name.equals("gamemode") && args.length > 0 ? args[0] : null);
                Player target;
                if (targetName == null) {
                    if (!(sender instanceof Player player)) { sender.sendMessage("Console must specify a player."); return true; }
                    target = player;
                    target.setGameMode(mode);
                    tell(sender, format("gamemode-self", "mode", modeText));
                } else {
                    target = onlinePlayer(sender, targetName);
                    if (target == null) return true;
                    target.setGameMode(mode);
                    tell(sender, format("gamemode-other", "player", target.getName(), "mode", modeText));
                    if (getConfig().getBoolean("actions.notify-staff-about-gamemode", false)) {
                        String notice = format("gamemode-other", "player", target.getName(), "mode", modeText);
                        for (Player viewer : Bukkit.getOnlinePlayers()) if (viewer.hasPermission("gamemode.use") && viewer != sender) viewer.sendMessage(notice);
                    }
                }
            }
            case "viewenderchest", "viewinventory" -> {
                if (!(sender instanceof Player viewer)) { sender.sendMessage("Players only."); return true; }
                if (args.length != 1) { sender.sendMessage(format("usage", "usage", "/" + label + " <player>")); return true; }
                Player target = onlinePlayer(sender, args[0]);
                if (target == null) return true;
                if (name.equals("viewenderchest")) {
                    viewer.openInventory(target.getEnderChest());
                    tell(viewer, format("view-enderchest", "player", target.getName()));
                } else {
                    viewer.openInventory(target.getInventory());
                    tell(viewer, format("view-inventory", "player", target.getName()));
                }
            }
            case "god" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                if (godMode.add(player.getUniqueId())) tell(player, msg("god-on"));
                else { godMode.remove(player.getUniqueId()); tell(player, msg("god-off")); }
                savePlayerStates();
            }
            default -> { return false; }
        }
        return true;
    }
}