package ir.practice.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * NMS Title + ActionBar utility for Spigot 1.8
 */
@SuppressWarnings({"unchecked","deprecation"})
public class TitleUtil {

    private static String version;

    static {
        version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> packetClass     = nms("PacketPlayOutTitle");
            Class<?> enumAction      = nms("PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatSerializer  = nms("IChatBaseComponent$ChatSerializer");

            java.lang.reflect.Method fromJson = chatSerializer.getMethod("a", String.class);

            // Times packet
            Object timesPacket = packetClass
                    .getConstructor(enumAction, nms("IChatBaseComponent"), int.class, int.class, int.class)
                    .newInstance(Enum.valueOf((Class<Enum>) enumAction, "TIMES"), null, fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            // Title
            if (title != null && !title.isEmpty()) {
                Object comp = fromJson.invoke(null, jsonEscape(title));
                Object pkt  = packetClass
                        .getConstructor(enumAction, nms("IChatBaseComponent"))
                        .newInstance(Enum.valueOf((Class<Enum>) enumAction, "TITLE"), comp);
                sendPacket(player, pkt);
            }

            // Subtitle
            if (subtitle != null && !subtitle.isEmpty()) {
                Object comp = fromJson.invoke(null, jsonEscape(subtitle));
                Object pkt  = packetClass
                        .getConstructor(enumAction, nms("IChatBaseComponent"))
                        .newInstance(Enum.valueOf((Class<Enum>) enumAction, "SUBTITLE"), comp);
                sendPacket(player, pkt);
            }
        } catch (Exception e) {
            // Fallback: send as chat
            if (title != null && !title.isEmpty()) player.sendMessage(title);
        }
    }

    public static void sendActionBar(Player player, String text) {
        try {
            Class<?> packetClass    = nms("PacketPlayOutChat");
            Class<?> chatSerializer = nms("IChatBaseComponent$ChatSerializer");
            java.lang.reflect.Method fromJson = chatSerializer.getMethod("a", String.class);
            Object comp = fromJson.invoke(null, jsonEscape(text));
            Object pkt  = packetClass.getConstructor(nms("IChatBaseComponent"), byte.class)
                    .newInstance(comp, (byte) 2);
            sendPacket(player, pkt);
        } catch (Exception e) {
            // silent fail
        }
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object conn   = handle.getClass().getField("playerConnection").get(handle);
        conn.getClass().getMethod("sendPacket", nms("Packet")).invoke(conn, packet);
    }

    private static Class<?> nms(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + version + "." + name);
    }

    private static String jsonEscape(String text) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }
}
