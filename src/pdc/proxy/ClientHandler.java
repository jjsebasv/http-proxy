package pdc.proxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
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
            logger.error("Address already in use");
        } catch (Exception e) {
            logger.error("Cant run proxy");
        }
	}

    /**
     * Accept connections to HTTP Proxy
     * @param key connection
     * @throws IOException
     */
    public void handleAccept(SelectionKey key) {
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
            this.logger.info("Accepted new client connection from " + localAddress + " to " + remoteAddress);
        } catch (ClosedChannelException e) {
            this.logger.error("Cannot register key as READ");
        } catch (IOException e) {
            this.logger.error("Cannot accept a connection to the proxy");
        }
    }

    /**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
    public void handleRead(SelectionKey key) {
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

            String side = channelIsServerSide(keyChannel, connection)? "server" : "client";

            if (bytesRead == -1) { // Did the other end close?
                logger.debug("Finish reading from " + connection.getHttpMessage().getUrl());
                keyChannel.close();
                connection.getHttpMessage().reset();
            } else if (bytesRead > 0) {
                // FIXME -- Ver que pasa cuando cierro el browser
               if (channelIsServerSide(keyChannel, connection)) {
                   connection.getHttpMessage().readResponse(connection.buffer);
                   sendToClient(key);
               } else {
                   connection.getHttpMessage().readRequest(connection.buffer);
                   sendToServer(key);
                   //connection.getHttpMessage().reset();
               }
            }
        }
        catch (IOException e) {
            logger.error("Cannot read from socket channel");
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        //connection.buffer.flip();
        // ** Prepare buffer for writing
        SocketChannel channel = (SocketChannel) key.channel();

        // DELETE THIS
        connection.buffer.flip();
        connection.buffer.rewind();
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(connection.buffer);
        String side = channelIsServerSide(channel, connection)? "server" : "client";
        System.out.println(charBuffer.toString());
        connection.buffer.flip();
        connection.buffer.rewind();
        // UNTIL HERE

        long bytesWritten = channel.write(connection.buffer);

        if (!connection.buffer.hasRemaining()) {
            key.interestOps(OP_READ);
            if (connection.getHttpMessage().getParsingStatus() == ParsingStatus.FINISH) {
                logger.info("Finish reading body " + connection.getHttpMessage().getUrl());
                if (side.equals("client")) {
                    if (connection.getServerChannel() != null) {
                        connection.getServerChannel().close();
                        connection.setServerChannel(null);
                    }
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
                    logger.info("buffer has: " + connection.buffer.remaining() + " remaining bytes");

                key.interestOps(OP_READ | OP_WRITE);
            }
        }
        connection.buffer.compact(); // Make room for more data to be read in
    }

    private void sendToClient(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        writeInChannel(connection.getServerKey(), connection.getClientKey());
    }


    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();

        try {
            if (connection.getServerChannel() == null) {
                connectToRemoteServer(key);
            }
            writeInChannel(connection.getClientKey(), connection.getServerKey());
        }
        catch(ClosedByInterruptException e) {
            logger.error(e.toString());
        }
        catch(IOException e) {
            logger.error(e.toString());
        }
    }

    private void connectToRemoteServer(SelectionKey key) throws IOException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        InetSocketAddress hostAddress = new InetSocketAddress(connection.getHttpMessage().getUrl().getHost(), 80);
        SocketChannel serverChannel = SocketChannel.open(hostAddress);
        logger.info("Connecting proxy to: " + connection.getHttpMessage().getUrl() + " - CLIENT CHANNEL " + connection.getClientChannel().hashCode());
        serverChannel.configureBlocking(false);
        SelectionKey serverKey = serverChannel.register(key.selector(), OP_READ, connection);
        connection.setServerKey(serverKey);
        connection.setServerChannel(serverChannel);
    }


    /**
     * Ask if a channel is server side
     * @param channel
     * @return
     */
    private boolean channelIsServerSide(SocketChannel channel, ProxyConnection connection) {
    	return channel.equals(connection.getServerChannel());
    }

	/**
     * Write data to a specific channel
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

    private void closeChannels(SelectionKey key){
        ProxyConnection connection = (ProxyConnection) key.attachment();
        try {
            if (connection.getClientChannel() != null)
                connection.getClientChannel().close();
            if (connection.getServerChannel() != null)
                connection.getServerChannel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
