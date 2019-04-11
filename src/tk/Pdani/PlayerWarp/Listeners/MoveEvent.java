package tk.Pdani.PlayerWarp.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static tk.Pdani.PlayerWarp.Message.tl;
import tk.Pdani.PlayerWarp.Managers.MessageManager;

public class MoveEvent implements Listener {
	private PlayerCommand pc = null;
	public MoveEvent(PlayerCommand pc){
		this.pc = pc;
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
			text = tl(text);
			player.sendMessage(text);
		}
	}
	
	private String c(String msg){
		return ChatColor.translateAlternateColorCodes('&', msg);
	}
}
