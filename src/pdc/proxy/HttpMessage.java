package pdc.proxy;

import pdc.conversor.Conversor;
import pdc.parser.ParsingHeaderSection;
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
    private ParsingHeaderSection parsingHeaderSection;
    private StringBuffer headerLine;
    private StringBuffer method;
    private StringBuffer status;
    private StringBuffer urlBuffer;
    private long bytesRead;

    public HttpMessage() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuffer();
        spaceCount = 0;
        bytesRead = 0;
        this.urlBuffer = new StringBuffer();
        method =  new StringBuffer();
        status = new StringBuffer();
        parsingHeaderSection = ParsingHeaderSection.START_HEADER_LINE;
    }


    public void readRequest(ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        System.out.println(charBuffer.toString());
        bytesRead += message.limit();
        for (char c: charBuffer.array()) {
            parseRequest(c);
            // FIXME -- We should find a way to skip the lecture of the body
        }
        parseBody();
        message.flip();
        message.rewind();
    }

    public URL getUrl() {
        return this.url;
    }

    private void parseRequest (char b) {
        switch (parsingSection) {
            case HEAD:
                if (spaceCount == 0) {
                    if (b != ' ') {
                        this.method.append(b);
                    } else {
                        spaceCount++;
                    }
                } else if (spaceCount == 1) {
                    if (b != ' ') {
                        this.urlBuffer.append(b);
                    } else {
                        spaceCount++;
                        try {
                            this.url = new URL(this.urlBuffer.toString());
                        } catch (MalformedURLException e) {
                            System.out.println("Malformed URL " + this.url);
                        }
                    }
                } else {
                    if (b == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
                parseHeader(b);
                break;
            case BODY:
                break;

        }
    }

    private void parseBody() {
        if (this.headers.containsKey("Content-Length") && (bytesRead - 2)  == Long.valueOf(this.headers.get("Content-Length"))) {
            this.parsingStatus = ParsingStatus.FINISH;
            System.out.println("FINISH READING BODY");
            reset();
        }
    }

    private void saveHeader(StringBuffer stringBuffer) {
        String string = stringBuffer.toString();
        String stringHeaders[] = string.split(": ");
        if (stringHeaders.length <= 1){
            System.out.println("Something went wrong here");
            return;
        }
        this.headers.put(stringHeaders[0], stringHeaders[1]);
    }

    public void readResponse (ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        System.out.println(charBuffer.toString());
        for (char c: charBuffer.array()) {
            parseResponse(c);
        }
        bytesRead +=getBodyBytes(message);
        parseBody();
        message.flip();
        message.rewind();
    }

    private int getBodyBytes(ByteBuffer message) {
        if (this.bytesRead == 0) {
            int i = 0;
            boolean endLine = false;
            for (byte b: message.array()) {
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

    private void parseResponse(char b) {
        switch (parsingSection) {
            case HEAD:
                if (spaceCount == 0) {
                    if (b != ' ') {
                        this.status.append(b);
                    } else {
                        spaceCount++;
                    }
                } else {
                    if (b == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
                parseHeader(b);
                break;
            case BODY:
                break;
        }
    }

    public ParsingStatus getParsingStatus() {
        return this.parsingStatus;
    }

    public Map<String, String> getHaders() {
        return this.headers;
    }

    private void parseHeader(char b) {
        switch (parsingHeaderSection) {
            case START_HEADER_LINE:
                if (b != '\n' && b != '\r') {
                    this.headerLine.append(b);
                } else if (b == '\r') {
                    if (this.headerLine.length() == 0) {
                        this.parsingHeaderSection = ParsingHeaderSection.END_HEADERS_SECTION;
                    } else {
                        this.parsingHeaderSection = ParsingHeaderSection.END_HEADER_LINE;
                    }
                }
                break;
            case END_HEADER_LINE:
                if (b == '\n') {
                    saveHeader(this.headerLine);
                    this.parsingHeaderSection = ParsingHeaderSection.START_HEADER_LINE;
                    this.headerLine = new StringBuffer();
                }
                break;
            case END_HEADERS_SECTION:
                if (b == '\n') {
                    this.parsingSection = ParsingSection.BODY;
                    if (!headers.containsKey("Content-Length")) {
                        this.parsingStatus = ParsingStatus.FINISH;
                        reset();
                    }
                }
                break;
        }
    }

    private void reset() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuffer();
        spaceCount = 0;
        this.urlBuffer = new StringBuffer();
        method =  new StringBuffer();
        status = new StringBuffer();
        parsingHeaderSection = ParsingHeaderSection.START_HEADER_LINE;
        this.bytesRead = 0;
    }
}
