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

    @Default
    @Syntax("<message>")
    @Description("Send a message to AI")
    @CommandCompletion("")
    public void onCommand(CommandSender sender, String message) {
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
        player.sendMessage(ChatColor.GREEN + "Current context history cleared!");
    }

    @Subcommand("reload")
    @Description("Reload config")
    @CommandPermission("mcaichat.reload")
    public void onReload(CommandSender sender) {
        plugin.getPluginConfig().load();
        plugin.getDbConfig().load();
        sender.sendMessage(ChatColor.GREEN + "Config and database config reloaded!");
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
        sender.sendMessage(ChatColor.GREEN + "Banned " + target.getName() + " from AI chat!");
        target.sendMessage(ChatColor.RED + "You have been banned from AI chat!");
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
        sender.sendMessage(ChatColor.GREEN + "Unbanned " + target.getName() + " from AI chat!");
        target.sendMessage(ChatColor.GREEN + "You have been unbanned from AI chat!");
    }
}
