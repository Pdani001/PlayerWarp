package tk.Pdani.PlayerWarp.Listeners;

import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Main;
import tk.Pdani.PlayerWarp.Managers.CustomConfig;

public class PlayerJoin implements Listener {
	private CustomConfig cc = null;
	private JavaPlugin plugin = null;
	public PlayerJoin(JavaPlugin plugin, CustomConfig cc) {
		this.cc = cc;
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent ev){
		String uuid = ev.getPlayer().getUniqueId().toString();
		if(!cc.hasConfig(uuid)){
			cc.getConfig(uuid).set("warps", new String[0]);
			cc.saveConfig(uuid);
			if(Main.isDebug()) plugin.getLogger().log(Level.INFO, "Player file created");
		}
	}

}
