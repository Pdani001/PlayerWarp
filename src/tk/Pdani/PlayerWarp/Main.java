package tk.Pdani.PlayerWarp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Listeners.PlayerCommand;
import tk.Pdani.PlayerWarp.Listeners.PlayerJoin;
import tk.Pdani.PlayerWarp.Managers.CustomConfig;
import tk.Pdani.PlayerWarp.Managers.MessageManager;

public class Main extends JavaPlugin {
	private CustomConfig cc = null;
	private static boolean debug = false;
	public static boolean msgUpdate = false;
	private static JavaPlugin instance = null;
	private static Main main = null;
	public void onEnable(){
		instance = this;
		main = this;
		
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		
		debug = this.getConfig().getBoolean("debug", false);
		
		/*
		 * MESSAGE MANAGER LOADING
		 */
		InputStream is = this.getResource("messages.properties");
		Properties defprops = new Properties();
		Properties props = new Properties();
		try {
			if(is == null) {
				getLogger().log(Level.SEVERE, "The messages.properties file was not loaded from the jar!");
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
		File f = new File(this.getDataFolder(),"messages.properties");
		if(!f.exists()) {
			if(isDebug())getLogger().log(Level.INFO, "Created new messages.properties file");
			this.saveResource("messages.properties",f);
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
				if(getConfig().getBoolean("autoUpdateMessages", true)){
					updateMsg(f,is);
				} else {
					msgUpdate = true;
					getLogger().log(Level.WARNING, "A new messages.properties file is available! You can update it with /playerwarp updatemsg");
				}
			}
		}
		
		this.cc = new CustomConfig(this);
		PlayerJoin pj = new PlayerJoin(this,cc);
		CommandExecutor cmdexec = new PlayerCommand(this);
		getServer().getPluginManager().registerEvents(pj, this);
		this.getCommand("playerwarp").setExecutor(cmdexec);
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
}
