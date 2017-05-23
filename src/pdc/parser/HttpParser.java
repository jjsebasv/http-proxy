package pdc.parser;

/**
 * Created by sebastian on 5/23/17.
 */
public class HttpParser {

    public static boolean headersReady(String message) {
        return message.contains("\r\n\r\n");
    }

    public static boolean headReady(String message) {
        return message.contains("\r\n") && (message.startsWith("GET") || message.startsWith("POST") || message.startsWith("OPTIONS") ||
                message.startsWith("HEAD") || message.startsWith("PUT") || message.startsWith("DELETE") || message.startsWith("CONNECT") ||
                message.startsWith("TRACE") || message.startsWith("PATCH"));
    }

    public static boolean bodyReady(String contentLength, long readLength) {
        return contentLength == null || Long.valueOf(contentLength) == readLength;
    }
}
