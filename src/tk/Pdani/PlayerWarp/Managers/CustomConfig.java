package tk.Pdani.PlayerWarp.Managers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomConfig {
	private HashMap<String, File> filelist = new HashMap<String, File>();
	private HashMap<String, FileConfiguration> conflist = new HashMap<String, FileConfiguration>();
	private JavaPlugin main = null;
	
	public CustomConfig(JavaPlugin main) {
		this.main = main;
	}
	
	public void reloadConfig(String name) {
		if (filelist.get(name) == null) {
			File ccf = new File(main.getDataFolder(), "./players/"+name+".yml");
			filelist.put(name, ccf);
		}
		if (conflist.get(name) == null) {
			conflist.put(name, YamlConfiguration.loadConfiguration(filelist.get(name)));
		}
	}
	
	public FileConfiguration getConfig(String name) {
		if (conflist.get(name) == null) {
			reloadConfig(name);
		}
		return conflist.get(name);
	}
	
	public boolean hasConfig(String name){
		File ccf = new File(main.getDataFolder(), "./players/"+name+".yml");
		return ccf.exists();
	}
	
	public void saveConfig(String name) {
		if (conflist.get(name) == null || filelist.get(name) == null) {
			return;
		}
		try {
			getConfig(name).save(filelist.get(name));
		} catch (IOException ex) {
			main.getLogger().log(Level.SEVERE, "Could not save config to " + filelist.get(name), ex);
		}
	}
}
