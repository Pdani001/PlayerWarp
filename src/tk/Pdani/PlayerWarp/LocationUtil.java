package tk.Pdani.PlayerWarp;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.earth2me.essentials.IUser;

import tk.Pdani.PlayerWarp.Managers.MessageManager;

import static tk.Pdani.PlayerWarp.Message.tl;

import java.lang.reflect.Field;
import java.util.*;

public class LocationUtil {
	private static final Set<Material> HOLLOW_MATERIALS = new HashSet<>();
    private static final Set<Material> TRANSPARENT_MATERIALS = new HashSet<>();
    public static final int RADIUS = 3;
    public static final Vector3D[] VOLUME;
    
    private static class EnumUtil {
    	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static <T extends Enum> T valueOf(Class<T> enumClass, String... names) {
            for (String name : names) {
                try {
                    Field enumField = enumClass.getDeclaredField(name);

                    if (enumField.isEnumConstant()) {
                        return (T) enumField.get(null);
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }

            return null;
        }
    	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static <T extends Enum> Set<T> getAllMatching(Class<T> enumClass, String... names) {
            Set<T> set = new HashSet<>();

            for (String name : names) {
                try {
                    Field enumField = enumClass.getDeclaredField(name);

                    if (enumField.isEnumConstant()) {
                        set.add((T) enumField.get(null));
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }

            return set;
        }
    	public static Material getMaterial(String... names) {
            return valueOf(Material.class, names);
        }
    }
    
    public static class MaterialUtil {
    	private static final Set<Material> BEDS;
    	static {
    		BEDS = EnumUtil.getAllMatching(Material.class, "BED", "WHITE_BED", "ORANGE_BED",
                    "MAGENTA_BED", "LIGHT_BLUE_BED", "YELLOW_BED", "LIME_BED", "PINK_BED", "GRAY_BED",
                    "LIGHT_GRAY_BED", "CYAN_BED", "PURPLE_BED", "BLUE_BED", "BROWN_BED", "GREEN_BED",
                    "RED_BED", "BLACK_BED");
    	}
    	public static boolean isBed(Material material) {
            return BEDS.contains(material);
        }
    }
    
    public static class Vector3D {
        public int x;
        public int y;
        public int z;

        Vector3D(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    public static boolean contains(Material m) {
    	return TRANSPARENT_MATERIALS.contains(m);
    }

    static {
        // Materials from Material.isTransparent()
        for (Material mat : Material.values()) {
            if (mat.isTransparent()) {
                HOLLOW_MATERIALS.add(mat);
            }
        }

        TRANSPARENT_MATERIALS.addAll(HOLLOW_MATERIALS);
        TRANSPARENT_MATERIALS.add(Material.WATER);
        try {
            TRANSPARENT_MATERIALS.add(Material.valueOf("FLOWING_WATER"));
        } catch (Exception ignored) {} // 1.13 WATER uses Levelled
    }
    static {
        List<Vector3D> pos = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    pos.add(new Vector3D(x, y, z));
                }
            }
        }
        pos.sort(Comparator.comparingInt(a -> (a.x * a.x + a.y * a.y + a.z * a.z)));
        VOLUME = pos.toArray(new Vector3D[0]);
    }
    
    public static boolean isBlockAboveAir(final World world, final int x, final int y, final int z) {
        return y > world.getMaxHeight() || HOLLOW_MATERIALS.contains(world.getBlockAt(x, y - 1, z).getType());
    }
    
    public static boolean isBlockUnsafeForUser(final Player user, final World world, final int x, final int y, final int z) {
    	boolean isGodMode = false;
    	if(Main.isEssAllowed()) {
    		IUser eu = Main.getEss().getUser(user);
    		if(user.isOnline()) {
    			isGodMode = eu.isGodModeEnabled();
    		}
    	}
        if (user.isOnline() && world.equals(user.getWorld()) && (user.getGameMode() == GameMode.CREATIVE || user.getGameMode() == GameMode.SPECTATOR || isGodMode) && user.getAllowFlight()) {
            return false;
        }

        if (isBlockDamaging(world, x, y, z)) {
            return true;
        }
        return isBlockAboveAir(world, x, y, z);
    }
    
    public static boolean isBlockUnsafe(final World world, final int x, final int y, final int z) {
        return isBlockDamaging(world, x, y, z) || isBlockAboveAir(world, x, y, z);
    }
    
    @SuppressWarnings("incomplete-switch")
    public static boolean isBlockDamaging(final World world, final int x, final int y, final int z) {
        final Block below = world.getBlockAt(x, y - 1, z);

        switch (below.getType()) {
            case LAVA:
            case FIRE:
                return true;
        }

        if (MaterialUtil.isBed(below.getType())) {
            return true;
        }

        try {
            if (below.getType() == Material.valueOf("FLOWING_LAVA")) {
                return true;
            }
        } catch (Exception ignored) {} // 1.13 LAVA uses Levelled
        
        try {
            if (below.getType() == Material.valueOf("STATIONARY_LAVA")) {
                return true;
            }
        } catch (Exception ignored) {}

        Material PORTAL = EnumUtil.getMaterial("NETHER_PORTAL", "PORTAL");

        if (world.getBlockAt(x, y, z).getType() == PORTAL) {
            return true;
        }

        return (!HOLLOW_MATERIALS.contains(world.getBlockAt(x, y, z).getType())) || (!HOLLOW_MATERIALS.contains(world.getBlockAt(x, y + 1, z).getType()));
    }
    
    public static Location getSafeDestination(final Player user, final Location loc) throws PlayerWarpException {
    	boolean isGodMode = false;
    	if(Main.isEssAllowed()) {
    		IUser eu = Main.getEss().getUser(user);
    		if(user.isOnline()) {
    			isGodMode = eu.isGodModeEnabled();
    		}
    	}
    	if (user.isOnline() && loc.getWorld().equals(user.getWorld()) && (user.getGameMode() == GameMode.CREATIVE || isGodMode)) {
            return loc;
        }
        return getSafeDestination(loc);
    }

    public static Location getSafeDestination(final Location loc) throws PlayerWarpException {
        if (loc == null || loc.getWorld() == null) {
            throw new PlayerWarpException(tl(MessageManager.getString("destinationNotSet")));
        }
        final World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = (int) Math.round(loc.getY());
        int z = loc.getBlockZ();
        final int origX = x;
        final int origY = y;
        final int origZ = z;
        while (isBlockAboveAir(world, x, y, z)) {
            y -= 1;
            if (y < 0) {
                y = origY;
                break;
            }
        }
        if (isBlockUnsafe(world, x, y, z)) {
            x = Math.round(loc.getX()) == origX ? x - 1 : x + 1;
            z = Math.round(loc.getZ()) == origZ ? z - 1 : z + 1;
        }
        int i = 0;
        while (isBlockUnsafe(world, x, y, z)) {
            i++;
            if (i >= VOLUME.length) {
                x = origX;
                y = origY + RADIUS;
                z = origZ;
                break;
            }
            x = origX + VOLUME[i].x;
            y = origY + VOLUME[i].y;
            z = origZ + VOLUME[i].z;
        }
        while (isBlockUnsafe(world, x, y, z)) {
            y += 1;
            if (y >= world.getMaxHeight()) {
                x += 1;
                break;
            }
        }
        while (isBlockUnsafe(world, x, y, z)) {
            y -= 1;
            if (y <= 1) {
                x += 1;
                y = world.getHighestBlockYAt(x, z);
                if (x - 48 > loc.getBlockX()) {
                    throw new PlayerWarpException(tl(MessageManager.getString("holeInFloor")));
                }
            }
        }
        return new Location(world, x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch());
    }
    
    public static boolean shouldFly(Location loc) {
        final World world = loc.getWorld();
        final int x = loc.getBlockX();
        int y = (int) Math.round(loc.getY());
        final int z = loc.getBlockZ();
        int count = 0;
        while (isBlockUnsafe(world, x, y, z) && y > -1) {
            y--;
            count++;
            if (count > 2) {
                return true;
            }
        }

        return y < 0;
    }
}
