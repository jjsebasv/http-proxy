package pdc.proxy;

import pdc.parser.ParsingSectionSection;
import pdc.parser.ParsingSection;
import pdc.parser.ParsingStatus;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
    private long bytesRead;

    public ParsingStatus getParsingStatus() {
        return parsingStatus;
    }

    public void setParsingStatus(ParsingStatus parsingStatus) {
        this.parsingStatus = parsingStatus;
    }

    public HttpMessage() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuilder();
        spaceCount = 0;
        bytesRead = 0;
        this.urlBuffer = new StringBuilder();
        method =  new StringBuilder();
        status = new StringBuilder();
        parsingSectionSection = ParsingSectionSection.START_LINE;
    }


    public void readRequest(ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        bytesRead += message.limit();
        for (char c: charBuffer.array()) { // FIXME : Dijimos en clase, nunca usar array(), esto asume siempre la data está desde el offset 0. Usar get() / charAt(), etc.
            parseRequest(c);
            // FIXME -- We should find a way to skip the lecture of the body
        }
        isBodyRead();

        message.flip();
        message.rewind();
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
                        // FIXME : Esto no maneja una request "tradicional" que no incluye el host en la primera linea (ie: GET / HTTP/1.1)
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

    private void isBodyRead() {
        if (this.headers.containsKey("Content-Length") && bytesRead  >= Long.valueOf(this.headers.get("Content-Length"))) {
            this.parsingStatus = ParsingStatus.FINISH;
        }
        //TODO QUE HACEMOS CUANDO ON TENEMOS CONTENT LENGTH Y VIENE TRASNFER CHUNKED
    }

    private void saveHeader(StringBuilder StringBuilder) {
        String string = StringBuilder.toString();
        String stringHeaders[] = string.split(": ");
        if (stringHeaders.length <= 1){
            System.out.println("Something went wrong here");
            return;
        }
        this.headers.put(stringHeaders[0], stringHeaders[1]);
    }

    public void readResponse(ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        for (char c: charBuffer.array()) {
            parseResponse(c);
        }
        bytesRead +=getBodyBytes(message);
        isBodyRead();
        message.flip();
        message.rewind();
    }

    private int getBodyBytes(ByteBuffer message) {
        if (this.bytesRead == 0) {
            int i = 0;
            boolean endLine = false;
            for (byte b: message.array()) { // FIXME : No usa array()!
                if (b == 10) {
                    endLine = true;
                } else {
                    if (b == 13 && endLine) {
                        break;
                    } else {
                        endLine = false;
                    }
                }
                i++;
            }
            return message.limit() - i;
        } else {
            return message.limit();
        }
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
                parseBody(c);
                break;
        }
    }

    // Si quedan los ultimos 4 bytes en distintas request se rompe todo
    private void parseBody(char c) {
        switch (parsingSectionSection) {
            case START_LINE:
                if (c == '\n') {
                    this.parsingSectionSection = ParsingSectionSection.END_LINE;
                }
                break;
            case END_LINE:
                if (c == '\r') {
                    this.parsingSectionSection = ParsingSectionSection.END_SECTION;
                } else {
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
                    saveHeader(this.headerLine);
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                    this.headerLine = new StringBuilder();
                }
                break;
            case END_SECTION:
                if (b == '\n') {
                    this.parsingSection = ParsingSection.BODY;
                    this.parsingSectionSection = ParsingSectionSection.START_LINE;
                    if (!headers.containsKey("Content-Length")) { // FIXME : Los headers HTTP son case-insensitive!!!
                        this.parsingStatus = ParsingStatus.FINISH;
                    }
                }
                break;
        }
    }

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
    }
}
