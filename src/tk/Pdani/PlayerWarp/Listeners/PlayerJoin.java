package tk.Pdani.PlayerWarp.Listeners;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
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
		Main.asyncTask(new Runnable() {
            @Override
            public void run() {
                delayedJoin(ev.getPlayer());
            }
        });
		String name = "["+plugin.getDescription().getName()+"] ";
		if(Main.msgUpdate && ev.getPlayer().hasPermission("playerwarp.reload"))
			ev.getPlayer().sendMessage(ChatColor.RED + name + "A new messages.properties file is available! You can update it with /playerwarp updatemsg");
	}
	
	private void delayedJoin(Player player){
		String uuid = player.getUniqueId().toString();
		if(!cc.hasConfig(uuid)){
			cc.getConfig(uuid).set("name", player.getName());
			cc.saveConfig(uuid);
			if(Main.isDebug()) plugin.getLogger().log(Level.INFO, "Player file created for "+player.getName());
		}
	}

}
