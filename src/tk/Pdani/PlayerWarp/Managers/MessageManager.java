package tk.Pdani.PlayerWarp.Managers;

import java.util.Properties;

public class MessageManager {
	private static Properties defProps = null;
	private static Properties props = null;
	
	public static void setDefProps(Properties dp){
		defProps = dp;
	}
	
	public static void setProps(Properties p){
		props = p;
	}

	public static String getDefString(String key, String def) {
		if(defProps == null)
			return def;
		return defProps.getProperty(key,def);
	}
	public static String getDefString(String key) {
		if(defProps == null)
			return null;
		return defProps.getProperty(key);
	}
	
	public static String getString(String key, String def) {
		if(props == null)
			return getDefString(key,def);
		return props.getProperty(key,def);
	}
	public static String getString(String key) {
		if(props == null)
			return getDefString(key);
		return props.getProperty(key);
	}
	public static String getString(String key, String def, boolean getDefString) {
		if(props == null && getDefString)
			return getDefString(key,def);
		return props.getProperty(key,def);
	}
	public static String getString(String key, boolean getDefString) {
		if(props == null && getDefString)
			return getDefString(key);
		return props.getProperty(key);
	}
}
