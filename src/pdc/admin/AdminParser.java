package pdc.admin;

import pdc.blocker.Blocker;
import pdc.conversor.Conversor;
import pdc.logger.HttpProxyLogger;

import java.nio.ByteBuffer;

/**
 * Created by sebastian on 5/25/17.
 */
public class AdminParser {

    private HttpProxyLogger logger = HttpProxyLogger.getInstance();
    private enum parsingSections { COMMAND, VALUE };
    private parsingSections section = parsingSections.COMMAND;

    private String enteredUser = null;

    private boolean addingUser = false;
    private String newUser;
    private boolean addingBlakclist = false;
    private boolean removingBlacklist = false;

    private String username;
    private boolean logged = false;

    /**
     * This function parses whatever the Admin writes in console.
     * Depending on which section is active, it consider the input a command or a value
     * @param messageBuffer
     * @return It returns an Admin response (enum)
     * @see AdminResponses
     */
    public AdminResponses parseCommands(ByteBuffer messageBuffer) {
        seeMessage(messageBuffer);

        AdminCommands command = AdminCommands.WRONG_COMMAND;
        byte[] auxBuffer = ByteBuffer.allocate(messageBuffer.capacity()).array();

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

    /**
     * Checks command vailidity
     * @param command
     * @return an Admin command (might be WRONG_COMMAND)
     * @see AdminCommands
     */
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
        if (command.toLowerCase().equals("set")) {
            return AdminCommands.SET;
        }
        if (command.toLowerCase().equals("remove")) {
            return AdminCommands.REMOVE;
        }
        return AdminCommands.WRONG_COMMAND;
    }

    /**
     * Checks the value validity (depending on the prior command written)
     * @param command
     * @param value
     * @return an Admin response
     * @see AdminResponses
     */
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
                if (this.addingUser || this.addingBlakclist || this.enteredUser == null)
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
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.addingUser  || this.addingBlakclist || this.enteredUser != null)
                    break;
                if (value.toLowerCase().equals("out")) {
                    this.logged = false;
                    this.username = null;
                    this.enteredUser = null;
                    return AdminResponses.LOG_OUT;
                }
                break;
            case USER:
                if (this.logged) {
                    return AdminResponses.ALREADY_LOGGED;
                }
                if (this.enteredUser != null || this.addingBlakclist)
                    break;
                if (this.addingUser) {
                    // If isValidUser(value) is true, then the user exists on the (user, pass) db
                    if(isValidUser(value)) {
                        return AdminResponses.USER_IN_USE;
                    }
                    this.newUser = value;
                    return AdminResponses.NEW_USER_OK;
                }
                this.enteredUser = value;
                return AdminResponses.OK_USERNAME;
            case PASS:
                if (this.enteredUser == null) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.addingBlakclist)
                    break;
                if (this.addingUser) {
                    boolean add = Admin.getAdmins().add(new Admin(this.newUser, value));
                    this.newUser = null;
                    this.addingUser = false;
                    if (add)
                        return AdminResponses.USER_CREATED;
                    return AdminResponses.INTERNAL_ERROR;
                }
                if (isValidUser(enteredUser) && isValidPass(enteredUser, value)) {
                    this.logged = true;
                    this.username = enteredUser;
                    return AdminResponses.CONNECTED;
                }
                this.username = null;
                this.enteredUser = null;
                return AdminResponses.ERROR_LOG_IN;
            case ADD:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (value.toLowerCase().equals("user")) {
                    this.addingUser = true;
                    return AdminResponses.ADD_USER;
                }
                if (value.toLowerCase().equals("blacklist")) {
                    this.addingBlakclist = true;
                    return AdminResponses.BLACK_LIST;
                }
                break;
            case REMOVE:
                if (this.addingUser  || this.addingBlakclist || this.enteredUser != null)
                    break;
                if (value.toLowerCase().equals("blacklist")) {
                    this.removingBlacklist = true;
                    return AdminResponses.REMOVE_BLACK_LIST;
                }
                break;
            case HOST:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.addingBlakclist) {
                    Blocker.getInstance().addBlockedHost(value);
                    this.addingBlakclist = false;
                    return AdminResponses.HOST_BLOCKED;
                }
                if (this.removingBlacklist) {
                    Blocker.getInstance().removeBlockedHost(value);
                    this.removingBlacklist = false;
                    return AdminResponses.UNBLOCKED_HOST;
                }
                break;
            case PORT:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.addingBlakclist) {
                    try {
                        Blocker.getInstance().addBlockedPort(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        return AdminResponses.ERROR_BAD_REQUEST;
                    }
                    this.addingBlakclist = false;
                    return AdminResponses.PORT_BLOCKED;
                }
                if (this.removingBlacklist) {
                    try {
                        Blocker.getInstance().removeBlockedPort(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        return AdminResponses.ERROR_BAD_REQUEST;
                    }
                    this.removingBlacklist = false;
                    return AdminResponses.UNBLOCKED_PORT;
                }
                break;
            case SET:
                if (!this.logged) {
                    return AdminResponses.UNAUTHORIZED;
                }
                if (this.addingUser || this.addingBlakclist)
                    break;
                if (value.toLowerCase().equals("leet-on")) {
                    Conversor.leetOn = true;
                    return AdminResponses.LEET_ON;
                }
                if (value.toLowerCase().equals("leet-off")) {
                    Conversor.leetOn = false;
                    return AdminResponses.LEET_OFF;
                }
                if (value.toLowerCase().equals("flip-on")) {
                    Conversor.flipOn = true;
                    return AdminResponses.FLIP_ON;
                }
                if (value.toLowerCase().equals("flip-off")) {
                    Conversor.flipOn = false;
                    return AdminResponses.FLIP_OFF;
                }
                break;
        }
        return AdminResponses.ERROR_BAD_REQUEST;
    }

    /**
     * Checks that the user requested exists
     * @param value
     * @return boolean
     */
    private boolean isValidUser(String value) {
        for (Admin a : Admin.getAdmins()) {
            if (a.getUsername().equals(value))
                return true;
        }
        return false;
    }

    /**
     * Checks that the password matches with the already validated user's password
     * @param
     * @param value
     * @return boolean
     */
    private boolean isValidPass(String user, String value) {
        for (Admin a : Admin.getAdmins()) {
            if (a.getUsername().equals(user) && a.getPassword().equals(value)) {
                this.logged = true;
                return true;
            }
        }
        return false;
    }

}
