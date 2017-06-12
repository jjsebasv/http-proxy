package pdc.conversor;

import pdc.proxy.Metrics;

import java.nio.ByteBuffer;

/**
 * Created by sebastian on 5/27/17.
 */
public class Conversor {
    
    public static boolean leetOn = false;
    public static boolean flipOn = false;

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
