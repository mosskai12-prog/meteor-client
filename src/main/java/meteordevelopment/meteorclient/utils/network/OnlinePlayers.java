package meteordevelopment.meteorclient.utils.network;

/**
 * Fizzy privacy change:
 *
 * Meteor's public online-player service is unrelated to the Minecraft server.
 * Keep these compatibility methods so existing call sites remain valid, but do
 * not send online/leave pings to meteorclient.com.
 */
public final class OnlinePlayers {
    private OnlinePlayers() {
    }

    public static void update() {
        // Disabled in Fizzy: no external online-presence reporting.
    }

    public static void leave() {
        // Disabled in Fizzy: no external online-presence reporting.
    }
}
