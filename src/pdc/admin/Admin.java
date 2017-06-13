package pdc.admin;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sebastian on 5/25/17.
 */
public class Admin {

    private final String username;
    private String password;

    public Admin (String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }

    public void setPassword(String newPass) {
        this.password = newPass;
    }

    private static final Set<Admin> admins = new HashSet<Admin>();
    public static Set<Admin> getAdmins() { return admins; }

    /**
     * Creates default admin
     */
    public static void generateFirstAdmin() {
        admins.add(new Admin("juan", "juanjuan123"));
    }
}
