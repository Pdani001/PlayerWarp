package tk.Pdani.PlayerWarp.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.PlayerWarpException;

public class WarpManager {
	private HashMap<Player,List<String>> warps = new HashMap<Player,List<String>>();
	private ArrayList<String> restricted = new ArrayList<String>();
	private JavaPlugin plugin = null;
	private CustomConfig cc = null;
	private Message m = null;
	public WarpManager(JavaPlugin plugin){
		this.plugin = plugin;
		this.cc = new CustomConfig(this.plugin);
		this.m = new Message(this.plugin);
	}
	public HashMap<Player,List<String>> getWarpList(){
		return this.warps;
	}
	public List<String> getPlayerWarps(Player owner){
		List<String> empty = new ArrayList<String>();
		List<String> warps = this.getWarpList().get(owner);
		return (warps == null) ? empty : warps;
	}
	public void addWarp(Player owner, String name) throws PlayerWarpException{
		if(this.warps.containsValue(name)){
			String text = MessageManager.getString("warpAlreadyExists");
			throw new PlayerWarpException(m.tl(text,name));
		}
		if(this.restricted.contains(name)){
			String text = MessageManager.getString("warpNameRestricted");
			throw new PlayerWarpException(m.tl(text,name));
		}
		String uuid = owner.getUniqueId().toString();
		if(this.warps.containsKey(owner)){
			ArrayList<String> list = (ArrayList<String>) this.getPlayerWarps(owner);
			list.add(name);
			this.warps.put(owner, list);
		} else {
			ArrayList<String> list = new ArrayList<String>();
			list.add(name);
			this.warps.put(owner, list);
		}
		
		cc.getConfig(uuid).set("warps."+name+".location", owner.getLocation());
		cc.saveConfig(uuid);
	}
	public void delWarp(Player user, String warp) throws PlayerWarpException{
		if(!this.warps.containsValue(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		if(this.restricted.contains(warp)){
			String text = MessageManager.getString("warpNameRestricted");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		String uuid = user.getUniqueId().toString();
		Player owner = this.getWarpOwner(warp);
		String warpowner = owner.getUniqueId().toString();
		boolean isOwner = (warpowner != null && warpowner.equals(uuid));
		if(!isOwner && !user.hasPermission("playerwarp.remove.others")){
			String text = MessageManager.getString("notOwnerOfWarp");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		boolean success = warps.remove(warpowner, warp);
		if(!success){
			String msg = MessageManager.getString("warpRemoveError");
			msg = m.tl(msg,warp);
			user.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
		} else {
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
	public Player getWarpOwner(String warp) throws PlayerWarpException{
		if(!this.warps.containsValue(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		for(Entry<Player, List<String>> entry : warps.entrySet()){
			if(entry.getValue().contains(warp)){
				Player warpowner = entry.getKey();
				return warpowner;
			}
		}
		return null;
	}
	public Location getWarpLocation(String warp) throws PlayerWarpException{
		if(!this.warps.containsValue(warp)){
			String text = MessageManager.getString("warpNotFound");
			throw new PlayerWarpException(m.tl(text,warp));
		}
		for(Entry<Player, List<String>> entry : warps.entrySet()){
			if(entry.getValue().contains(warp)){
				String uuid = entry.getKey().getUniqueId().toString();
				Object l = cc.getConfig(uuid).get("warps."+warp+".location");
				Location loc = (l != null) ? (Location)l : null;
				return loc;
			}
		}
		return null;
	}
	public void putRestricted(){
		restricted.add("create");
		restricted.add("remove");
		restricted.add("list");
	}
}
