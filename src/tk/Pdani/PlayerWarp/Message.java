package tk.Pdani.PlayerWarp;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

public class Message {
	private JavaPlugin plugin = null;
	private static final Pattern NODOUBLEMARK = Pattern.compile("''");
	
	public Message(JavaPlugin plugin){
		this.plugin = plugin;
	}
	
	public String tl(final String string, final Object... objects) {
        if (objects.length == 0) {
            return NODOUBLEMARK.matcher(string).replaceAll("'");
        } else {
            return this.format(string, objects);
        }
    }

    private String format(final String string, final Object... objects) {
    	String format = string;
		MessageFormat messageFormat = null;
		try {
			messageFormat = new MessageFormat(format);
		} catch (IllegalArgumentException e) {
			plugin.getLogger().log(Level.SEVERE, "Invalid Translation key for '" + string + "': " + e.getMessage());
			format = format.replaceAll("\\{(\\D*?)\\}", "\\[$1\\]");
			messageFormat = new MessageFormat(format);
		}
        return messageFormat.format(objects);
    }
}
