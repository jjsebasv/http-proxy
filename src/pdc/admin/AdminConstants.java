package pdc.admin;

import pdc.proxy.Metrics;

/**
 * Created by sebastian on 5/25/17.
 */
public final class AdminConstants {

    public static final byte[] WELCOME_MSG = ("Welcome to the Admin Manager\n").getBytes();
    public static final byte[] LINE_SEPARATOR = ("**********************************************************************************\n").getBytes();

    /**
     * 200 OK - Request was correct - Data gives human readable information about response
     */
    public static final byte[] HELP = ("200 OK\r\nThis is help\r\n\r\n").getBytes();
    public static final byte[] LOG_REQUEST = ("200 OK\r\nEnter username (type: user [USERNAME])\r\n\r\n").getBytes();
    public static final byte[] KNOWN_USER = ("200 OK\r\nKnown username (type: pass [PASS]\r\n\r\n").getBytes();
    public static final byte[] LOGGED_IN = ("200 OK\r\nYou are know logged in\r\n\r\n").getBytes();
    public static final byte[] LOGGED_OUT = ("200 OK\r\nGood Bye!\r\n\r\n").getBytes();
    public static final byte[] LEET_ON = ("200 OK\r\nThe leet converter is on\r\n\r\n").getBytes();
    public static final byte[] LEET_OFF = ("200 OK\r\nThe leet converter is off\r\n\r\n").getBytes();
    public static final byte[] BLOCKED_HOST = ("200 OK\r\nHost Blocked\r\n\r\n").getBytes();
    public static final byte[] BLOCKED_PORT= ("200 OK\r\nPort Blocked\r\n\r\n").getBytes();
    public static final byte[] UNBLOCKED_HOST = ("200 OK\r\nHost Unblocked\r\n\r\n").getBytes();
    public static final byte[] UNBLOCKED_PORT = ("200\r\nPort Unblocked\r\n\r\n").getBytes();

    /**
     * 201 Created
     * Request was correct and succesfully created what asked
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
        if (metrics.equals("total_acceses")) {
            metricsAnswer = Metrics.getInstance().getTotalAccesses();
        } else if (metrics.equals("bytes_sent")) {
            metricsAnswer = Metrics.getInstance().getTransferredBytes();
        } else if (metrics.equals("bytes_received")) {
            metricsAnswer = Metrics.getInstance().getReceivedBytes();
        } else if (metrics.equals("converted_chars")) {
            metricsAnswer = Metrics.getInstance().getConvertedChars();
        } else if (metrics.equals("flipped_immages")) {
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
    public static final byte[] WRONG_PASSWORD = ("401 Unauthorized\r\nUsername and password doesn't match\r\n\r\n").getBytes();
    public static final byte[] UNAUTHORIZED = ("401\r\nYou should be logged in\r\n\r\n").getBytes();

    /**
     * 500 Internal Error
     * Something bad happened, client might know the what and why, we surely don't
     * Data gives human readable information about response
     **/
    public static final byte[] INTERNAL_ERROR = ("500 Internal Error\r\nPlease get in touch with group 3\r\n\r\n").getBytes();

}
