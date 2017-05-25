package pdc.admin;

import com.google.common.base.Charsets;
import pdc.logger.HttpProxyLogger;
import pdc.parser.ParsingSection;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by sebastian on 5/25/17.
 */
public class AdminParser {

    private HttpProxyLogger logger = HttpProxyLogger.getInstance();
    private enum parsingSections { COMMAND, VALUE };
    private parsingSections section = parsingSections.COMMAND;

    private boolean logRequest = false;
    private boolean knownUsername = false;
    private boolean logged = false;
    private String username;

    public AdminResponses parseCommands(ByteBuffer messageBuffer) {
        seeMessage(messageBuffer);

        AdminCommands command = AdminCommands.WRONG_COMMAND;
        byte[] auxBuffer = ByteBuffer.allocate(messageBuffer.capacity()).array();
        messageBuffer.flip();

        byte b;
        int i = 0;
        while (true) {
            switch (section) {
                case COMMAND:
                    while (messageBuffer.hasRemaining()) {
                        b = messageBuffer.get();
                        auxBuffer[i++] = b;
                        if (b == ' ')
                            break;
                    }

                    command = isValidCommand(new String(auxBuffer, 0, i-1));
                    if (!messageBuffer.hasRemaining() ||
                            command == AdminCommands.WRONG_COMMAND) {
                        messageBuffer.rewind();
                        messageBuffer.compact();
                        return AdminResponses.ERROR_BAD_REQUEST;
                    }

                    section = parsingSections.VALUE;
                    break;

                case VALUE:
                    i = 0;
                    while (messageBuffer.hasRemaining()) {
                        b = messageBuffer.get();
                        auxBuffer[i++] = b;
                        if (b == '\n')
                            break;
                    }
                    return checkValue(command, new String(auxBuffer, 0, i-1));

            }

        }
    }

    private void seeMessage(ByteBuffer messageBuffer) {
        String messageString = new String(messageBuffer.array());

        logger.info("[Admin] Command written " + messageString.split("\n")[0]);
    }

    private AdminCommands isValidCommand(String command) {
        if (command.toLowerCase().equals("get")) {
            return AdminCommands.GET;
        }
        if (command.toLowerCase().equals("log")) {
            return AdminCommands.LOG;
        }
        if (command.toLowerCase().equals("add")) {
            return AdminCommands.ADD;
        }
        if (command.toLowerCase().equals("user")) {
            return AdminCommands.USER;
        }
        if (command.toLowerCase().equals("pass")) {
            return AdminCommands.PASS;
        }
        return AdminCommands.WRONG_COMMAND;
    }

    private AdminResponses checkValue(AdminCommands command, String value) {
        this.section = parsingSections.COMMAND;
        switch (command) {
            case GET:
                if (value.toLowerCase().equals("help")) {
                    return AdminResponses.HELP;
                }
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (value.toLowerCase().equals("all-metrics")) {
                    return AdminResponses.ALL_METRICS;
                }
                if (value.toLowerCase().equals("bytes-sent")) {
                    return AdminResponses.BYTES_SENT;
                }
                if (value.toLowerCase().equals("bytes-received")) {
                    return AdminResponses.BYTES_RECEIVED;
                }
                if (value.toLowerCase().equals("method-histograms")) {
                    return AdminResponses.HISTOGRAMS;
                }
                if (value.toLowerCase().equals("converted-chars")) {
                    return AdminResponses.CONVERTED_CHARS;
                }
                if (value.toLowerCase().equals("flipped-images")) {
                    return AdminResponses.FLIPPED_IMAGES;
                }
                if (value.toLowerCase().equals("total-accesses")) {
                    return AdminResponses.TOTAL_ACCESSES;
                }
                break;
            case LOG:
                if (value.toLowerCase().equals("in")) {
                    this.logRequest = true;
                    return AdminResponses.LOG_REQUEST;
                }
                if (value.toLowerCase().equals("out")) {
                    this.logged = false;
                    this.logRequest = false;
                    this.knownUsername = false;
                    return AdminResponses.LOG_OUT;
                }
                break;
            case USER:
                if (!this.logRequest) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (isValidUser(value)) {
                    this.knownUsername = true;
                    return AdminResponses.KNOWN_USERNAME;
                }
                this.logRequest = false;
                return AdminResponses.ERROR_USERNAME;
            case PASS:
                if (!knownUsername) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (isValidPass(value)) {
                    this.logged = true;
                    return AdminResponses.CONNECTED;
                }
                this.logRequest = false;
                this.knownUsername = false;
                return AdminResponses.ERROR_PASSWORD;
        }
        return AdminResponses.ERROR_BAD_REQUEST;
    }

    private boolean isValidUser(String value) {
        return false;
    }

    private boolean isValidPass(String value) {
        return false;
    }

}
