package com.mcaichat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.mcaichat.MCAIChatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("ai|aichat")
@Description("Chat with AI")
public class AICommand extends BaseCommand {
    private final MCAIChatPlugin plugin;

    public AICommand(MCAIChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("chat")
    @Syntax("<message>")
    @Description("Send a message to AI")
    @CommandCompletion("")
    public void onChat(CommandSender sender, String message) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        plugin.getChatManager().sendMessage(player, message);
    }

    @Subcommand("clear")
    @Description("Clear the current context's chat history")
    public void onClear(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        plugin.getChatManager().clearActiveContext(player);
        player.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "context_history_cleared"));
    }

    @Subcommand("reload")
    @Description("Reload config")
    @CommandPermission("mcaichat.reload")
    public void onReload(CommandSender sender) {
        plugin.getPluginConfig().load();
        plugin.getDbConfig().load();
        plugin.getLangManager().reload();
        sender.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(null, "config_reloaded"));
    }

    @Subcommand("context")
    @Description("Manage chat contexts")
    public void onContext(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        String contexts = plugin.getChatManager().listContexts(player);
        player.sendMessage(contexts);
    }

    @Subcommand("context create")
    @Syntax("<name>")
    @Description("Create a new chat context")
    public void onCreateContext(CommandSender sender, String name) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        String result = plugin.getChatManager().createContext(player, name);
        player.sendMessage(result);
    }

    @Subcommand("context switch")
    @Syntax("<name>")
    @Description("Switch to another chat context")
    @CommandCompletion("@contexts")
    public void onSwitchContext(CommandSender sender, String name) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        String result = plugin.getChatManager().switchContext(player, name);
        player.sendMessage(result);
    }

    @Subcommand("context delete")
    @Syntax("<name>")
    @Description("Delete a chat context")
    @CommandCompletion("@contexts")
    public void onDeleteContext(CommandSender sender, String name) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        String result = plugin.getChatManager().deleteContext(player, name);
        player.sendMessage(result);
    }

    @Subcommand("context list")
    @Description("List all chat contexts")
    public void onListContexts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        String contexts = plugin.getChatManager().listContexts(player);
        player.sendMessage(contexts);
    }

    @Subcommand("ban")
    @Syntax("<player>")
    @Description("Ban a player from using AI chat")
    @CommandPermission("mcaichat.ban")
    public void onBan(CommandSender sender, Player target) {
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        plugin.getChatManager().banPlayer(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(null, "player_banned").replace("{0}", target.getName()));
        target.sendMessage(ChatColor.RED + plugin.getLangManager().get(target.getUniqueId(), "banned"));
    }

    @Subcommand("unban")
    @Syntax("<player>")
    @Description("Unban a player from using AI chat")
    @CommandPermission("mcaichat.unban")
    public void onUnban(CommandSender sender, Player target) {
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        plugin.getChatManager().unbanPlayer(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(null, "player_unbanned").replace("{0}", target.getName()));
        target.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(target.getUniqueId(), "lang_set").replace("{0}", ""));
    }

    @Subcommand("lang")
    @Syntax("<lang_code>")
    @Description("Set your language (zh-CN, en, jp)")
    @CommandCompletion("@languages")
    public void onLang(CommandSender sender, String langCode) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        Player player = (Player) sender;
        if (plugin.getLangManager().isValidLang(langCode)) {
            plugin.getLangManager().setPlayerLang(player.getUniqueId(), langCode);
            player.sendMessage(ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "lang_set").replace("{0}", langCode));
        } else {
            String available = String.join(", ", plugin.getLangManager().getAvailableLangs());
            player.sendMessage(ChatColor.RED + plugin.getLangManager().get(player.getUniqueId(), "lang_invalid").replace("{0}", available));
        }
    }
}
