package pdc.proxy;

import pdc.conversor.Conversor;
import pdc.parser.ParsingSectionSection;
import pdc.parser.ParsingSection;
import pdc.parser.ParsingStatus;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;

/**
 * Created by nowi on 5/22/17.
 */
public class HttpMessage {

    private URL url;
    private ParsingSection parsingSection;
    private ParsingStatus parsingStatus;
    private Map<String, String> headers;
    private int spaceCount;
    private ParsingSectionSection parsingSectionSection;
    private StringBuilder headerLine;
    private StringBuilder method;
    private StringBuilder status;
    private StringBuilder urlBuffer;
    private Metrics metrics = Metrics.getInstance();
    private int lastChars = 0;

    public long getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    private long bytesRead;
    private long bodyBytes;

    private boolean request, response;

    public ParsingStatus getParsingStatus() {
        return parsingStatus;
    }

    public HttpMessage() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuilder();
        spaceCount = 0;
        bytesRead = 0;
        bodyBytes = 0;
        this.urlBuffer = new StringBuilder();
        method =  new StringBuilder();
        status = new StringBuilder();
        parsingSectionSection = ParsingSectionSection.START_LINE;
        this.response = false;
        this.request = false;
    }


    /**
     * Considers the given ByteBuffer as a HTTP request and parse it, leaving the buffer ready for the next operation
     *
     *
     * @param message
     *
     */
    public void readRequest(ByteBuffer message) {
        request = true;
        int pos = message.position();
        message.flip();
        message.rewind();

        CharsetDecoder decoder = Charset.forName("ISO_8859_1").newDecoder();
        CharBuffer messageAsChar = CharBuffer.allocate(message.capacity());
        decoder.decode(message, messageAsChar, false);
        messageAsChar.flip();
        while (messageAsChar.hasRemaining()) {
            char c = messageAsChar.get();
            parseRequest(c);
        }
        bytesRead += message.limit();
        isBodyRead();
        message.flip();
        message.rewind();
        message.position(pos);
        metrics.addMethod(this.method.toString());
    }

    private void isBodyRead() {
        if (this.headers.containsKey("content-length") && bodyBytes  >= Long.valueOf(this.headers.get("content-length"))) {
            this.parsingStatus = ParsingStatus.FINISH;
        }
    }

    public URL getUrl() {
        return this.url;
    }


    private void parseRequest (char c) {
        switch (parsingSection) {
            case HEAD:
                if (spaceCount == 0) {
                    if (c != ' ') {
                        this.method.append(c);
                    } else {
                        spaceCount++;
                    }
                } else if (spaceCount == 1) {
                    if (c != ' ') {
                        this.urlBuffer.append(c);
                    } else {
                        spaceCount++;
                        try {
                            this.url = new URL(this.urlBuffer.toString());
                        } catch (MalformedURLException e) {
                            System.out.println("Malformed URL " + this.url);
                        }
                    }
                } else {
                    if (c == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
                parseHeader(c);
                break;
            case BODY:
                parseBody(c);
                break;

        }
    }

    private void saveHeader(String StringBuilder) {
        String string = StringBuilder.toString();
        String stringHeaders[] = string.split(": ");
        if (stringHeaders.length <= 1){
            System.out.println("Something went wrong here");
            return;
        }
        this.headers.put(stringHeaders[0], stringHeaders[1]);
    }


    /**
     * Considers the given ByteBuffer as a HTTP response and parse it, leaving the buffer ready for the next operation
     *
     *
     * @param message
     *
     */
    public void readResponse(ByteBuffer message) {
        System.out.println("********************");
        int pos = message.position();
        int i = 0;
        response = true;
        message.flip();
        message.rewind();

        CharsetDecoder decoder = Charset.forName("ISO_8859_1").newDecoder();
        CharBuffer messageAsChar = CharBuffer.allocate(message.capacity());
        decoder.decode(message, messageAsChar, false);
        messageAsChar.flip();
        while (messageAsChar.hasRemaining()) {
            char c = messageAsChar.get();
            parseResponse(c);
            if (Conversor.leetOn &&  parsingSection == ParsingSection.BODY &&
                    this.headers.containsKey("content-type") &&
                    this.headers.get("content-type").equals("text/plain")) {
                message.put(i, Conversor.leetChar(c));
            }
            i++;
        }
        bytesRead += message.limit();
        isBodyRead();
        message.flip();
        message.rewind();
        message.position(pos);
        System.out.println("********************");
    }

    private void parseResponse(char c) {
        switch (parsingSection) {
            case HEAD:
                if (spaceCount == 0) {
                    if (c != ' ') {
                        this.status.append(c);
                    } else {
                        spaceCount++;
                    }
                } else {
                    if (c == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
                parseHeader(c);
                break;
            case BODY:
                if (this.headers.containsKey("transfer-encoding") && this.headers.get("transfer-encoding").equals("chunked")) {
                    parseChunkedBody(c);
                    return;
                } else {
                    parseBody(c);
                }
                bodyBytes++;
                break;
        }
    }

    private void parseChunkedBody(char c) {

        if (c == '\r') {
            System.out.print("/r");
        } else if (c == '\n') {
            System.out.print("/n");
        } else {
            System.out.print("-");
        }

        switch (parsingSectionSection) {
        case START_LINE:
            if (c == '\r') {
                this.lastChars++;
            } else if (c == '\n') {
                this.lastChars++;
                this.parsingSectionSection = ParsingSectionSection.END_LINE;
            }
            break;
        case END_LINE:
            if (c == '\r') {
                this.lastChars++;
                this.parsingSectionSection = ParsingSectionSection.END_SECTION;
            } else {
                lastChars = 0;
                this.parsingSectionSection = ParsingSectionSection.START_LINE;
            }
            break;
        case END_SECTION:
            this.parsingStatus = ParsingStatus.FINISH;
            break;
        }
    }

    private void parseBody(char c) {
        if (c == '\r') {
            System.out.print("/r");
        } else if (c == '\n') {
            System.out.print("/n");
        } else {
            System.out.print("-");
        }

        switch (parsingSectionSection) {
            case START_LINE:
                if (c == '\r') {
                    this.lastChars++;
                } else if (c == '\n') {
                    this.lastChars++;
                    this.parsingSectionSection = ParsingSectionSection.END_LINE;
                }
                break;
            case END_LINE:
                if (c == '\r') {
                    this.lastChars++;
                    this.parsingSectionSection = ParsingSectionSection.END_SECTION;
                } else {
                    lastChars = 0;
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                }
                break;
            case END_SECTION:
                this.parsingStatus = ParsingStatus.FINISH;
                break;
        }
    }

    private void parseHeader(char b) {
        switch (parsingSectionSection) {
            case START_LINE:
                if (b != '\n' && b != '\r') {
                    this.headerLine.append(b);
                } else if (b == '\r') {
                    if (this.headerLine.length() == 0) {
                        this.parsingSectionSection = ParsingSectionSection.END_SECTION;
                    } else {
                        this.parsingSectionSection = ParsingSectionSection.END_LINE;
                    }
                }
                break;
            case END_LINE:
                if (b == '\n') {
                    saveHeader(this.headerLine.toString().toLowerCase());
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                    this.headerLine = new StringBuilder();
                }
                break;
            case END_SECTION:
                if (b == '\n') {
                    this.parsingSection = ParsingSection.BODY;
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                    if (!response && request && !headers.containsKey("content-length")) {
                        this.parsingStatus = ParsingStatus.FINISH;
                    }
                }
                break;
        }
    }

    /**
     *
     * Resets completely the actual HttpMessage in order to be ready for a new connection between client and server
     *
     *
     */
    public void reset() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuilder();
        spaceCount = 0;
        this.urlBuffer = new StringBuilder();
        method =  new StringBuilder();
        status = new StringBuilder();
        parsingSectionSection = ParsingSectionSection.START_LINE;
        this.bytesRead = 0;
        this.bodyBytes = 0;
        this.response = false;
        this.request = false;
    }


    /**
     *
     * Resets only some params of the actual HttpMessage in order to be ready for new parsing but knowing some information
     * might be needed from the connection.
     *
     *
     */
    public void resetRequest() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        this.bytesRead = 0;
        this.response = false;
        this.request = false;
    }
}
