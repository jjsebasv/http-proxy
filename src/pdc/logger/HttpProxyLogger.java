package pdc.logger;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpProxyLogger {

    private static HttpProxyLogger instance;
    private Logger logger;


    private HttpProxyLogger() throws IOException {
        logger = LoggerFactory.getLogger(HttpProxyLogger.class);
    }

    public static HttpProxyLogger getInstance() {
        try {
            if (instance == null)
                instance = new HttpProxyLogger();
        } catch (IOException e) {
            HttpProxyLogger.getInstance().error("Cannot open logger configuration file");
        }
        return instance;
    }

    /**
     * Set a log with type DEBUG
     * @param s
     */
    public void debug(String s) {
        logger.debug(s);
    }

    /**
     * Set a log with type ERROR
     * @param s
     */
    public void error(String s) {
        logger.error(s);
    }

    /**
     * Set a log with type INFO
     * @param s
     */
    public void info(String s) {
        logger.info(s);
    }

    /**
     * Set a log with type WARN
     * @param s
     */
    public void warn(String s) {
        logger.warn(s);
    }
}
