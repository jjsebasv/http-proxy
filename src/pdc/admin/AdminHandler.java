package pdc.admin;

import com.sun.corba.se.spi.activation.Server;
import pdc.config.ProxyConfiguration;
import pdc.connection.AdminConnection;
import pdc.logger.HttpProxyLogger;

import java.io.IOException;
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

    private int bufferSize;

    public AdminHandler (String host, int port, Selector selector) throws IOException {
        this.bufferSize = Integer.parseInt(proxyConfiguration.getProperty("buffer_size"));
        InetSocketAddress listenAddress = new InetSocketAddress(host, port);

        this.adminServerChannel = ServerSocketChannel.open();
        adminServerChannel.socket().bind(listenAddress);
        adminServerChannel.configureBlocking(false);
        adminServerChannel.register(selector, SelectionKey.OP_ACCEPT);

        logger.info("New admin handler started");
    }

    /**
     * Handles incoming connections to admin port.
     *
     * Creates a new ChannelBuffers object which will contain the read and write
     * buffers related to the channel.
     *
     */
    public void handleAccept (SelectionKey key) throws IOException {
        AdminConnection connection = new AdminConnection(key.selector());

        SocketChannel clientChannel = null;
        try {
            clientChannel = this.adminServerChannel.accept();
        } catch (IOException e) {
            this.logger.error("[Admin] Cannot accept a connection to the proxy");
        }

        Socket socket = clientChannel.socket();

        try {
            clientChannel.configureBlocking(false);
        } catch (IOException e) {
            this.logger.error("[Admin] Cannot configure the channel as non blocking");
        }

        connection.setClientChannel(clientChannel);

        SocketAddress remoteAddress = socket.getRemoteSocketAddress();
        SocketAddress localAddress = socket.getLocalSocketAddress();

        this.logger.info("[Admin] Accepted new connection from " + localAddress);

        try {
            SelectionKey clientKey = clientChannel.register(key.selector(), SelectionKey.OP_READ);
            clientKey.attach(connection);
        } catch (ClosedChannelException e) {
            this.logger.error("[Admin] Closed channel " + clientChannel.toString());
        }

        this.adminClientChannel = clientChannel;

        clientChannel.write(ByteBuffer.wrap(AdminConstants.WELCOME_MSG));
        clientChannel.write(ByteBuffer.wrap(AdminConstants.LINE_SEPARATOR));
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
            AdminResponses response = parser.parseCommands(connection.buffer);
            respond(response, channel);
            connection.buffer.clear();
        } else {
            logger.debug("[Admin] Partial reading");
        }

    }


    /**
     * Writes into the admin client the reponse to it request.
     *
     * @param response the response indicator
     * @param channel the channel to which it should write
     *
     */
    private void respond(AdminResponses response, SocketChannel channel) throws IOException{
        switch (response) {
            case HELP:
                channel.write(ByteBuffer.wrap(AdminConstants.HELP));
                break;
            case LOG_REQUEST:
                channel.write(ByteBuffer.wrap(AdminConstants.LOG_REQUEST));
                break;
            case ERROR_USERNAME:
                channel.write(ByteBuffer.wrap(AdminConstants.WRONG_USERNAME));
                break;
            case KNOWN_USERNAME:
                channel.write(ByteBuffer.wrap(AdminConstants.KNOWN_USER));
                break;
            case ERROR_PASSWORD:
                channel.write(ByteBuffer.wrap(AdminConstants.WRONG_PASSWORD));
                break;
            case CONNECTED:
                channel.write(ByteBuffer.wrap(AdminConstants.LOGGED_IN));
                break;
            case LOG_OUT:
                channel.write(ByteBuffer.wrap(AdminConstants.LOGGED_OUT));
                break;
            case ALL_METRICS:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("all")));
                break;
            case BYTES_SENT:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("bytes_sent")));
                break;
            case BYTES_RECEIVED:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("bytes_received")));
                break;
            case HISTOGRAMS:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("method_histograms")));
                break;
            case CONVERTED_CHARS:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("converted_chars")));
                break;
            case FLIPPED_IMAGES:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("flipped_immages")));
                break;
            case TOTAL_ACCESSES:
                channel.write(ByteBuffer.wrap(AdminConstants.metricsRequested("total_acceses")));
                break;
            case USER_IN_USE:
                channel.write(ByteBuffer.wrap(AdminConstants.USERNAME_IN_USE));
                break;
            case NEW_USER_OK:
                channel.write(ByteBuffer.wrap(AdminConstants.ADDING_PASS));
                break;
            case USER_CREATED:
                channel.write(ByteBuffer.wrap(AdminConstants.USER_CREATED));
                break;
            case ADD_USER:
                channel.write(ByteBuffer.wrap(AdminConstants.ADDING_USER));
                break;
            case BLACK_LIST:
                channel.write(ByteBuffer.wrap(AdminConstants.ADDING_BLACKLIST));
                break;
            case HOST_BLOCKED:
                channel.write(ByteBuffer.wrap(AdminConstants.BLOCKED_HOST));
                break;
            case PORT_BLOCKED:
                channel.write(ByteBuffer.wrap(AdminConstants.BLOCKED_PORT));
                break;
            case LEET_ON:
                channel.write(ByteBuffer.wrap(AdminConstants.LEET_ON));
                break;
            case LEET_OFF:
                channel.write(ByteBuffer.wrap(AdminConstants.LEET_OFF));
                break;
            case FLIP_ON:
                channel.write(ByteBuffer.wrap(AdminConstants.FLIP_ON));
                break;
            case FLIP_OFF:
                channel.write(ByteBuffer.wrap(AdminConstants.FLIP_OFF));
                break;
            case UNAUTHORIZED:
                channel.write(ByteBuffer.wrap(AdminConstants.UNAUTHORIZED));
                break;
            case ERROR_BAD_REQUEST:
                channel.write(ByteBuffer.wrap(AdminConstants.WRONG_COMMAND));
                break;
            default:
                channel.write(ByteBuffer.wrap(AdminConstants.INTERNAL_ERROR));
                break;

        }
    }

    public SocketChannel getAdminChannel() { return this.adminClientChannel; }
    public ServerSocketChannel getAdminServerChannel() { return this.adminServerChannel; }

}
