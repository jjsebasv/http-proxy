package pdc.admin;

import pdc.proxy.Metrics;

/**
 * Created by sebastian on 5/25/17.
 */
public final class AdminConstants {

    //public static final byte[] WELCOME_MSG = ("Welcome to the Admin Manager\n").getBytes();
    public static final byte[] WELCOME_MSG = ("200 OK\r\nAdmin Panel (type: GET HELP if you need it)\n").getBytes();
    public static final byte[] LINE_SEPARATOR = ("**********************************************************************************\n").getBytes();

    /**
     * 200 OK - Request was correct - Data gives human readable information about response
     */
    public static final byte[] HELP = ("200 OK\r\nThis is help.\r\nAvailable Commands:\r\n" +
            "USER <username>\r\n" +
            "LOG OUT [require login]\r\n" +
            "ADD USER [require login]\r\n" +
            "ADD BLACKLIST [require login]\r\n" +
            "REMOVE BLACKLIST [require login]\r\n" +
            "GET ALL-METRICS [require login]\r\n" +
            "GET BYTES-SENT [require login]\r\n" +
            "GET BYTES-RECEIVED [require login]\r\n" +
            "GET METHOD-HISTOGRAMS [require login]\r\n" +
            "GET CONVERTED-CHARS [require login]\r\n" +
            "GET FLIPPED-IMAGES [require login]\r\n" +
            "GET TOTAL-ACCESSES [require login]\r\n" +
            "SET LEET-ON [require login]\r\n" +
            "SET LEET-OFF [require login]\r\n" +
            "SET FLIP-ON [require login]\r\n" +
            "SET FLIP-OFF [require login]\r\n" +
            "\r\n\r\n"
    ).getBytes();
    public static final byte[] OK_USER = ("200 OK\r\nEnter Password (type: pass [PASS])\r\n\r\n").getBytes();
    public static final byte[] LOGGED_IN = ("200 OK\r\nYou are know logged in\r\n\r\n").getBytes();
    public static final byte[] LOGGED_OUT = ("200 OK\r\nGood Bye!\r\n\r\n").getBytes();
    public static final byte[] LEET_ON = ("200 OK\r\nThe leet converter is on\r\n\r\n").getBytes();
    public static final byte[] LEET_OFF = ("200 OK\r\nThe leet converter is off\r\n\r\n").getBytes();
    public static final byte[] FLIP_ON = ("200 OK\r\nThe image flip converter is on\r\n\r\n").getBytes();
    public static final byte[] FLIP_OFF = ("200 OK\r\nThe flip image converter is off\r\n\r\n").getBytes();
    public static final byte[] BLOCKED_HOST = ("200 OK\r\nHost Blocked\r\n\r\n").getBytes();
    public static final byte[] BLOCKED_PORT= ("200 OK\r\nPort Blocked\r\n\r\n").getBytes();
    public static final byte[] UNBLOCKED_HOST = ("200 OK\r\nHost Unblocked\r\n\r\n").getBytes();
    public static final byte[] UNBLOCKED_PORT = ("200 OK\r\nPort Unblocked\r\n\r\n").getBytes();

    public static final byte[] ADDING_USER = ("200 OK\r\nEnter username (type: user [USERNAME])\r\n\r\n").getBytes();
    public static final byte[] ADDING_PASS = ("200 OK\r\nUsername available (type: pass [PASS])\r\n\r\n").getBytes();
    public static final byte[] ADDING_BLACKLIST = ("200 OK\r\nYou can block hosts or ports (type: host [HOST] / port [PORT])\r\n\r\n").getBytes();
    public static final byte[] REMOVING_BLACKLIST = ("200 OK\r\nYou can unblock hosts or ports (type: host [HOST] / port [PORT])\r\n\r\n").getBytes();

    /**
     * 201 Created
     * Request was correct and successfully created what asked
     * Data gives human readable information about response
     **/
    public static final byte[] USER_CREATED = ("201 Created\r\nUser created\r\n\r\n").getBytes();

    /**
     * 222 Metrics Requested
     * Request was done correctly and asked for metrics
     * Data gives human readable information that is sensitive and might be interesting to read
     *
     * @param metrics the type of metrics requested
     * @return byte[] of response
     */
    public static byte[] metricsRequested(String metrics) {
        String metricsAnswer = "";
        if (metrics.equals("total_accesses")) {
            metricsAnswer = Metrics.getInstance().getTotalAccesses();
        } else if (metrics.equals("bytes_sent")) {
            metricsAnswer = Metrics.getInstance().getTransferredBytes();
        } else if (metrics.equals("bytes_received")) {
            metricsAnswer = Metrics.getInstance().getReceivedBytes();
        } else if (metrics.equals("converted_chars")) {
            metricsAnswer = Metrics.getInstance().getConvertedChars();
        } else if (metrics.equals("flipped_images")) {
            metricsAnswer = Metrics.getInstance().getFlippedImages();
        } else if (metrics.equals("method_histograms")) {
            metricsAnswer = Metrics.getInstance().getMethodHistograms();
        } else if (metrics.equals("all")) {
            metricsAnswer = Metrics.getInstance().getAll();
        }
        return String.format("222 Metrics Requested\r\n%s\r\n\r\n", metricsAnswer).getBytes();
    }

    /**
     * 400 Wrong command
     * Request was done with an incorrect command
     * Data gives human readable information about response
     **/
    public static final byte[] WRONG_COMMAND = ("400 Wrong command\r\nCheck your spelling\r\n\r\n").getBytes();

    /**
     * 201 Unauthorized
     * Either the request was done without authorization or authentication wasn't complete
     * Data gives human readable information about response
     **/
    public static final byte[] WRONG_USERNAME = ("401 Unauthorized\r\nThe username specified doesn't exist\r\n\r\n").getBytes();
    public static final byte[] ERROR_LOG_IN = ("401 Unauthorized\r\nUsername and password doesn't match\r\n\r\n").getBytes();
    public static final byte[] UNAUTHORIZED = ("401 Unauthorized\r\nYou should be logged in\r\n\r\n").getBytes();

    /**
     * 409 Conflict
     * The request was processed but some of the information given resulted in conflict
     * Data gives human readable information about response
     **/
    public static final byte[] USER_LOGGED = ("409 Conflict\r\nYou are already logged in. Logout first to change account\r\n\r\n").getBytes();
    public static final byte[] USERNAME_IN_USE = ("409 Conflict\r\nUsername in use. Enter username (type: user [USERNAME])\r\n\r\n").getBytes();


    /**
     * 500 Internal Error
     * Something bad happened, client might know the what and why, we surely don't
     * Data gives human readable information about response
     **/
    public static final byte[] INTERNAL_ERROR = ("500 Internal Error\r\nPlease get in touch with group 3\r\n\r\n").getBytes();

}
