import app.logvar.ShareAddressPolicy;

public class ShareAddressCheck {
    public static void main(String[] args) {
        for (String name : new String[]{"wlan0", "wlan1", "ap0", "swlan0", "br0", "ap_br_swlan0", "br_softap0"})
            if (!ShareAddressPolicy.isHotspotInterface(name)) throw new AssertionError(name);
        for (String name : new String[]{"rmnet_data0", "ccmni0", "tun0", "ppp0", "lo", "p2p0", "rndis0"})
            if (ShareAddressPolicy.isHotspotInterface(name)) throw new AssertionError("Unsafe interface: " + name);
        System.out.println("PASS: hotspot interface selection excludes cellular, VPN and peer-to-peer addresses");
    }
}
