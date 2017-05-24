package pdc.proxy;

/**
 * Created by sebastian on 5/21/17.
 */
public class Config {
    private static final int BUFFER_SIZE = 250000000;
    private static final int TIMEOUT = 3000;
    private static final int PROXY_PORT = 9090;
    private static final String PROXY_HOST = "127.0.0.1";
    private static boolean verbose = true;

    public static int getBufferSize() {
        return BUFFER_SIZE;
    }

    public static int getTIMEOUT() {
        return TIMEOUT;
    }

    public static int getProxyPort() {
        return PROXY_PORT;
    }

    public static String getProxyHost() {
        return PROXY_HOST;
    }

    public static boolean isVerbose() {
        return verbose;
    }
}
