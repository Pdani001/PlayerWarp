package tk.Pdani.PlayerWarp.Listeners;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.HelpType;
import tk.Pdani.PlayerWarp.Main;
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
				} else if(args[0].equalsIgnoreCase("listown")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					Player player = (Player) sender;
					String list = "";
					for(String w : wm.getPlayerWarps(player)){
						list += (list.equals("")) ? w : ", "+w;
					}
					if(list.equals(""))
						list = MessageManager.getString("none");
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', m.tl(MessageManager.getString("ownWarps"), list)));
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
				} else if(args[0].equalsIgnoreCase("reload")){
					if(!sender.hasPermission("playerwarp.reload")){
						String noPerm = MessageManager.getString("NoPerm");
						sender.sendMessage(ChatColor.RED + noPerm);
						return true;
					}
					Main.reloadMessages();
					try {
						wm.reloadWarps();
					} catch (NullPointerException e) {
						e.printStackTrace();
						return false;
					} catch (PlayerWarpException e) {
						String msg = m.tl(MessageManager.getString("errorWithMsg"), e.getMessage());
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
					}
					plugin.reloadConfig();
					String msg = m.tl(MessageManager.getString("reload"), "v"+plugin.getDescription().getVersion());
					sender.sendMessage(ChatColor.RED + msg);
				} else if(Main.msgUpdate && args[0].equalsIgnoreCase("updateMsg")){
					if(!sender.hasPermission("playerwarp.reload")){
						sendHelp(sender,commandLabel,HelpType.ALL);
						return true;
					}
					InputStream is = plugin.getResource("messages.properties");
					File f = new File(plugin.getDataFolder(),"messages.properties");
					Main.updateMsg(f, is);
				} else {
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "Warping can only be used in-game!");
						return true;
					}
					Player player = (Player) sender;
					String warp = args[0];
					if(!wm.isWarp(warp)){
						String text = ChatColor.translateAlternateColorCodes('&', MessageManager.getString("warpNotFound"));
						text = m.tl(text,warp);
						sender.sendMessage(text);
						return true;
					}
					Location loc = null;
					try {
						loc = wm.getWarpLocation(warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
						return true;
					}
					String text = ChatColor.translateAlternateColorCodes('&', MessageManager.getString("warping"));
					text = m.tl(text,warp);
					sender.sendMessage(text);
					player.teleport(loc);
				}
			} else if(args.length == 2) {
				if(args[0].equalsIgnoreCase("create")){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					Player player = (Player) sender;
					int limit = (sender.hasPermission("playerwarp.limit.unlimited")) ? -1 : getLimit(player);
					int count = wm.getPlayerWarps(player).size();
					if(limit != -1 && count >= limit){
						String msg = ChatColor.translateAlternateColorCodes('&', MessageManager.getString("warpLimitReached"));
						msg = m.tl(msg, limit);
						sender.sendMessage(msg);
						return true;
					}
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
					Player player = (Player) sender;
					String warp = args[1];
					try {
						wm.delWarp(player, warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
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
			sender.sendMessage(ChatColor.GOLD + l(label)+"list"+c("&7 - &6"+MessageManager.getString("help.list")));
			if(sender.hasPermission("playerwarp.create") || sender.hasPermission("playerwarp.remove"))
				sender.sendMessage(ChatColor.GOLD + l(label)+"listown"+c("&7 - &6"+MessageManager.getString("help.listown")));
			if(sender.hasPermission("playerwarp.create"))
				sender.sendMessage(ChatColor.GOLD + l(label)+"create <warp>"+c("&7 - &6"+MessageManager.getString("help.create")));
			if(sender.hasPermission("playerwarp.remove"))
				sender.sendMessage(ChatColor.GOLD + l(label)+"remove <warp>"+c("&7 - &6"+MessageManager.getString("help.remove")));
			if(sender.hasPermission("playerwarp.remove.others"))
				sender.sendMessage(ChatColor.LIGHT_PURPLE + l(label)+"remove <warp> [player]"+c("&7 - &6"+MessageManager.getString("help.remove.others")));
			if(sender.hasPermission("playerwarp.reload"))
				sender.sendMessage(ChatColor.LIGHT_PURPLE + l(label)+"reload"+c("&7 - &6"+MessageManager.getString("help.reload")));
			if(Main.msgUpdate && sender.hasPermission("playerwarp.reload"))
				sender.sendMessage(ChatColor.RED + l(label)+"updatemsg"+c("&7 - &6"+MessageManager.getString("help.updatemsg")));
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
	
	private String c(String msg){
		return ChatColor.translateAlternateColorCodes('&', msg);
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
