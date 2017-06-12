package pdc.proxy;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.BindException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;

import nio.TCPProtocol;
import pdc.config.ProxyConfiguration;
import pdc.connection.ProxyConnection;
import pdc.logger.HttpProxyLogger;
import pdc.parser.ParsingStatus;

import static java.nio.channels.SelectionKey.*;

public class ClientHandler implements TCPProtocol {
    private ServerSocketChannel channel;
    private HttpProxyLogger logger;
    private ProxyConfiguration proxyConfiguration;
    String proxyHost;
    int proxyPort;
    InetSocketAddress listenAddress;
    Metrics metrics = Metrics.getInstance();


	public ClientHandler(Selector selector) {
        proxyConfiguration = ProxyConfiguration.getInstance();
        proxyHost = String.valueOf(proxyConfiguration.getProperty("proxy_host"));
        proxyPort = Integer.parseInt(proxyConfiguration.getProperty("proxy_port"));
        this.listenAddress = new InetSocketAddress(proxyHost, proxyPort);
        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(listenAddress);
            channel.register(selector, OP_ACCEPT);
            logger = HttpProxyLogger.getInstance();
            logger.info("Client proxy started");
        } catch (BindException e) {
            System.out.println("Address already in use");
        } catch (Exception e) {
            logger.error("Cant run proxy");
        }
	}


    /**
     *
     * Handles incoming connections to client port
     *
     * Handles the received key - which is acceptable - and creates the clientChannel
     *
     * Registers the key with a ProxyConnection that afterwards will be accessed by the
     * attachment field.
     * It uses the selector inside the key.
     *
     * @param key
     * @throws IOException
     *
     */
    public void handleAccept(SelectionKey key) {
        System.out.println("banda de gilada accept");
        try {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            ProxyConnection connection = new ProxyConnection(key.selector());
            connection.setClientChannel(clientChannel);
            clientChannel.register(key.selector(), OP_READ, connection);

            /* Logs and information */
            Socket socket = clientChannel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            SocketAddress localAddress = socket.getLocalSocketAddress();
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
                System.out.println("Accepted new client connection from " + localAddress + " to " + remoteAddress);
            }
            this.logger.info("Accepted new client connection from " + localAddress + " to " + remoteAddress);
            metrics.addAccess();
        } catch (ClosedChannelException e) {
            this.logger.error("Cannot register key as READ");
        } catch (IOException e) {
            this.logger.error("Cannot accept a connection to the proxy");
        }
    }


    /**
     * Handles incoming reads from both server and clients
     *
     * Handles the received key - which is readable - and gets its channel that afterwards will
     * be compared with the connection's server channel. If equal, the message will be sent to
     * the client, else to the server.
     *
     *
     *  @param key
     * @param key
     * @throws IOException
     */
    public void handleRead(SelectionKey key) {
        System.out.print("banda de gilada");
        SocketChannel keyChannel = (SocketChannel) key.channel();
        ProxyConnection connection = (ProxyConnection) key.attachment();
        /* Client socket channel has pending data */
        if (connection.getClientKey() == null) {
            connection.setClientKey(key);
        }

        if (connection.buffer.position() == connection.buffer.capacity() && connection.getServerChannel() != null){
            return;
        }

        try {
            long bytesRead = keyChannel.read(connection.buffer);

            if (bytesRead == -1) { // Did the other end close?
                connection.getHttpMessage().reset();
                closeChannels(key);
            } else if (bytesRead > 0) {
                // FIXME -- Ver que pasa cuando cierro el browser
               if (channelIsServerSide(keyChannel, connection)) {
                   connection.getHttpMessage().readResponse(connection.buffer);
                   sendToClient(key);
               } else {
                   connection.buffer = connection.getHttpMessage().readRequest(connection.buffer);
                   connection.buffer.position(connection.buffer.limit());
                   sendToServer(key);
               }

               metrics.addReceivedBytes(connection.getHttpMessage().getBytesRead());
               connection.getHttpMessage().setBytesRead(0);
            }
        }
        catch (IOException e) {
            logger.error("Cannot read from socket channel");
        }
    }


    /**
     * Handles incoming writes from both server and clients
     *
     * Handles the received key - which is writable - and gets its channel, which will be used to write the information
     * given in the buffer contained by de connection in the attachment.
     *
     *
     *  @param key
     * @param key
     * @throws IOException
     *
     */
    public void handleWrite(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        String side = channelIsServerSide(channel, connection)? "server" : "client";

        // DELETE THIS
        connection.buffer.flip();
        connection.buffer.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(connection.buffer);
        //System.out.println("Sending this to " + side);
        System.out.println(charBuffer.toString());
        connection.buffer.flip();
        connection.buffer.rewind();
        // UNTIL HERE


        long bytesWritten = 0;
        try {
            bytesWritten = channel.write(connection.buffer);

            if (!connection.buffer.hasRemaining()) { // Buffer completely written?
                // Nothing left, so no longer interested in writes

                key.interestOps(OP_READ);
                if (connection.getHttpMessage().getParsingStatus() == ParsingStatus.FINISH) {
                    System.out.println("Finish reading body " + connection.getHttpMessage().getUrl());
                    if (side.equals("client")) {
                        closeChannels(key);
                        connection.getHttpMessage().reset();
                    } else {
                        connection.getHttpMessage().resetRequest();
                    }
                }
            } else {
                if ((key.interestOps() & SelectionKey.OP_READ) == 0) {
                    // We now can read again
                    if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
                        System.out.println("buffer has: " + connection.buffer.remaining() + " remaining bytes");

                    key.interestOps(OP_READ | OP_WRITE);
                }
            }
            metrics.addTransferredBytes(bytesWritten);
            connection.buffer.compact(); // Make room for more data to be read in
        } catch (IOException e) {
            // If the write fails, write again (:
            // Broken pipe exception
            System.out.println(side);
            System.out.println(channel.isConnected());
            if (!channel.isConnected()) {
                System.out.print("la comiste nowi");
            }
            System.out.print(e.getMessage());
        }
    }


    /**
     * Recognize which channel belongs to the client and which to the server and manage their keys accordingly
     * for the client to be the one to be ready to receive information from the server
     *
     *
     * @param key
     *
     */
    private void sendToClient(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        writeInChannel(connection.getServerKey(), connection.getClientKey());
    }


    /**
     * Recognize which channel belongs to the client and which to the server and manage their keys accordingly
     * for the server to be the one to be ready to receive information from the client
     *
     * If the server channel is not yet initialed, it does so.
     *
     * @param key
     *
     */
    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();

        if (connection.getServerChannel() == null) {
            connectToRemoteServer(key);
        }

        writeInChannel(connection.getClientKey(), connection.getServerKey());
    }

    /**
     * Initializes the new connection with the server, creating a new channel from which it could be accessed afterwards.
     * The channel is saved in the proxyConnection present in the attachment.
     *
     *
     * @param key
     *
     */
    private void connectToRemoteServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        InetSocketAddress hostAddress = new InetSocketAddress(connection.getHttpMessage().getUrl().getHost(), connection.getHttpMessage().getUrl().getPort());
        SocketChannel serverChannel = null;
        try {
            //FIXME -- This throws java.net.ConnectException: Connection refused. We should do something like dns error
            serverChannel = SocketChannel.open(hostAddress);
            logger.info("Connecting proxy to: " + connection.getHttpMessage().getUrl().getHost() + " Port: " +  connection.getHttpMessage().getUrl().getPort());
            serverChannel.configureBlocking(false);
            SelectionKey serverKey = serverChannel.register(key.selector(), OP_READ, connection);
            connection.setServerKey(serverKey);
            connection.setServerChannel(serverChannel);
        } catch (UnresolvedAddressException e) {
            sendDNSError(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDNSError(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        String current = null;
        try {
            current = new File(".").getCanonicalPath();
            File file = new File(current + "/src/resources/error.html");
            FileInputStream fis = new FileInputStream(file);
            FileChannel fci = fis.getChannel();
            connection.buffer.clear();
            fci.read(connection.buffer);
            connection.buffer.flip();
            connection.buffer.rewind();
            connection.getClientChannel().write(connection.buffer);
            connection.getClientKey().cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Compares the given channel with the server channel present in the given connection and check if the channel belongs
     * to the server.
     *
     *
     * @param channel
     * @param connection
     * @return boolean whether if the channel belongs to the server
     */
    private boolean channelIsServerSide(SocketChannel channel, ProxyConnection connection) {
    	return channel.equals(connection.getServerChannel());
    }


    /**
     * Settles the keys accordingly for the source key to be readable and the dest key to be writable
     *
     *
     * @param sourceKey
     * @param destKey
     *
     */
    public void writeInChannel(SelectionKey sourceKey, SelectionKey destKey) {
        ProxyConnection connection = (ProxyConnection) sourceKey.attachment();
        if (!sourceKey.isValid())
            return;
        if (connection.buffer.hasRemaining()) {
            sourceKey.interestOps(SelectionKey.OP_READ);
            destKey.interestOps(SelectionKey.OP_WRITE);
        } else {
            sourceKey.interestOps(SelectionKey.OP_READ);
            destKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    /**
     * Given a key, a proxyConnection is determined, and both (client and server) channels are clossed.
     *
     *
     * @param key
     *
     */
    private void closeChannels(SelectionKey key){
        ProxyConnection connection = (ProxyConnection) key.attachment();
        try {
            if (connection.getClientChannel() != null) {
                logger.debug("Closing client side " + connection.getHttpMessage().getUrl());
                connection.getClientChannel().close();
                connection.setClientChannel(null);
            }
            if (connection.getServerChannel() != null) {
                logger.debug("Closing server side " + connection.getHttpMessage().getUrl());
                connection.getServerChannel().close();
                connection.setServerChannel(null);
            }
            if(connection.getClientKey() != null) {
                connection.getClientKey().cancel();
                connection.setClientKey(null);
            }
            if(connection.getServerKey() != null) {
                connection.getServerKey().cancel();
                connection.setServerKey(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}