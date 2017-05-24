package pdc.proxy;

import pdc.parser.HttpParser;
import pdc.parser.ParsingSection;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


public class HttpMessage {

    private StringBuilder message;

    private ByteBuffer messageBuffer;

    private boolean messageReady;
    private ParsingSection parsingSection;

    private Map<String, String> headers;
    private long bodyLengthRead;
    private String contentType;

    // Head attributes
    private String method;
    private String url;
    private String path;
    private String version;

    private String statusCode;
    private String statusMsg;

    // Request Response
    private boolean request;
    private boolean response;
    private String msgString;

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

    public void setMessageBuffer(ByteBuffer message) {
        CharBuffer messageBuffer = Charset.forName("UTF-8").decode(message);
        msgString = messageBuffer.toString();

        byte[] auxBuffer = ByteBuffer.allocate(message.capacity()).array();
        message.flip();
        byte b = 0;
        int i = 0;
        while (true) {
            switch (this.parsingSection) {
                case HEAD:

                    while( message.hasRemaining() ){
                        b = message.get();
                        auxBuffer[i++] = b;
                        if (b == '\n')
                            break;
                    }

                    if( auxBuffer[i-2] == '\r')
                        setHeadAttributes(new String(auxBuffer, 0, i - 2));
                    else
                        setHeadAttributes(new String(auxBuffer));

                    this.parsingSection = ParsingSection.HEADER;

                    if (!message.hasRemaining()) {
                        message.rewind();
                        message.compact();
                        return;
                    }

                case HEADER:

                    do {
                        i = 0;
                        while (message.hasRemaining()) {
                            if ( (b = message.get()) != '\n')
                                auxBuffer[i++] = b;
                            else {
                                if (i == 0 || i == 1) // case \r\n => 1 and \n => 0
                                    this.parsingSection = ParsingSection.BODY;
                                break;
                            }
                        }

                        if (i > 1) { // last case
                            if (auxBuffer[i - 1] == '\r')
                                setHeaders(new String(auxBuffer, 0, i - 1));
                            else
                                setHeaders(new String(auxBuffer));
                        }
                    } while (i > 1); // if is 1 it reached \r\n line

                    if (this.parsingSection != ParsingSection.BODY) {
                        message.rewind();
                        message.compact();
                        //this.messageReady = true;
                        return;
                    }

                case BODY:
                    this.contentType = headers.get("Content-Type");
                    while (message.hasRemaining() && (b = message.get()) != -1)
                        ; // TODO -- Here it should be the converter
                    message.rewind();
                    this.messageReady = true;
                    return;
            }
        }
    }


    private void setHeadAttributes(String message) {
        String headAttributes[] = message.split("\r\n")[0].split(" ");
        if (this.request = HttpParser.isRequest(message)) {
            this.method = headAttributes[0];
            int i;

            String[] aux;
            if(headAttributes[1].startsWith("http")) {
                headAttributes[1] = headAttributes[1].substring(headAttributes[1].indexOf("://")+3);
            }
            aux = headAttributes[1].split("/",1);
            if(aux.length > 1) {
                this.path = aux[1];
            } else {
                this.path = "";
            }
/*
            String relative = headAttributes[1];
            this.url = headAttributes[1].split("://")[1];
            if (this.url.endsWith("/"))
                this.url = this.url.substring(0, this.url.length() -1);
*/
            this.version = headAttributes[2];
        } else if (this.response = HttpParser.isResponse(message)) {
            this.statusCode = headAttributes[1];
            this.statusMsg = headAttributes[2];
        }
    }

    private void setHeaders(String string) {
        //FIXME -- RFC to this
        String stringHeaders[] = string.split(": ");
        if(stringHeaders.length != 2) {
            System.out.println(stringHeaders.length);
            //FIXME -- this is a negrada
            return;
        }
        headers.put(stringHeaders[0], stringHeaders[1]);
        if(stringHeaders[0].toLowerCase().equals("host")) {
            this.url = stringHeaders[1] + "/" + this.path;
            if (this.url.endsWith("/"))
                this.url = this.url.substring(0, this.url.length() -1);
        }
    }

    public StringBuilder getMessage() {
        return message;
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