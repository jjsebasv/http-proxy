package pdc.proxy;

import pdc.parser.HttpParser;
import pdc.parser.ParsingSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nowi on 5/22/17.
 */
public class HttpMessage {

    private StringBuilder message;

    private boolean messageReady;
    private ParsingSection parsingSection;

    private Map<String, String> headers;
    private long bodyLengthRead;

    // Head attributes
    private String method;
    private String url;
    private String version;

    private String statusCode;
    private String statusMsg;

    // Request Response
    private boolean request;
    private boolean response;

    public HttpMessage() {
        this.message = new StringBuilder();
        this.parsingSection = ParsingSection.HEAD;
        this.messageReady = false;
        this.headers = new HashMap<String, String>();
        this.setBodyLengthRead(0);
        this.request = false;
        this.response = false;
    }

    public String getUrl() {
        return url;
    }

    public void appendMessage(String string) {
        this.message.append(string);
        switch (this.parsingSection) {
            case HEAD:
                if (HttpParser.headReady(this.message.toString())) {
                    this.parsingSection = ParsingSection.HEADER;
                    this.request = true;
                    setHeadAttributes();
                } else if (HttpParser.headReadyResponse(this.message.toString())) {
                    this.parsingSection = ParsingSection.HEADER;
                    this.response = true;
                    setHeadAttributes();
                }
            case HEADER:
                if (HttpParser.headersReady(this.message.toString())) {
                    /**
                     * The first split is to get only whats on the headers and avoid the body
                     * The second split is to get each header separately
                     */
                    String stringHeaders[] = (this.message.toString().split("\r\n\r\n")[0]).split("\r\n");
                    for (int i = 1; i < stringHeaders.length; i++) {
                        /**
                         * That space is needed for the long parser to work afterwards
                         */
                        String headersContent[] = stringHeaders[i].split(": ");
                        headers.put(headersContent[0], headersContent[1]);
                    }
                    String length = headers.get("Content-Length");
                    if (length == null)
                        this.messageReady = true;
                    this.parsingSection = ParsingSection.BODY;
                }
                break;
            case BODY:
                this.bodyLengthRead = this.message.toString().split("\r\n\r\n")[1].length();
                if (HttpParser.bodyReady(headers.get("Content-Length"), this.bodyLengthRead)) {
                    this.messageReady = true;
                }
                break;
            default:
                break;
        }
    }

    private void setHeadAttributes() {
        String headAttributes[] = this.message.toString().split("\r\n")[0].split(" ");
        if (this.request) {
            this.method = headAttributes[0];
            this.url = headAttributes[1].split("://")[1];
            if (this.url.endsWith("/"))
                this.url = this.url.substring(0, this.url.length() -1);
            this.version = headAttributes[2];
        } else if (this.response) {
            this.statusCode = headAttributes[1];
            this.statusMsg = headAttributes[2];
        }
    }

    public void resetMessage() {
        this.message = new StringBuilder();
        /*
        this.parsingSection = ParsingSection.HEAD;
        this.messageReady = false;
        this.headers = new HashMap<>();
        this.setBodyLengthRead(0);
        */
    }

    public StringBuilder getMessage() {
        return message;
    }

    public void setMessage(StringBuilder message) {
        this.message = message;
    }

    public boolean isMessageReady() {
        return messageReady;
    }

    public void setMessageReady(boolean messageReady) {
        this.messageReady = messageReady;
    }

    public ParsingSection getParsingSection() {
        return parsingSection;
    }

    public void setParsingSection(ParsingSection parsingSection) {
        this.parsingSection = parsingSection;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public long getBodyLengthRead() {
        return bodyLengthRead;
    }

    public void setBodyLengthRead(long bodyLengthRead) {
        this.bodyLengthRead = bodyLengthRead;
    }
}
