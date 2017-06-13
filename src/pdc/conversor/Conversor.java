package pdc.conversor;

import pdc.config.ProxyConfiguration;
import pdc.proxy.Metrics;

/**
 * Created by sebastian on 5/27/17.
 */
public class Conversor {

    public static boolean leetOn;
    public static boolean flipOn;

    private static Conversor instance;

    public static Conversor getInstance() {
        if (instance == null)
            instance = new Conversor();
        return instance;
    }

    public Conversor() {
        this.leetOn = Boolean.valueOf(ProxyConfiguration.getInstance().getProperty("leet"));
        this.flipOn =  Boolean.valueOf(ProxyConfiguration.getInstance().getProperty("flip"));
    }

    public static boolean isLeetOn() {
        return leetOn;
    }

    public static void setLeetOn(boolean leetOn) {
        Conversor.leetOn = leetOn;
    }

    public static boolean isFlipOn() {
        return flipOn;
    }

    public static void setFlipOn(boolean flipOn) {
        Conversor.flipOn = flipOn;
    }

    /**
     * Given a char this function changes it if it's one of the special characters.
     * @param a
     * @return a converted character
     */
    public static byte leetChar(char a) {
        switch (Character.toLowerCase(a)) {
            case 'a':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'4';
            case 'e':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'3';
            case 'i':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'1';
            case 'o':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'0';
            case 'c':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'<';
            default:
                return (byte)a;
        }
    }

}
