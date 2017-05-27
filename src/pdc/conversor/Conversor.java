package pdc.conversor;

import pdc.proxy.Metrics;

/**
 * Created by sebastian on 5/27/17.
 */
public class Conversor {

    public static byte leet(char a) {
        switch (a) {
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
