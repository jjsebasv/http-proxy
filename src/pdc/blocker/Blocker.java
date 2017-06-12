package pdc.blocker;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sebastian on 6/12/17.
 */
public class Blocker {

    private static Set<String> blockedHosts;
    private static Set<Integer> blockedPorts;

    private static Blocker instance;

    private Blocker() {
        blockedHosts = new HashSet<String>();
        blockedPorts = new HashSet<Integer>();
    }

    public static Blocker getInstance() {
        if (instance == null)
            instance = new Blocker();
        return instance;
    }

    public static boolean isHostBlocked(String host) {
        return blockedHosts.contains(host);
    }

    public static boolean isPortBlocked(int port) {
        return blockedPorts.contains(port);
    }

    public static void addBlockedHost(String host) {
        blockedHosts.add(host);
    }

    public static void addBlockedPort(int port) {
        blockedPorts.add(port);
    }

    public static void removeBlockedHost(String host) {
        blockedHosts.remove(host);
    }

    public static void removeBlockedPort(int port) {
        blockedPorts.remove(port);
    }
}
