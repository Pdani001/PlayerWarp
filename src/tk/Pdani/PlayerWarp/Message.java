package tk.Pdani.PlayerWarp;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Based on Essentials' I18n class
 */
public class Message {
	private static JavaPlugin plugin = Main.getInstance();
	private static final Pattern NODOUBLEMARK = Pattern.compile("''");
	
	public static String tl(final String string, final Object... objects) {
        if (objects.length == 0) {
            return NODOUBLEMARK.matcher(string).replaceAll("'");
        } else {
            return format(string, objects);
        }
    }

    private static String format(final String string, final Object... objects) {
    	String format = string;
		MessageFormat messageFormat = null;
		try {
			messageFormat = new MessageFormat(format);
		} catch (IllegalArgumentException e) {
			plugin.getLogger().log(Level.SEVERE, "Invalid Translation key for '" + string + "': " + e.getMessage());
			format = format.replaceAll("\\{(\\D*?)\\}", "\\[$1\\]");
			messageFormat = new MessageFormat(format);
		} catch (NullPointerException e) {
			plugin.getLogger().log(Level.SEVERE, e.getMessage());
			return format;
		}
        return messageFormat.format(objects);
    }
}
