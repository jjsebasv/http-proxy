package pdc.proxy;

import pdc.conversor.Conversor;
import pdc.conversor.FlippedImage;
import pdc.logger.HttpProxyLogger;
import pdc.parser.ParsingSectionSection;
import pdc.parser.ParsingSection;
import pdc.parser.ParsingStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.Buffer;
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
    private FlippedImage image;
    private HttpProxyLogger logger;
    private int linesCount = 0;
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
        method = new StringBuilder();
        status = new StringBuilder();
        parsingSectionSection = ParsingSectionSection.START_LINE;
        this.response = false;
        this.request = false;
        logger = HttpProxyLogger.getInstance();
    }


    /**
     * Considers the given ByteBuffer as a HTTP request and parse it, leaving the buffer ready for the next operation
     *
     *
     * @param message
     *
     */
    public ByteBuffer readRequest(ByteBuffer message) {
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
            parseRequest(c, messageAsChar.position());
        }
        if (this.url == null && this.headers.containsKey("host")) {
            try {
                String urlString = urlBuffer.toString();
                String protocol = "http";
                String path = "";
                int port = 80;
                if (urlString.endsWith("/")) {
                    urlString = urlString.substring(0, urlBuffer.length()-1);
                }
                if (urlString.contains("://")) {
                    protocol = urlString.split("://")[0];
                    urlString = urlString.split("://")[1];
                }
                if (urlString.contains("/")) {
                    path = "/" + urlString.split("/", 2)[1];
                    urlString = urlString.split("/")[0];
                }
                if (urlString.contains(":")) {
                    port = Integer.parseInt(urlString.split(":")[1]);
                    urlString = urlString.split(":")[0];
                }
                this.url = new URL(protocol, this.headers.get("host").contains(":") ? this.headers.get("host").split(":")[0] : this.headers.get("host"), port, path);
                //this.url = new URL(this.headers.get("host"));
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL " + this.url);
            }
        }
        bytesRead += message.limit();
        isBodyRead();
        message.flip();
        message.rewind();
        metrics.addMethod(this.method.toString());

        if (this.headers.containsKey("accept-encoding") && this.headers.get("accept-encoding").contains("gzip")){
            ByteBuffer newMessage = (ByteBuffer) removeAcceptEncoding(message);
            newMessage.position(newMessage.limit());
            return newMessage;
        }
        return message;
    }

    private Buffer removeAcceptEncoding(ByteBuffer message) {
        ByteBuffer header = ByteBuffer.allocate(1024);
        int quantity = 0;
        for (int pos = 0; pos < message.limit(); pos++) {
            byte b = message.get(pos);
            if (b == 10) {
                if (checkAcceptEncodingHeader(header)) {
                    int i;
                    for (i = pos - quantity; i < message.limit() - quantity -1 ; i++) {
                        message.put(i, message.get(i + quantity + 1));
                    }
                    Buffer newMessage = message.limit(i);
                    return newMessage;
                } else {
                    header = ByteBuffer.allocate(1024);
                    quantity = 0;
                }
            } else {
                header.put(b);
                quantity++;
            }
        }
        return message;
    }

    private boolean checkAcceptEncodingHeader(ByteBuffer message) {
        byte[] bytes = message.array();
        String v = new String( bytes, Charset.forName("UTF-8") );
        String[] strings = v.split(":");
        return strings[0].toLowerCase().equals("accept-encoding");
    }

    /**
     * Evaluates if there is still information to read in the body section.
     */
    private void isBodyRead() {
        if (this.headers.containsKey("content-length") && bodyBytes  >= Long.valueOf(this.headers.get("content-length"))) {
            this.parsingStatus = ParsingStatus.FINISH;
        }
    }

    public URL getUrl() {
        return this.url;
    }

    /**
     * Receives a char and delegates its handling depending on which parsing section is currently active.
     *
     * @param c
     * @param pos
     */
    private void parseRequest (char c, int pos) {
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
                    }
                } else {
                    if (c == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
                parseHeader(c, pos);
                break;
            case BODY:
                parseBody(c);
                break;

        }
    }

    private void saveHeader(String StringBuilder, int pos) {
        String string = StringBuilder.toString();
        String stringHeaders[] = string.split(": ");
        if (stringHeaders.length <= 1) {
            stringHeaders = string.split(":");
            if (stringHeaders.length <= 1) {
                System.out.println("Something went wrong here");
                return;
            }
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
            parseResponse(c, messageAsChar.position());
            if (parsingSection == ParsingSection.BODY) {
                if (c == '\n') {
                    this.linesCount++;
                }
                if (Conversor.getInstance().isLeetOn()) {
                    if ((this.headers.containsKey("content-type") && this.headers.get("content-type").equals("text/plain")) ||
                            (this.url.getFile() != null && this.url.getFile().endsWith("txt"))) {
                        if (linesCount <= 1) {
                            message.put(i, (byte) c);
                        } else {
                            message.put(i, Conversor.leetChar(c));
                        }
                    }
                } else if (Conversor.getInstance().isFlipOn()) {
                    if ((this.headers.containsKey("content-type") && this.headers.get("content-type").equals("image/png")) ||
                            (this.url.getFile() != null && this.url.getFile().endsWith("png"))) {
                        if (this.image == null) {
                            this.image = new FlippedImage(i+1, "PNG");
                        } else {
                            this.image.putByte(message.get(i));
                        }
                    } else if ((this.headers.containsKey("content-type") && this.headers.get("content-type").equals("image/jpeg")) ||
                            (this.url.getFile() != null && this.url.getFile().endsWith("jpg"))) {
                        if (this.image == null) {
                            this.image = new FlippedImage(i, "JPEG");
                        } else {
                            this.image.putByte(message.get(i));
                        }
                    }
                }
            }
            i++;
        }
        if (this.image != null) {
            try {
                byte[] converted = this.image.getConvertedImage();
                if (converted.length < message.limit()) {
                    int k = this.image.getInitialPositionInMessage();
                    for (int j = 0; j < converted.length; j++) {
                        message.put(k, converted[j]);
                        k++;
                    }
                    Metrics.getInstance().addFlippedImage();
                }
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
        bytesRead += message.limit();
        isBodyRead();
        message.flip();
        message.rewind();
        message.position(pos);
    }


    /**
     * @param c
     * @param position
     */
    private void parseResponse(char c, int position) {
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
                parseHeader(c, position);
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

    /**
     * Depending on the character it receives and the current parsing sections, this function sets the
     * next state of the parsing section while parsing a chinked body
     * @param c
     */
    private void parseChunkedBody(char c) {
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

    /**
     * Depending on the character it receives and the current parsing sections, this function sets the
     * next state of the parsing section while parsing the body
     * @param c
     */
    private void parseBody(char c) {
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

    /**
     * @param b
     * @param  pos
     */
    private void parseHeader(char b, int pos) {
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
                    saveHeader(this.headerLine.toString().toLowerCase(), pos);
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                    this.headerLine = new StringBuilder();
                }
                break;
            case END_SECTION:
                if (b == '\n') {
                    this.parsingSection = ParsingSection.BODY;
                    this.linesCount = 0;
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
        this.linesCount = 0;
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
        this.linesCount = 0;
    }
}
