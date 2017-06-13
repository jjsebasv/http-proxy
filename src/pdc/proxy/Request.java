package pdc.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastian on 5/22/17.
 */
public class Request {

    private String method;
    private String url;
    private String fullRequest;
    private Map<String, String> headers;
    private String requestBody;

    public Request (String requestString) {
        this.headers = new HashMap<String, String>();
        String requestSections[] = requestString.split("\r\n\r\n", 2);
        String requestHead = requestSections[0];

        String requestParts[] = requestHead.split("\r\n", 2);

        if (requestSections.length > 1 && requestSections[1].contains("HEAD")) {
            this.requestBody = requestSections[1];
        }

        if (isMethod(requestString)) {
            this.method = requestParts[0].split(" ", 2)[0];
            this.url = (requestParts[0].split("://", 2)[1]).split(" ", 2)[0];
            if (this.url.endsWith("/"))
                this.url = this.url.substring(0, this.url.length() -1);
        }

        this.fullRequest = requestHead + "\r\n\r\n";
        if (this.requestBody != null) {
            this.fullRequest = this.fullRequest + this.requestBody + "\r\n\r\n";
        }
    }

    /**
     * Check if the string received matches a known method name.
     * @param r
     * @return whether if the string received is a known method or not
     */
    private boolean isMethod(String r) {
        if( r.startsWith("GET") || r.startsWith("POST") || r.startsWith("OPTIONS") || r.startsWith("HEAD") ||
                r.startsWith("PUT") || r.startsWith("DELETE") || r.startsWith("CONNECT") || r.startsWith("TRACE") ||
                r.startsWith("PATCH") )
            return true;
        return false;
    }

}
