package tk.Pdani.PlayerWarp.Managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Main;
import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.PlayerWarpException;

public class WarpManager {
	private HashMap<OfflinePlayer,List<String>> warps = new HashMap<OfflinePlayer,List<String>>();
	private HashMap<OfflinePlayer,Integer> count = new HashMap<OfflinePlayer,Integer>();
	private ArrayList<String> restricted = new ArrayList<String>();
	private JavaPlugin plugin = null;
	private CustomConfig cc = null;
	private Message m = null;
	public WarpManager(JavaPlugin plugin){
		this.plugin = plugin;
		this.cc = new CustomConfig(this.plugin);
		this.m = new Message(this.plugin);
		putRestricted();
		try {
			loadWarps();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (PlayerWarpException e) {
			if(Main.isDebug()) plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
	}
	public HashMap<OfflinePlayer,List<String>> getWarpList(){
		return this.warps;
	}
	public List<String> getWarps(){
		ArrayList<String> list = new ArrayList<String>();
		for(List<String> wl : this.warps.values()){
			for(String w : wl){
				list.add(w);
			}
		}
		return list;
	}
	public List<String> getPlayerWarps(OfflinePlayer owner){
		List<String> empty = new ArrayList<String>();
		List<String> warps = this.getWarpList().get(owner);
		return (warps == null) ? empty : warps;
	}
	public boolean isWarp(String warp){
		for(List<String> wl : this.warps.values()){
			for(String w : wl){
				if(plugin.getConfig().getBoolean("warpNameIgnoreCase", false)){
					if(w.equalsIgnoreCase(warp))
						return true;
				}
				if(w.equals(warp))
					return true;
			}
		}
		return false;
	}
	public void addWarp(Player owner, String name) throws PlayerWarpException{
		if(isWarp(name)){
			if(!owner.hasPermission("playerwarp.create.override")){
				String text = MessageManager.getString("warpAlreadyExists");
				throw new PlayerWarpException(m.tl(text,name));
			} else {
				delWarp(null,name);
			}
		}
		if(this.restricted.contains(name.toLowerCase())){
			String text = MessageManager.getString("warpNameRestricted");
			throw new PlayerWarpException(m.tl(text,name));
		}
		String pat = "^[\\p{L}0-9]*$";
		if(!name.matches(pat)){
			String text = MessageManager.getString("warpNameWithIllegalChars");
			throw new PlayerWarpException(text);
		}
		OfflinePlayer op = owner;
		String uuid = op.getUniqueId().toString();
		if(this.warps.containsKey(op)){
			ArrayList<String> list = (ArrayList<String>) this.getPlayerWarps(op);
			list.add(name);
			this.warps.put(op, list);
		} else {
			ArrayList<String> list = new ArrayList<String>();
			list.add(name);
			this.warps.put(op, list);
		}
		
		Location l = owner.getLocation();
		cc.getConfig(uuid).set("warps."+name+".location.world", l.getWorld().getName());
		cc.getConfig(uuid).set("warps."+name+".location.x", l.getX());
		cc.getConfig(uuid).set("warps."+name+".location.y", l.getY());
		cc.getConfig(uuid).set("warps."+name+".location.z", l.getZ());
		cc.getConfig(uuid).set("warps."+name+".location.pitch", l.getPitch());
		cc.getConfig(uuid).set("warps."+name+".location.yaw", l.getYaw());
		cc.saveConfig(uuid);
		
		String msg = MessageManager.getString("warpCreated");
		msg = m.tl(msg,name);
		owner.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
	}
	public void delWarp(Player user, String warp) throws PlayerWarpException{
		if(!isWarp(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		if(user != null){
			String uuid = user.getUniqueId().toString();
			OfflinePlayer owner = this.getWarpOwner(warp);
			String warpowner = owner.getUniqueId().toString();
			boolean isOwner = (warpowner != null && warpowner.equals(uuid));
			if(!isOwner && !user.hasPermission("playerwarp.remove.others")){
				String text = MessageManager.getString("notOwnerOfWarp");
				throw new PlayerWarpException(m.tl(text,warp));
			}
			ArrayList<String> olist = new ArrayList<String>();
			olist.addAll(warps.get(owner));
			String name = getRealWarpName(warp);
			boolean success = olist.remove(name);
			if(!success){
				String msg = MessageManager.getString("warpRemoveError");
				msg = m.tl(msg,warp);
				user.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
			} else {
				warps.put(owner, olist);
				cc.getConfig(warpowner).set("warps."+name, null);
				cc.saveConfig(warpowner);
				String msg = "";
				if(isOwner){
					msg = MessageManager.getString("warpRemoved");
					msg = m.tl(msg,warp);
				} else {
					msg = MessageManager.getString("warpRemovedOther");
					msg = m.tl(msg,owner.getName(),warp);
				}
				user.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
			}
		} else {
			OfflinePlayer owner = this.getWarpOwner(warp);
			String warpowner = owner.getUniqueId().toString();
			ArrayList<String> olist = new ArrayList<String>();
			olist.addAll(warps.get(owner));
			String name = getRealWarpName(warp);
			boolean success = olist.remove(name);
			if(!success){
				String msg = MessageManager.getString("warpRemoveError");
				msg = m.tl(msg,warp);
				throw new PlayerWarpException(ChatColor.translateAlternateColorCodes('&', msg));
			} else {
				warps.put(owner, olist);
				cc.getConfig(warpowner).set("warps."+name, null);
				cc.saveConfig(warpowner);
			}
		}
	}
	public OfflinePlayer getWarpOwner(String warp) throws PlayerWarpException{
		if(!isWarp(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		for(Entry<OfflinePlayer, List<String>> entry : warps.entrySet()){
			if(contains(entry.getValue(),warp)){
				OfflinePlayer warpowner = entry.getKey();
				return warpowner;
			}
		}
		return null;
	}
	public Location getWarpLocation(String warp) throws PlayerWarpException{
		if(!isWarp(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		for(Entry<OfflinePlayer, List<String>> entry : warps.entrySet()){
			if(contains(entry.getValue(),warp)){
				String uuid = entry.getKey().getUniqueId().toString();
				String name = getRealWarpName(warp);
				String w = cc.getConfig(uuid).getString("warps."+name+".location.world");
				World world = plugin.getServer().getWorld(w);
				if(world == null){
					String text = MessageManager.getString("warpWorldInvalid");
					throw new PlayerWarpException(m.tl(text));
				}
				double x = cc.getConfig(uuid).getDouble("warps."+name+".location.x");
				double y = cc.getConfig(uuid).getDouble("warps."+name+".location.y");
				double z = cc.getConfig(uuid).getDouble("warps."+name+".location.z");
				float pitch = (float)cc.getConfig(uuid).getDouble("warps."+name+".location.pitch");
				float yaw = (float)cc.getConfig(uuid).getDouble("warps."+name+".location.yaw");
				Location loc = new Location(world,x,y,z,yaw,pitch);
				return loc;
			}
		}
		return null;
	}
	private String getRealWarpName(String name){
		if(!isWarp(name)){
			return null;
		}
		if(!plugin.getConfig().getBoolean("warpNameIgnoreCase", false)){
			return name;
		}
		for(String w : getWarps()){
			if(w.equalsIgnoreCase(name)) return w;
		}
		return null;
	}
	private boolean contains(List<String> list, String word){
		for(String s : list){
			if(plugin.getConfig().getBoolean("warpNameIgnoreCase", false)) {
				if(s.equalsIgnoreCase(word)) return true;
			} else {
				if(s.equals(word)) return true;
			}
		}
		return false;
	}
	private void putRestricted(){
		List<String> list = plugin.getConfig().getStringList("restricted");
		if(list != null)
			restricted.addAll(list);
	}
	public void loadWarps() throws NullPointerException,PlayerWarpException {
		if(!this.warps.isEmpty()){
			throw new PlayerWarpException("Warps already loaded!");
		}
		ArrayList<String> all = new ArrayList<String>();
		File dir = new File(plugin.getDataFolder(),"/players");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				ArrayList<String> wl = new ArrayList<String>();
				String name = child.getName();
				String uuid = "";
				if(name.endsWith(".yml")){
					uuid = name.replace(".yml", "");
				} else {
					continue;
				}
				cc.reloadConfig(uuid);
				if(cc.getConfig(uuid).isConfigurationSection("warps")){
					for(String k : cc.getConfig(uuid).getConfigurationSection("warps").getKeys(false)){
						if(plugin.getConfig().getBoolean("warpNameIgnoreCase", false)){
							boolean f = false;
							for(String a : all){
								f = (a.equalsIgnoreCase(k));
								if(f) break;
							}
							if(f){
								if(Main.isDebug()) plugin.getLogger().log(Level.WARNING, "Ignoring warp '"+k+"' (Duplicate entry)");
								continue;
							}
						}
						wl.add(k);
						all.add(k);
					}
					int amount = cc.getConfig(uuid).getInt("count",0);
					Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));
					if(player == null){
						OfflinePlayer offp = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
						this.warps.put(offp, wl);
						if(cc.getConfig(uuid).isSet("count"))
							this.count.put(offp, amount);
					} else {
						this.warps.put(player, wl);
						if(cc.getConfig(uuid).isSet("count"))
							this.count.put(player, amount);
					}
				} else {
					if(cc.getConfig(uuid).isSet("count")){
						int amount = cc.getConfig(uuid).getInt("count");
						Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));
						if(player == null){
							OfflinePlayer offp = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
							this.count.put(offp, amount);
						} else {
							this.count.put(player, amount);
						}
					}
				}
			}
		} else {
			throw new PlayerWarpException("Players directory not found!");
		}
		Main.convertOldWarps(this,this.cc);
	}
	
	public void reloadWarps() throws NullPointerException,PlayerWarpException {
		this.warps.clear();
		loadWarps();
	}
	
	public void addCount(OfflinePlayer p, int am) throws PlayerWarpException{
		int pc = 0;
		if(p.isOnline()){
			Player onp = plugin.getServer().getPlayer(p.getUniqueId());
			pc = getPermCount(onp);
		}
		String uuid = p.getUniqueId().toString();
		String text = MessageManager.getString("count.added");
		if(count.get(p) != null){
			Integer i = count.get(p);
			count.replace(p, i+am);
			cc.getConfig(uuid).set("count", i+am);
			cc.saveConfig(uuid);
			throw new PlayerWarpException(m.tl(text,am,p.getName()));
		}
		if(pc > 0)
			am += pc;
		cc.getConfig(uuid).set("count", am);
		cc.saveConfig(uuid);
		count.put(p, am);
		throw new PlayerWarpException(m.tl(text,am-pc,p.getName()));
	}
	
	public void delCount(OfflinePlayer p, int am) throws PlayerWarpException{
		int pc = 0;
		if(p.isOnline()){
			Player onp = plugin.getServer().getPlayer(p.getUniqueId());
			pc = getPermCount(onp);
		}
		String uuid = p.getUniqueId().toString();
		Integer i = count.get(p);
		if(i != null){
			i -= pc;
			if(i <= am) {
				cc.getConfig(uuid).set("count", null);
				count.remove(p);
			} else {
				cc.getConfig(uuid).set("count", i-am);
				count.replace(p, i-am);
			}
			cc.saveConfig(uuid);
			String text = MessageManager.getString("count.removed");
			throw new PlayerWarpException(m.tl(text,am,p.getName()));
		}
		String text = MessageManager.getString("count.notEnough");
		throw new PlayerWarpException(m.tl(text,p.getName()));
	}
	
	public Object getCount(OfflinePlayer p){
		return count.get(p);
	}
	
	public int getPermCount(Player player, int defaultValue) {
		String permissionPrefix = "playerwarp.limit.";

		for (PermissionAttachmentInfo attachmentInfo : player.getEffectivePermissions()) {
			String perm = attachmentInfo.getPermission();
			if (perm.startsWith(permissionPrefix) && !perm.endsWith("unlimited")) {
				return Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
			}
		}

		return defaultValue;
	}
	
	public int getPermCount(Player player){
		return getPermCount(player,0);
	}
}
