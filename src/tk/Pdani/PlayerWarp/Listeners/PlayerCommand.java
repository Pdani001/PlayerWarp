package tk.Pdani.PlayerWarp.Listeners;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import tk.Pdani.PlayerWarp.HelpType;
import tk.Pdani.PlayerWarp.Main;
import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.PlayerWarpException;
import tk.Pdani.PlayerWarp.Managers.MessageManager;
import tk.Pdani.PlayerWarp.Managers.WarpManager;

public class PlayerCommand extends BukkitCommand {
	private JavaPlugin plugin = null;
	private WarpManager wm = null;
	private Message m = null;
	private HashMap<Player, BukkitTask> delayed = new HashMap<Player, BukkitTask>();
	private static final int WARPS_PER_PAGE = 10;
	private static String CMD_LIST = "list";
	private static String CMD_LISTOWN = "listown";
	private static String CMD_CREATE = "create";
	private static String CMD_REMOVE = "remove";
	private static String CMD_RELOAD = "reload";
	private static String CMD_UPDATEMSG = "updatemsg";
	private static String CMD_INFO = "info";
	private static String CMD_COUNT = "count";
	private static String CMD_COUNT_ADD = "add";
	private static String CMD_COUNT_DEL = "del";
	private static String CMD_COUNT_INFO = "info";
	private static String CMD_PLAYER = "<player>";
	private static String CMD_AMOUNT = "<amount>";
	private static String CMD_WARP = "<warp>";
	private static String CMD_COLOR = "6";
	private static boolean WORLDS_AS_BLACKLIST = true;
	private static List<String> WORLDS = new ArrayList<String>();
	public PlayerCommand(String name, JavaPlugin plugin, List<String> aliases){
		super(name);
		this.setAliases(aliases);
		this.plugin = plugin;
		this.wm = new WarpManager(this.plugin);
		this.m = new Message(this.plugin);
		CMD_LIST = this.plugin.getConfig().getString("cmdargs.list",CMD_LIST);
		CMD_LISTOWN = this.plugin.getConfig().getString("cmdargs.listown",CMD_LISTOWN);
		CMD_CREATE = this.plugin.getConfig().getString("cmdargs.create",CMD_CREATE);
		CMD_REMOVE = this.plugin.getConfig().getString("cmdargs.remove",CMD_REMOVE);
		CMD_RELOAD = this.plugin.getConfig().getString("cmdargs.reload",CMD_RELOAD);
		CMD_UPDATEMSG = this.plugin.getConfig().getString("cmdargs.updatemsg",CMD_UPDATEMSG);
		CMD_INFO = this.plugin.getConfig().getString("cmdargs.info",CMD_INFO);
		CMD_COUNT = this.plugin.getConfig().getString("cmdargs.count",CMD_COUNT);
		CMD_COUNT_ADD = this.plugin.getConfig().getString("cmdargs.count-add",CMD_COUNT_ADD);
		CMD_COUNT_DEL = this.plugin.getConfig().getString("cmdargs.count-del",CMD_COUNT_DEL);
		CMD_COUNT_INFO = this.plugin.getConfig().getString("cmdargs.count-info",CMD_COUNT_INFO);
		CMD_WARP = this.plugin.getConfig().getString("cmdargs.warp",CMD_WARP);
		CMD_PLAYER = this.plugin.getConfig().getString("cmdargs.player",CMD_PLAYER);
		CMD_AMOUNT = this.plugin.getConfig().getString("cmdargs.amount",CMD_AMOUNT);
		CMD_COLOR = this.plugin.getConfig().getString("cmdcolor",CMD_COLOR).substring(0, 1);
		WORLDS = this.plugin.getConfig().getStringList("worlds");
		WORLDS_AS_BLACKLIST = this.plugin.getConfig().getBoolean("worldsAsBlacklist",WORLDS_AS_BLACKLIST);
	}
	@SuppressWarnings("deprecation")
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if(sender instanceof Player){
			Player player = (Player) sender;
			if(disallowWorld(player)){
				String noPerm = MessageManager.getString("worldDisallowed");
				sender.sendMessage(ChatColor.RED + noPerm);
				return true;
			}
		}
			String noPerm = MessageManager.getString("noPerm");
			if(!sender.hasPermission("playerwarp.use")){
				sender.sendMessage(ChatColor.RED + noPerm);
				return true;
			}
			if(args.length == 0){
				sendHelp(sender,commandLabel,HelpType.ALL);
			} else if(args.length == 1){
				if(args[0].equalsIgnoreCase(CMD_LIST)){
					warpList(sender,1);
				} else if(args[0].equalsIgnoreCase(CMD_LISTOWN)){
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
					sender.sendMessage(c(m.tl(MessageManager.getString("ownWarps"), list)));
				} else if(args[0].equalsIgnoreCase(CMD_CREATE)){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					sendHelp(sender,commandLabel,HelpType.CREATE);
				} else if(args[0].equalsIgnoreCase(CMD_REMOVE)){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					sendHelp(sender,commandLabel,HelpType.REMOVE);
				} else if(args[0].equalsIgnoreCase(CMD_INFO)){
					sendHelp(sender,commandLabel,HelpType.INFO);
				} else if(args[0].equalsIgnoreCase(CMD_COUNT)){
					sendHelp(sender,commandLabel,HelpType.COUNT);
				} else if(args[0].equalsIgnoreCase(CMD_RELOAD)){
					if(!sender.hasPermission("playerwarp.reload")){
						sender.sendMessage(ChatColor.RED + noPerm);
						return true;
					}
					Main.reloadMessages();
					plugin.reloadConfig();
					reloadMsg();
					try {
						wm.reloadWarps();
					} catch (NullPointerException e) {
						e.printStackTrace();
						return false;
					} catch (PlayerWarpException e) {
						String msg = m.tl(MessageManager.getString("errorWithMsg"), e.getMessage());
						sender.sendMessage(c(msg));
					}
					String msg = m.tl(MessageManager.getString("reload"), "v"+plugin.getDescription().getVersion());
					sender.sendMessage(ChatColor.RED + msg);
				} else if(Main.msgUpdate && args[0].equalsIgnoreCase(CMD_UPDATEMSG)){
					if(!sender.hasPermission("playerwarp.reload")){
						sendHelp(sender,commandLabel,HelpType.ALL);
						return true;
					}
					InputStream is = plugin.getResource("messages.properties");
					File f = new File(plugin.getDataFolder(),"messages.properties");
					Main.updateMsg(f, is);
					Main.reloadMessages();
				} else {
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "Warping can only be used in-game!");
						return true;
					}
					Player player = (Player) sender;
					String warp = args[0];
					if(!wm.isWarp(warp)){
						String text = c(MessageManager.getString("warpNotFound"));
						text = m.tl(text,warp);
						sender.sendMessage(text);
						return true;
					}
					int delay = plugin.getConfig().getInt("warpDelay",0);
					final String textd = m.tl(c(MessageManager.getString("warpingDelayed")),warp,delay);
					if(delay > 0){
						cancelWarping(player);
						sender.sendMessage(textd);
						BukkitTask task = Main.asyncTaskLater(new Runnable() {
				            @Override
				            public void run() {
				            	String text = c(MessageManager.getString("warping"));
								text = m.tl(text,warp);
				            	sender.sendMessage(text);
				            	Location loc = null;
								try {
									loc = wm.getWarpLocation(warp);
								} catch (PlayerWarpException e) {
									sender.sendMessage(c(e.getMessage()));
									return;
								}
								player.teleport(loc);
								delayed.remove(player);
				            }
						},(long)delay*20L);
						delayed.put(player, task);
					} else {
						Location loc = null;
						try {
							loc = wm.getWarpLocation(warp);
						} catch (PlayerWarpException e) {
							sender.sendMessage(c(e.getMessage()));
							return true;
						}
						String text = c(MessageManager.getString("warping"));
						text = m.tl(text,warp);
						sender.sendMessage(text);
						player.teleport(loc);
					}
				}
			} else if(args.length == 2) {
				if(args[0].equalsIgnoreCase(CMD_CREATE)){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					if(!sender.hasPermission("playerwarp.create")){
						sender.sendMessage(ChatColor.RED + noPerm);
						return true;
					}
					Player player = (Player) sender;
					int limit = (sender.hasPermission("playerwarp.limit.unlimited")) ? -1 : getLimit(player);
					int count = wm.getPlayerWarps(player).size();
					if(limit != -1 && count >= limit){
						String msg = c(MessageManager.getString("warpLimitReached"));
						msg = m.tl(msg, limit);
						sender.sendMessage(msg);
						return true;
					}
					String warp = args[1];
					try {
						wm.addWarp(player, warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(c(e.getMessage()));
					}
				} else if(args[0].equalsIgnoreCase(CMD_REMOVE)){
					if(!(sender instanceof Player)){
						sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
						return true;
					}
					if(!sender.hasPermission("playerwarp.remove")){
						sender.sendMessage(ChatColor.RED + noPerm);
						return true;
					}
					Player player = (Player) sender;
					String warp = args[1];
					try {
						wm.delWarp(player, warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(c(e.getMessage()));
					}
				} else if(args[0].equalsIgnoreCase(CMD_LIST)){
					int page = 1;
			        if (Main.isInt(args[1])) {
			            page = Integer.parseInt(args[1]);
			        }
					warpList(sender,page);
				} else if(args[0].equalsIgnoreCase(CMD_INFO)){
					String warp = args[1];
					OfflinePlayer player = null;
					Location loc = null;
					try {
						player = wm.getWarpOwner(warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(c(e.getMessage()));
						return true;
					}
					try {
						loc = wm.getWarpLocation(warp);
					} catch (PlayerWarpException e) {
						sender.sendMessage(c(e.getMessage()));
						return true;
					}
					String top,owner,world,pos,w;
					w = loc.getWorld().getName();
					w = plugin.getConfig().getString("worldAlias."+w,w);
					top = m.tl(MessageManager.getString("info.top"),warp);
					owner = m.tl(MessageManager.getString("info.owner"),player.getName());
					world = m.tl(MessageManager.getString("info.world"),w);
					pos = m.tl(MessageManager.getString("info.pos"),loc.getX(),loc.getY(),loc.getZ());
					sender.sendMessage(c(top));
					sender.sendMessage(c(owner));
					sender.sendMessage(c(world));
					sender.sendMessage(c(pos));
				} else if(args[0].equalsIgnoreCase(CMD_COUNT)){
					sendHelp(sender,commandLabel,HelpType.COUNT);
				} else {
					sendHelp(sender,commandLabel,HelpType.ALL);
				}
			} else {
				if(args[0].equalsIgnoreCase(CMD_LIST)){
					int page = 1;
			        if (Main.isInt(args[1])) {
			            page = Integer.parseInt(args[1]);
			        }
					warpList(sender,page);
				} else if(args[0].equalsIgnoreCase(CMD_COUNT)){
					String m = args[1];
					if((args.length < 4 && !m.equalsIgnoreCase(CMD_COUNT_INFO)) || (args.length < 3 && m.equalsIgnoreCase(CMD_COUNT_INFO))) {
						sendHelp(sender,commandLabel,HelpType.COUNT);
						return true;
					}
					String t = args[2];
					String a = "";
					if(args.length == 4){
						a = args[3];
					}
					OfflinePlayer target = plugin.getServer().getOfflinePlayer(t);
					if(!target.hasPlayedBefore()){
						sender.sendMessage(c(MessageManager.getString("playerNotFound")));
						return true;
					}
					int amount = 0;
					if(!m.equalsIgnoreCase(CMD_COUNT_INFO)){
						try {
					        amount = Integer.parseInt(a); 
					    } catch(NumberFormatException e) {
					    	sender.sendMessage(c(MessageManager.getString("amountNotNumber")));
					        return true;
					    } catch(NullPointerException e) {
					    	sender.sendMessage(c(MessageManager.getString("amountNotNumber")));
					        return true;
					    }
					}
					if(m.equalsIgnoreCase(CMD_COUNT_ADD)){
						try {
							wm.addCount(target, amount);
						} catch (PlayerWarpException e) {
							sender.sendMessage(c(e.getMessage()));
						}
					} else if(m.equalsIgnoreCase(CMD_COUNT_DEL)){
						try {
							wm.delCount(target, amount);
						} catch (PlayerWarpException e) {
							sender.sendMessage(c(e.getMessage()));
						}
					} else if(m.equalsIgnoreCase(CMD_COUNT_INFO)){
						if(target.isOnline()){
							Player onp = plugin.getServer().getPlayer(target.getUniqueId());
							amount = (onp.hasPermission("playerwarp.limit.unlimited")) ? -1 : getLimit(target);
						} else {
							sender.sendMessage(c(MessageManager.getString("playerNotOnline")));
							return true;
							//amount = getLimit(target);
						}
						String limit = (amount == -1) ? MessageManager.getString("unlimited") : Integer.toString(amount);
						sender.sendMessage(c(this.m.tl(MessageManager.getString("count.info"),target.getName(),limit)));
					} else {
						sender.sendMessage(c(MessageManager.getString("count.invalidMethod")));
					}
				} else {
					sendHelp(sender,commandLabel,HelpType.ALL);
				}
			}
		return true;
	}
	private void reloadMsg(){
		CMD_LIST = plugin.getConfig().getString("cmdargs.list",CMD_LIST);
		CMD_LISTOWN = plugin.getConfig().getString("cmdargs.listown",CMD_LISTOWN);
		CMD_CREATE = plugin.getConfig().getString("cmdargs.create",CMD_CREATE);
		CMD_REMOVE = plugin.getConfig().getString("cmdargs.remove",CMD_REMOVE);
		CMD_RELOAD = plugin.getConfig().getString("cmdargs.reload",CMD_RELOAD);
		CMD_UPDATEMSG = plugin.getConfig().getString("cmdargs.updatemsg",CMD_UPDATEMSG);
		CMD_INFO = plugin.getConfig().getString("cmdargs.info",CMD_INFO);
		CMD_COUNT = this.plugin.getConfig().getString("cmdargs.count",CMD_COUNT);
		CMD_COUNT_ADD = this.plugin.getConfig().getString("cmdargs.count-add",CMD_COUNT_ADD);
		CMD_COUNT_DEL = this.plugin.getConfig().getString("cmdargs.count-del",CMD_COUNT_DEL);
		CMD_COUNT_INFO = this.plugin.getConfig().getString("cmdargs.count-info",CMD_COUNT_INFO);
		CMD_WARP = this.plugin.getConfig().getString("cmdargs.warp",CMD_WARP);
		CMD_PLAYER = this.plugin.getConfig().getString("cmdargs.player",CMD_PLAYER);
		CMD_AMOUNT = this.plugin.getConfig().getString("cmdargs.amount",CMD_AMOUNT);
		CMD_COLOR = plugin.getConfig().getString("cmdcolor",CMD_COLOR).substring(0, 1);
		WORLDS = this.plugin.getConfig().getStringList("worlds");
		WORLDS_AS_BLACKLIST = this.plugin.getConfig().getBoolean("worldsAsBlacklist",WORLDS_AS_BLACKLIST);
	}
	public void sendHelp(CommandSender sender, String label, HelpType type){
		String noPerm = MessageManager.getString("noPerm");
		if(type == HelpType.ALL){
			List<String> authors = plugin.getDescription().getAuthors();
			String plugin_author = "";
			for(String a : authors){
				plugin_author += plugin_author == "" ? a : ", "+a;
			}
			String plugin_name = plugin.getName();
			String plugin_version = plugin.getDescription().getVersion();
			String plugin = MessageManager.getString("plugin");
			plugin = m.tl(plugin, plugin_name, plugin_version, plugin_author);
			plugin = ChatColor.translateAlternateColorCodes('&',plugin);
			sender.sendMessage(plugin);
			if(sender instanceof Player){
				Player p = (Player) sender;
				int l = (sender.hasPermission("playerwarp.limit.unlimited")) ? -1 : getLimit(p);
				String limit = (l == -1) ? MessageManager.getString("unlimited") : Integer.toString(l);
				String msg = ChatColor.translateAlternateColorCodes('&', MessageManager.getString("help.warp_count"));
				int count = wm.getPlayerWarps(p).size();
				msg = m.tl(msg, count, limit);
				sender.sendMessage(msg);
			}
			sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_WARP+c("&7 - &6"+MessageManager.getString("help.warp")));
			sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_LIST+c("&7 - &6"+MessageManager.getString("help.list")));
			if(sender.hasPermission("playerwarp.create") || sender.hasPermission("playerwarp.remove"))
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_LISTOWN+c("&7 - &6"+MessageManager.getString("help.listown")));
			if(sender.hasPermission("playerwarp.create"))
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_CREATE + " " + CMD_WARP+c("&7 - &6"+MessageManager.getString("help.create")));
			if(sender.hasPermission("playerwarp.remove"))
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_REMOVE+" "+CMD_WARP+c("&7 - &6"+MessageManager.getString("help.remove")));
			if(sender.hasPermission("playerwarp.info"))
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_INFO + " " + CMD_WARP+c("&7 - &6"+MessageManager.getString("help.info")));
			if(sender.hasPermission("playerwarp.reload"))
				sender.sendMessage(ChatColor.getByChar("d") + l(label)+CMD_RELOAD+c("&7 - &6"+MessageManager.getString("help.reload")));
			if(sender.hasPermission("playerwarp.grant"))
				sender.sendMessage(ChatColor.getByChar("d") + l(label)+CMD_COUNT + " <"+CMD_COUNT_ADD+"/"+CMD_COUNT_DEL+"/"+CMD_COUNT_INFO+"> "+CMD_PLAYER+" "+CMD_AMOUNT+c("&7 - &6"+MessageManager.getString("help.count")));
			if(Main.msgUpdate && sender.hasPermission("playerwarp.reload"))
				sender.sendMessage(ChatColor.getByChar("d") + l(label)+CMD_UPDATEMSG+c("&7 - &6"+MessageManager.getString("help.updatemsg")));
		} else if(type == HelpType.CREATE){
			if(sender.hasPermission("playerwarp.create")) {
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_CREATE+" "+CMD_WARP);
			} else {
				sender.sendMessage(ChatColor.getByChar("c") + noPerm);
			}
		} else if(type == HelpType.REMOVE){
			if(sender.hasPermission("playerwarp.remove")) {
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_REMOVE+" "+CMD_WARP);
			} else {
				sender.sendMessage(ChatColor.getByChar("c") + noPerm);
			}
		} else if(type == HelpType.INFO){
			if(sender.hasPermission("playerwarp.info")) {
				sender.sendMessage(ChatColor.getByChar(CMD_COLOR) + l(label)+CMD_INFO+" "+CMD_WARP);
			} else {
				sender.sendMessage(ChatColor.getByChar("c") + noPerm);
			}
		} else if(type == HelpType.COUNT){
			if(sender.hasPermission("playerwarp.grant")) {
				sender.sendMessage(ChatColor.getByChar("d") + l(label)+CMD_COUNT + " <"+CMD_COUNT_ADD+"/"+CMD_COUNT_DEL+"/"+CMD_COUNT_INFO+"> "+CMD_PLAYER+" "+CMD_AMOUNT);
			} else {
				sender.sendMessage(ChatColor.getByChar("c") + noPerm);
			}
		}
	}
	
	private boolean disallowWorld(Player player){
		if(WORLDS_AS_BLACKLIST){
			if(WORLDS.contains(player.getWorld().getName())){
				return true;
			}
		} else {
			if(!WORLDS.contains(player.getWorld().getName())){
				return true;
			}
		}
		return false;
	}
	
	private String l(String label){
		return "/"+label+" ";
	}
	
	private String c(String msg){
		return ChatColor.translateAlternateColorCodes('&', msg);
	}
	
	public int getLimit(OfflinePlayer player, int defaultValue) {
		Object i = wm.getCount(player);
		if(i != null)
			return (int)i;

		if(!player.isOnline())
			return defaultValue;
		
		Player onp = plugin.getServer().getPlayer(player.getUniqueId());
		return wm.getPermCount(onp,defaultValue);
	}
	
	public int getLimit(OfflinePlayer player) {
		return getLimit(player,0);
	}
	
	private void warpList(CommandSender sender, int page){
		List<String> list = wm.getWarps();
		final int maxPages = (int) Math.ceil(list.size() / (double) WARPS_PER_PAGE);

        if (page > maxPages) {
            page = maxPages;
        }
        String warps = "";
        if(list.size() > 0){
	        final int warpPage = (page - 1) * WARPS_PER_PAGE;
	        List <String> view = list.subList(warpPage, warpPage + Math.min(list.size() - warpPage, WARPS_PER_PAGE));
	        for(String w : view){
	        	warps += (warps.equals("")) ? w : ", "+w;
	        }
        }
        if(warps.equals(""))
        	warps = MessageManager.getString("none");
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', m.tl(MessageManager.getString("warpList"), page, maxPages, warps)));
	}
	
	public WarpManager getWM(){
		return wm;
	}
	
	public boolean isWarping(Player player){
		return (delayed.get(player) != null);
	}
	
	public void cancelWarping(Player player){
		if(delayed.get(player) != null){
			delayed.get(player).cancel();
			delayed.remove(player);
		}
	}
}
