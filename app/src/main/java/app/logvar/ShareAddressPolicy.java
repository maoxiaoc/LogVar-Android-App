package app.logvar;

public final class ShareAddressPolicy {
    public static boolean isHotspotInterface(String name) {
        return name.matches("(?:wlan|swlan|ap|softap|wifi|br|bridge)[0-9]+")
            || name.matches("br_(?:wlan|softap|ap)[0-9]+")
            || name.equals("ap_br_swlan0");
    }
}
