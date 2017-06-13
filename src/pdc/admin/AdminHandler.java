package pdc.admin;

import com.sun.corba.se.spi.activation.Server;
import pdc.config.ProxyConfiguration;
import pdc.connection.AdminConnection;
import pdc.connection.Connection;
import pdc.logger.HttpProxyLogger;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Created by sebastian on 5/25/17.
 */
public class AdminHandler {

    private ProxyConfiguration proxyConfiguration = ProxyConfiguration.getInstance();
    private HttpProxyLogger logger = HttpProxyLogger.getInstance();
    private AdminParser parser = new AdminParser();
    private ServerSocketChannel adminServerChannel;
    private SocketChannel adminClientChannel;
    private SocketChannel serverChannel;
    private InetSocketAddress listenAddress;
    private boolean firstTime = true;

    public AdminHandler (String host, int port, Selector selector) throws IOException {
        listenAddress = new InetSocketAddress(host, port);

        try {
            this.adminServerChannel = ServerSocketChannel.open();
            adminServerChannel.configureBlocking(false);
            adminServerChannel.socket().bind(listenAddress);
            adminServerChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("New admin handler started");
        } catch (BindException e) {
            logger.error("Address already in use");
        } catch (Exception e) {
            logger.error("Cant run proxy");
        }
    }

    /**
     * Handles incoming connections to admin port.
     *
     * Creates a new ChannelBuffers object which will contain the read and write
     * buffers related to the channel.
     *
     */
    public void handleAccept (SelectionKey key) {
        SocketChannel clientChannel = null;
        try {
            clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            AdminConnection connection = new AdminConnection(key.selector());
            connection.setClientChannel(clientChannel);
            clientChannel.register(key.selector(), SelectionKey.OP_WRITE, connection);

            this.adminClientChannel = clientChannel;

            /* Logs and information */
            Socket socket = clientChannel.socket();
            SocketAddress localAddress = socket.getLocalSocketAddress();
            this.logger.info("[Admin] Accepted new connection from " + localAddress);
        }  catch (ClosedChannelException e) {
            this.logger.error("[Admin] Closed channel " + clientChannel.toString());
        } catch (IOException e) {
            this.logger.error("[Admin] Cannot accept a connection to the proxy");
        }
    }

    /**
     * Handles incoming reads from administrators.
     *
     * Parses the message and validates the syntax.
     *
     */
    public void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        AdminConnection connection = (AdminConnection) key.attachment();

        int bytesRead = channel.read(connection.buffer);
        if (bytesRead == -1) {
            logger.debug("[Admin] Error while reading from channel");
            channel.close();
            key.cancel();
        } else if(bytesRead > 0) {
            if (channel.equals(connection.getClientChannel())) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } else {
            // Not really sure about this
            logger.debug("[Admin] Partial reading");
        }

    }

    public void handleWrite(SelectionKey key) throws  IOException {
        AdminConnection connection = (AdminConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (channel.equals(connection.getClientChannel())) {
            if (firstTime) {
                channel.write(ByteBuffer.wrap(AdminConstants.WELCOME_MSG));
                channel.write(ByteBuffer.wrap(AdminConstants.LINE_SEPARATOR));
                firstTime = false;
            } else {
                connection.buffer.flip();
                AdminResponses response = parser.parseCommands(connection.buffer);

                connection.buffer.clear();
                respond(response, connection.buffer);

                connection.buffer.flip();
                channel.write(connection.buffer);

            }
            connection.buffer.clear();
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Writes into the admin client the reponse to it request.
     *
     * @param response the response indicator
     * @param buffer the channel to which it should write
     *
     */
    private void respond(AdminResponses response, ByteBuffer buffer) throws IOException{
        // FIXME : Por qué asumís que podés escribir y vas a poder escribir todos estos bytes?
        switch (response) {
            case HELP:
                buffer.put(AdminConstants.HELP);
                break;
            case LOG_REQUEST:
                buffer.put(AdminConstants.LOG_REQUEST);
                break;
            case ERROR_USERNAME:
                buffer.put(AdminConstants.WRONG_USERNAME);
                break;
            case KNOWN_USERNAME:
                buffer.put(AdminConstants.KNOWN_USER);
                break;
            case ERROR_PASSWORD:
                buffer.put(AdminConstants.WRONG_PASSWORD);
                break;
            case CONNECTED:
                buffer.put(AdminConstants.LOGGED_IN);
                break;
            case LOG_OUT:
                buffer.put(AdminConstants.LOGGED_OUT);
                break;
            case ALL_METRICS:
                buffer.put(AdminConstants.metricsRequested("all"));
                break;
            case BYTES_SENT:
                buffer.put(AdminConstants.metricsRequested("bytes_sent"));
                break;
            case BYTES_RECEIVED:
                buffer.put(AdminConstants.metricsRequested("bytes_received"));
                break;
            case HISTOGRAMS:
                buffer.put(AdminConstants.metricsRequested("method_histograms"));
                break;
            case CONVERTED_CHARS:
                buffer.put(AdminConstants.metricsRequested("converted_chars"));
                break;
            case FLIPPED_IMAGES:
                buffer.put(AdminConstants.metricsRequested("flipped_immages"));
                break;
            case TOTAL_ACCESSES:
                buffer.put(AdminConstants.metricsRequested("total_acceses"));
                break;
            case USER_IN_USE:
                buffer.put(AdminConstants.USERNAME_IN_USE);
                break;
            case NEW_USER_OK:
                buffer.put(AdminConstants.ADDING_PASS);
                break;
            case USER_CREATED:
                buffer.put(AdminConstants.USER_CREATED);
                break;
            case ADD_USER:
                buffer.put(AdminConstants.ADDING_USER);
                break;
            case BLACK_LIST:
                buffer.put(AdminConstants.ADDING_BLACKLIST);
                break;
            case HOST_BLOCKED:
                buffer.put(AdminConstants.BLOCKED_HOST);
                break;
            case PORT_BLOCKED:
                buffer.put(AdminConstants.BLOCKED_PORT);
                break;
            case LEET_ON:
                buffer.put(AdminConstants.LEET_ON);
                break;
            case LEET_OFF:
                buffer.put(AdminConstants.LEET_OFF);
                break;
            case FLIP_ON:
                buffer.put(AdminConstants.FLIP_ON);
                break;
            case FLIP_OFF:
                buffer.put(AdminConstants.FLIP_OFF);
                break;
            case UNAUTHORIZED:
                buffer.put(AdminConstants.UNAUTHORIZED);
                break;
            case ERROR_BAD_REQUEST:
                buffer.put(AdminConstants.WRONG_COMMAND);
                break;
            default:
                buffer.wrap(AdminConstants.INTERNAL_ERROR);
                break;

        }
    }

    public SocketChannel getAdminChannel() { return this.adminClientChannel; }
    public ServerSocketChannel getAdminServerChannel() { return this.adminServerChannel; }

}
