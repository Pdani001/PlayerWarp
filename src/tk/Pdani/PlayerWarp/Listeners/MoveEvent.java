package tk.Pdani.PlayerWarp.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import tk.Pdani.PlayerWarp.Message;
import tk.Pdani.PlayerWarp.Managers.MessageManager;

public class MoveEvent implements Listener {
	private PlayerCommand pc = null;
	private Message m = null;
	public MoveEvent(PlayerCommand pc, JavaPlugin plugin){
		this.pc = pc;
		this.m = new Message(plugin);
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }
		Player player = event.getPlayer();
		if(pc.isWarping(player)){
			pc.cancelWarping(player);
			String text = c(MessageManager.getString("warpingCancelled"));
			text = m.tl(text);
			player.sendMessage(text);
		}
	}
	
	private String c(String msg){
		return ChatColor.translateAlternateColorCodes('&', msg);
	}
}
