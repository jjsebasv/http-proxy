package pdc.parser;

/**
 * Created by sebastian on 5/23/17.
 */
public class HttpParser {

    public static boolean headersReady(String message) {
        return message.contains("\r\n\r\n");
    }

    public static boolean isRequest(String message) {
        return message.startsWith("GET") || message.startsWith("POST") || message.startsWith("OPTIONS") ||
                message.startsWith("HEAD") || message.startsWith("PUT") || message.startsWith("DELETE") || message.startsWith("CONNECT") ||
                message.startsWith("TRACE") || message.startsWith("PATCH");
    }

    public static boolean isResponse(String message) {
        return message.startsWith("HTTP");
    }
}
