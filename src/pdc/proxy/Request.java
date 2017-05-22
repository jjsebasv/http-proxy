package pdc.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastian on 5/22/17.
 */
public class Request {

    private String method;
    private String uri;
    private String url;
    private String host;
    private String fullRequest;
    private Map<String, String> headers;

    public Request (String requestString) {
        this.headers = new HashMap<>();

        String fullRequest = requestString.split("\r\n\r\n")[0];
        String requestParts[] = fullRequest.split("\r\n");

        this.method = requestParts[0].split(" ")[0];
        this.uri = (requestParts[0].split("://")[1]).split(" ")[0];

        for (int i = 1; i < requestParts.length; i++) {
            String aux[] = requestParts[i].split(":");
            headers.put(aux[0], aux[1]);
            if (aux[0].toLowerCase().equals("host")) this.host = aux[1];
        }

        this.url = this.uri.substring(0, this.uri.length() -1);
        this.fullRequest = fullRequest + "\r\n\r\n";
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getFullRequest() {
        return fullRequest;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
