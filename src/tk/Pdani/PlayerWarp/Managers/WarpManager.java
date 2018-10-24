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
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Main;
import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.PlayerWarpException;

public class WarpManager {
	private HashMap<OfflinePlayer,List<String>> warps = new HashMap<OfflinePlayer,List<String>>();
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
				if(w.equals(warp)){
					return true;
				}
			}
		}
		return false;
	}
	public void addWarp(Player owner, String name) throws PlayerWarpException{
		if(isWarp(name)){
			String text = MessageManager.getString("warpAlreadyExists");
			throw new PlayerWarpException(m.tl(text,name));
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
		if(this.restricted.contains(warp.toLowerCase())){
			String text = MessageManager.getString("warpNameRestricted");
			throw new PlayerWarpException(m.tl(text,warp));
		}
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
		boolean success = olist.remove(warp);
		if(!success){
			String msg = MessageManager.getString("warpRemoveError");
			msg = m.tl(msg,warp);
			user.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
		} else {
			warps.put(owner, olist);
			cc.getConfig(warpowner).set("warps."+warp, null);
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
	}
	public OfflinePlayer getWarpOwner(String warp) throws PlayerWarpException{
		if(!isWarp(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		for(Entry<OfflinePlayer, List<String>> entry : warps.entrySet()){
			if(entry.getValue().contains(warp)){
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
			if(entry.getValue().contains(warp)){
				String uuid = entry.getKey().getUniqueId().toString();
				String w = cc.getConfig(uuid).getString("warps."+warp+".location.world");
				World world = plugin.getServer().getWorld(w);
				if(world == null){
					String text = MessageManager.getString("warpWorldInvalid");
					throw new PlayerWarpException(m.tl(text));
				}
				double x = cc.getConfig(uuid).getDouble("warps."+warp+".location.x");
				double y = cc.getConfig(uuid).getDouble("warps."+warp+".location.y");
				double z = cc.getConfig(uuid).getDouble("warps."+warp+".location.z");
				float pitch = cc.getConfig(uuid).getInt("warps."+warp+".location.pitch");
				float yaw = cc.getConfig(uuid).getInt("warps."+warp+".location.yaw");
				Location loc = new Location(world,x,y,z,pitch,yaw);
				return loc;
			}
		}
		return null;
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
						wl.add(k);
					}
					Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));
					if(player == null){
						OfflinePlayer offp = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
						this.warps.put(offp, wl);
					} else {
						this.warps.put(player, wl);
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
}
