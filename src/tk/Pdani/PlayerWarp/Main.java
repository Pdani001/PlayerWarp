package tk.Pdani.PlayerWarp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import tk.Pdani.PlayerWarp.Listeners.PlayerCommand;
import tk.Pdani.PlayerWarp.Listeners.PlayerJoin;
import tk.Pdani.PlayerWarp.Managers.CustomConfig;
import tk.Pdani.PlayerWarp.Managers.MessageManager;
import tk.Pdani.PlayerWarp.Managers.WarpManager;

public class Main extends JavaPlugin {
	private CustomConfig cc = null;
	private static boolean debug = false;
	public static boolean msgUpdate = false;
	private static JavaPlugin instance = null;
	private static Main main = null;
	private List<String> aliases = null;
	
	public void onEnable(){
		instance = this;
		main = this;
		
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		
		debug = this.getConfig().getBoolean("debug", false);
		aliases = this.getConfig().getStringList("aliases");
		if(aliases == null){
			aliases = new ArrayList<String>();
		}
		
		
		PlayerCommand pc = new PlayerCommand("playerwarp",this,aliases);
		Command cmdexec = pc;
		
		Field bukkitCommandMap;
		try {
			bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
			commandMap.register("playerwarp", cmdexec);
		} catch (Exception e) {}
		
		reloadMessages();
		
		this.cc = new CustomConfig(this);
		PlayerJoin pj = new PlayerJoin(this,cc,pc.getWM());
		getServer().getPluginManager().registerEvents(pj, this);
		//this.getCommand("playerwarp").setExecutor(cmdexec);
		getLogger().log(Level.INFO, "Plugin enabled.");
		List<Player> players = getOnlinePlayers();
		for(Player p : players){
			String uuid = p.getUniqueId().toString();
			if(!cc.hasConfig(uuid)){
				cc.getConfig(uuid).set("name", p.getName());
				cc.saveConfig(uuid);
				if(isDebug()) getLogger().log(Level.INFO, "Player file created for "+p.getName());
			}
		}
	}
	public void onDisable(){
		getLogger().log(Level.INFO, "Plugin disabled.");
		unRegisterBukkitCommand("playerwarp",aliases);
	}
	
