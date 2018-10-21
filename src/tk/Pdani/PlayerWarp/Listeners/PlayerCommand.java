package tk.Pdani.PlayerWarp.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.Managers.MessageManager;
import tk.Pdani.PlayerWarp.Managers.WarpManager;

public class PlayerCommand implements CommandExecutor {
	private JavaPlugin plugin = null;
	private WarpManager wm = null;
	private Message m = null;
	public PlayerCommand(JavaPlugin plugin){
		this.plugin = plugin;
		this.wm = new WarpManager(this.plugin);
		this.m = new Message(this.plugin);
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(cmd.getName().equalsIgnoreCase("playerwarp")){
			if(args.length == 0){
				
			}
		}
		return true;
	}
	public void sendHelp(CommandSender sender, String label){
		String plugin_author = plugin.getDescription().getAuthors().get(0);
		String plugin_name = plugin.getName();
		String plugin_version = plugin.getDescription().getVersion();
		sender.sendMessage("§e"+plugin_name+" plugin v"+plugin_version+" created by "+plugin_author);
		if(sender instanceof Player){
			Player p = (Player) sender;
			int l = (sender.hasPermission("playerwarp.limit.unlimited")) ? -1 : getLimit(p);
			String limit = (l == -1) ? MessageManager.getString("unlimited") : Integer.toString(l);
			String msg = ChatColor.translateAlternateColorCodes('&', MessageManager.getString("help.warp_count"));
			int count = wm.getPlayerWarps(p).size();
			msg = m.tl(msg, count, limit);
			sender.sendMessage(msg);
		}
		if(sender.hasPermission("playerwarp.create"))
			sender.sendMessage(ChatColor.GOLD + label(label)+"create <warp>");
		if(sender.hasPermission("playerwarp.remove"))
			sender.sendMessage(ChatColor.GOLD + label(label)+"remove <warp>");
		if(sender.hasPermission("playerwarp.remove.others"))
			sender.sendMessage(ChatColor.RED + label(label)+"remove <warp> [player]");
	}
	
	private String label(String label){
		return "/"+label+" ";
	}
	
	public int getLimit(Player player, int defaultValue) {
		String permissionPrefix = "playerwarp.limit.";

		for (PermissionAttachmentInfo attachmentInfo : player.getEffectivePermissions()) {
			String perm = attachmentInfo.getPermission();
			if (perm.startsWith(permissionPrefix) && !perm.endsWith("unlimited")) {
				return Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
			}
		}

		return defaultValue;
	}
	
	public int getLimit(Player player) {
		return getLimit(player,0);
	}
}
