package tk.Pdani.PlayerWarp.Listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.HelpType;
import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.PlayerWarpException;
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
				sendHelp(sender,commandLabel,HelpType.ALL);
			} else if(args.length == 1){
				if(args[0].equalsIgnoreCase("list")){
					String list = "";
					for(List<String> wl : wm.getWarpList().values()){
						for(String w : wl){
							list += (list.equals("")) ? w : ", "+w;
						}
					}
					if(list.equals(""))
						list = MessageManager.getString("none");
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', m.tl(MessageManager.getString("warpList"), list)));
				} else if(args[0].equalsIgnoreCase("create")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					sendHelp(sender,commandLabel,HelpType.CREATE);
				} else if(args[0].equalsIgnoreCase("remove")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					sendHelp(sender,commandLabel,HelpType.REMOVE);
				} else {
					sendHelp(sender,commandLabel,HelpType.ALL);
				}
			} else if(args.length == 2) {
				if(args[0].equalsIgnoreCase("create")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					Player player = (Player) sender;
					String warp = args[1];
					try {
						wm.addWarp(player, warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
					}
				} else if(args[0].equalsIgnoreCase("remove")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
				} else {
					sendHelp(sender,commandLabel,HelpType.ALL);
				}
			}
		}
		return true;
	}
	public void sendHelp(CommandSender sender, String label, HelpType type){
		String noPerm = MessageManager.getString("NoPerm");
		if(!sender.hasPermission("playerwarp.use")){
			sender.sendMessage(ChatColor.RED + noPerm);
			return;
		}
		if(type == HelpType.ALL){
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
			sender.sendMessage(ChatColor.GOLD + l(label)+"list");
			if(sender.hasPermission("playerwarp.create"))
				sender.sendMessage(ChatColor.GOLD + l(label)+"create <warp>");
			if(sender.hasPermission("playerwarp.remove"))
				sender.sendMessage(ChatColor.GOLD + l(label)+"remove <warp>");
			if(sender.hasPermission("playerwarp.remove.others"))
				sender.sendMessage(ChatColor.YELLOW + l(label)+"remove <warp> [player]");
		} else if(type == HelpType.CREATE){
			if(sender.hasPermission("playerwarp.create")) {
				sender.sendMessage(ChatColor.GOLD + l(label)+"create <warp>");
			} else {
				sender.sendMessage(ChatColor.RED + noPerm);
			}
		} else if(type == HelpType.REMOVE){
			if(sender.hasPermission("playerwarp.remove")) {
				sender.sendMessage(ChatColor.GOLD + l(label)+"remove <warp>");
				if(sender.hasPermission("playerwarp.remove.others"))
					sender.sendMessage(ChatColor.RED + l(label)+"remove <warp> [player]");
			} else {
				sender.sendMessage(ChatColor.RED + noPerm);
			}
		}
	}
	
	private String l(String label){
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
