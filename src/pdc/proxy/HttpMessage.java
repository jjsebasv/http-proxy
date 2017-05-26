package pdc.proxy;

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
    private StringBuilder message;
    private ParsingSection parsingSection;
    private ParsingStatus parsingStatus;
    private Map<String, String> headers;
    private int spaceCount;
    private ParsingHeaderSection parsingHeaderSection;
    private StringBuffer headerLine;
    private StringBuffer method;
    private StringBuffer status;
    private StringBuffer urlBuffer;

    public HttpMessage() {
        this.parsingStatus = ParsingStatus.PENDING;
        this.parsingSection = ParsingSection.HEAD;
        this.message = new StringBuilder();
        this.headers = new HashMap<String, String>();
        headerLine = new StringBuffer();
        spaceCount = 0;
        this.urlBuffer = new StringBuffer();
        method =  new StringBuffer();
        status = new StringBuffer();
        parsingHeaderSection = ParsingHeaderSection.START_HEADER_LINE;
    }


    public void readRequest(ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        //System.out.print(charBuffer.toString());
        for (char c: charBuffer.array()) {
            parseRequest(c);
        }
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
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (b == '\n') {
                        this.parsingSection = ParsingSection.HEADERS;
                    }
                }
                break;
            case HEADERS:
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
                            }
                        }
                        break;
                }
                break;
            case BODY:
                break;

        }
    }

    private void saveHeader(StringBuffer stringBuffer) {
        String string = stringBuffer.toString();
        String stringHeaders[] = string.split(": ");
        this.headers.put(stringHeaders[0], stringHeaders[1]);
    }

    public void readResponse (ByteBuffer message) {
        message.flip();
        message.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(message);
        //System.out.print(charBuffer.toString());
        for (char c: charBuffer.array()) {
            parseResponse(c);
        }
        message.flip();
        message.rewind();
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
                            }
                        }
                        break;
                }
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

    public StringBuilder getMessage() {
        return this.message;
    }
}
