package tk.Pdani.PlayerWarp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Listeners.PlayerCommand;
import tk.Pdani.PlayerWarp.Listeners.PlayerJoin;
import tk.Pdani.PlayerWarp.Managers.CustomConfig;
import tk.Pdani.PlayerWarp.Managers.MessageManager;

public class Main extends JavaPlugin {
	private CustomConfig cc = null;
	private static boolean debug = false;
	public void onEnable(){
		
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		
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
			if(cv == null || !cv.equals(version)){
				try {
					InputStream is2 = new FileInputStream(f);
					if(is2 != is){
						if(isDebug())getLogger().log(Level.INFO, "Saved updated messages.properties");
						File oldF = new File(this.getDataFolder()+"/messages.properties");
						File newF = new File(this.getDataFolder()+"/messages.old.properties");
						if(newF.exists())
							newF.delete();
						if(oldF.renameTo(newF)) {
							oldF.delete();
							this.saveResource("messages.properties",f);
						}
					}
					is2.close();
				} catch (Exception e) {
					e.printStackTrace(); // Will probably never print
				}
			}
		}
		
		debug = this.getConfig().getBoolean("debug", false);
		this.cc = new CustomConfig(this);
		PlayerJoin pj = new PlayerJoin(this,cc);
		CommandExecutor cmdexec = new PlayerCommand(this);
		getServer().getPluginManager().registerEvents(pj, this);
		this.getCommand("playerwarp").setExecutor(cmdexec);
		getLogger().log(Level.INFO, "Plugin enabled.");
	}
	public void onDisable(){
		getLogger().log(Level.INFO, "Plugin disabled.");
	}
	
	public static boolean isDebug(){
		return debug;
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
