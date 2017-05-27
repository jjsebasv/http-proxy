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

    // FIXME -- Diff bet adding user and blacklist
    private boolean adding = false;
    private String newUser;

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
                if (this.adding)
                    break;
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
                if (this.adding)
                    break;
                if (value.toLowerCase().equals("in")) {
                    this.logRequest = true;
                    return AdminResponses.LOG_REQUEST;
                }
                if (value.toLowerCase().equals("out")) {
                    this.logged = false;
                    this.logRequest = false;
                    this.knownUsername = false;
                    this.username = null;
                    return AdminResponses.LOG_OUT;
                }
                break;
            case USER:
                if (!this.logRequest) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.adding) {
                    String auxUser = this.username;
                    if(isValidUser(value)) {
                        this.username = auxUser;
                        return AdminResponses.USER_IN_USE;
                    }
                    this.newUser = value;
                    this.username = auxUser;
                    return AdminResponses.NEW_USER_OK;
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
                if (this.adding) {
                    boolean add = Admin.getAdmins().add(new Admin(this.newUser, value));
                    this.newUser = null;
                    this.adding = false;
                    if (add)
                        return AdminResponses.USER_CREATED;
                    return AdminResponses.INTERNAL_ERROR;
                }
                if (isValidPass(value)) {
                    this.logged = true;
                    return AdminResponses.CONNECTED;
                }
                this.logRequest = false;
                this.knownUsername = false;
                this.username = null;
                return AdminResponses.ERROR_PASSWORD;
            case ADD:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                this.adding = true;
                if (value.toLowerCase().equals("user")) {
                    return AdminResponses.ADD_USER;
                }
                if (value.toLowerCase().equals("blacklist")) {
                    return AdminResponses.BLACK_LIST;
                }
                break;
            case HOST:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (!this.adding)
                    break;
                // TODO -- Add host blacklist
                this.adding = false;
                return AdminResponses.HOST_BLOCKED;
            case PORT:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (!this.adding)
                    break;
                // TODO -- Add port blacklist
                this.adding = false;
                return AdminResponses.PORT_BLOCKED;
            case SET:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.adding)
                    break;
                if (value.toLowerCase().equals("leet-on")) {
                    // TODO -- Turn on leet
                    return AdminResponses.LEET_ON;
                }
                if (value.toLowerCase().equals("leet-off")) {
                    // TODO -- Turn off leet
                    return AdminResponses.LEET_OFF;
                }
                if (value.toLowerCase().equals("flip-on")) {
                    // TODO -- Turn on flip
                    return AdminResponses.FLIP_ON;
                }
                if (value.toLowerCase().equals("flip-on")) {
                    // TODO -- Turn off flip
                    return AdminResponses.FLIP_OFF;
                }
                break;
        }
        return AdminResponses.ERROR_BAD_REQUEST;
    }

    private boolean isValidUser(String value) {
        for (Admin a : Admin.getAdmins()) {
            if (a.getUsername().equals(value)) {
                this.username = value;
                return true;
            }
        }
        return false;
    }

    private boolean isValidPass(String value) {
        for (Admin a : Admin.getAdmins()) {
            if (a.getUsername().equals(this.username) && a.getPassword().equals(value)) {
                this.logged = true;
                return true;
            }
        }
        return false;
    }

}