	private static Object getPrivateField(Object object, String field)throws SecurityException,
	    NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
	    Class<?> clazz = object.getClass();
	    Field objectField = clazz.getDeclaredField(field);
	    objectField.setAccessible(true);
	    Object result = objectField.get(object);
	    objectField.setAccessible(false);
	    return result;
	}
	
	private void unRegisterBukkitCommand(String cmd, List<String> aliases) {
	    try {
	        Object result = getPrivateField(Bukkit.getServer().getPluginManager(), "commandMap");
	        SimpleCommandMap commandMap = (SimpleCommandMap) result;
	        Object map = getPrivateField(commandMap, "knownCommands");
	        @SuppressWarnings("unchecked")
	        HashMap<String, Command> knownCommands = (HashMap<String, Command>) map;
	        knownCommands.remove(cmd);
	        for (String alias : aliases){
	           if(knownCommands.containsKey(alias) && knownCommands.get(alias).toString().contains(this.getName())){
	                knownCommands.remove(alias);
	            }
	        }
	    } catch (Exception e) {}
	}
	
	public static void convertOldWarps(WarpManager wm, CustomConfig cc){
		asyncTask(new Runnable() {
			public void run(){
				List<String> warps = wm.getWarps();
				for(String w : warps){
					String owner = "";
					try {
						owner = wm.getWarpOwner(w).getUniqueId().toString();
					} catch (PlayerWarpException e) {
						e.printStackTrace();	// will never print
					}
					if(cc.getConfig(owner).isConfigurationSection("warps."+w+".location")){
						continue;
					}
					
					Object o = cc.getConfig(owner).get("warps."+w+".location");
					if(o instanceof Location){
						if(isDebug()) instance.getLogger().log(Level.INFO, "Converting "+w+" warp...");
						Location l = (Location) o;
						cc.getConfig(owner).set("warps."+w+".location", null);
						cc.getConfig(owner).set("warps."+w+".location.world", l.getWorld().getName());
						cc.getConfig(owner).set("warps."+w+".location.x", l.getX());
						cc.getConfig(owner).set("warps."+w+".location.y", l.getY());
						cc.getConfig(owner).set("warps."+w+".location.z", l.getZ());
						cc.getConfig(owner).set("warps."+w+".location.pitch", l.getPitch());
						cc.getConfig(owner).set("warps."+w+".location.yaw", l.getYaw());
						cc.saveConfig(owner);
					}
				}
			}
		});
	}
	
	public static void asyncTask(Runnable run){
		getScheduler().runTaskAsynchronously(instance, run);
	}
	
	public static BukkitScheduler getScheduler() {
        return instance.getServer().getScheduler();
    }
	
	public static boolean isDebug(){
		return debug;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Player> getOnlinePlayers(){
		ArrayList<Player> players = new ArrayList<Player>();
		Collection<? extends Player> collectionList = null;
		Player[] playerList = null;
		boolean isOldClass = false;
		try {
		    if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class) {
		    	collectionList = ((Collection<? extends Player>)Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]));
		    } else {
		    	playerList = ((Player[])Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]));
		    	isOldClass = true;
		    }
		} catch (Exception ex) {
			ex.printStackTrace(); // will probably never print
			return players;
		}
		if(isOldClass){
			for(Player player : playerList){
				players.add(player);
			}
		} else {
			for(Player player : collectionList){
				players.add(player);
			}
		}
		return players;
	}
	
	public static void updateMsg(File f, InputStream is){
		try {
			InputStream is2 = new FileInputStream(f);
			if(is2 != is){
				if(isDebug())instance.getLogger().log(Level.INFO, "Saved updated messages.properties");
				File oldF = new File(instance.getDataFolder()+"/messages.properties");
				File newF = new File(instance.getDataFolder()+"/messages.old.properties");
				if(newF.exists())
					newF.delete();
				if(oldF.renameTo(newF)) {
					oldF.delete();
					main.saveResource("messages.properties",f);
				}
			}
			is2.close();
			msgUpdate = false;
		} catch (Exception e) {
			e.printStackTrace(); // Will probably never print
		}
	}
	public static void reloadMessages(){
		InputStream is = instance.getResource("messages.properties");
		Properties defprops = new Properties();
		Properties props = new Properties();
		try {
			if(is == null) {
				instance.getLogger().log(Level.SEVERE, "The messages.properties file was not loaded from the jar!");
				defprops = null;
			} else {
				defprops.load(is);
			}
		} catch (IOException e) {
			e.printStackTrace();
			defprops = null;
		}
		MessageManager.setDefProps(defprops);
		String version = MessageManager.getDefString("version");
		File f = new File(instance.getDataFolder(),"messages.properties");
		if(!f.exists()) {
			if(isDebug())instance.getLogger().log(Level.INFO, "Created new messages.properties file");
			main.saveResource("messages.properties",f);
		} else {
			InputStream fis = null;
			try {
				fis = new FileInputStream(f);
				props.load(fis);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			MessageManager.setProps(props);
			String cv = MessageManager.getString("version", false);
			if((cv == null || !cv.equals(version))){
				if(instance.getConfig().getBoolean("autoUpdateMessages", true)){
					updateMsg(f,is);
				} else {
					msgUpdate = true;
					instance.getLogger().log(Level.WARNING, "A new messages.properties file is available! You can update it with /playerwarp updatemsg");
				}
			}
		}
	}
	
	private void saveResource(String name, File outFile) {
	    try (InputStream in = this.getResource(
	            name);
	            OutputStream out = new FileOutputStream(outFile);) {

	        int read = 0;
	        byte[] bytes = new byte[1024];

	        while ((read = in.read(bytes)) != -1) {
	            out.write(bytes, 0, read);
	        }

	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}
	
	public static boolean isInt(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    } catch(NullPointerException e) {
	        return false;
	    }
	    return true;
	}
}
