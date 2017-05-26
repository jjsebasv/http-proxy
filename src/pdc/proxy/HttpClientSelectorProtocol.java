package pdc.proxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;

import nio.TCPProtocol;
import pdc.config.ProxyConfiguration;
import pdc.logger.HttpProxyLogger;
import pdc.parser.ParsingStatus;

public class HttpClientSelectorProtocol implements TCPProtocol {
    private int bufferSize;
    private ServerSocketChannel channel;
    private HttpProxyLogger logger;
    private ProxyConfiguration proxyConfiguration;
    private Selector selector;
    String proxyHost;
    int proxyPort;
    InetSocketAddress listenAddress;


	public HttpClientSelectorProtocol(Selector selector) throws IOException {
        this.selector = selector;
        proxyConfiguration = ProxyConfiguration.getInstance();
        proxyHost = String.valueOf(proxyConfiguration.getProperty("proxy_host"));
        proxyPort = Integer.parseInt(proxyConfiguration.getProperty("proxy_port"));
        this.bufferSize = Integer.parseInt(proxyConfiguration.getProperty("buffer_size"));
        this.listenAddress = new InetSocketAddress(proxyHost, proxyPort);
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(listenAddress);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        logger = HttpProxyLogger.getInstance();
        logger.info("Client proxy started");
	}
    
	/**
     * Accept connections to HTTP Proxy
     * @param key connection
     * @throws IOException
     */
    public void handleAccept(SelectionKey key) {
        ProxyConnection connection = new ProxyConnection(key.selector());
        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = null;

        try {
            newChannel = keyChannel.accept();
        } catch (IOException e) {
            this.logger.error("Cannot accept a connection to the proxy");
        }

        try {
            newChannel.configureBlocking(false);
        } catch (IOException e) {
            this.logger.error("Cannot configure the channel as non blocking");
        }

        connection.setClientChannel(newChannel);
        connection.setClientKey(key);

        try {
            newChannel.register(this.selector, SelectionKey.OP_READ, connection);
        } catch (ClosedChannelException e) {
            this.logger.error("Cannot register key as READ");
        }

        Socket socket = newChannel.socket();
        SocketAddress remoteAddress = socket.getRemoteSocketAddress();
        SocketAddress localAddress = socket.getLocalSocketAddress();
        if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
            System.out.println("Accepted new client connection from " + localAddress + " to " + remoteAddress);
        }
        this.logger.info("Accepted new client connection from " + localAddress + " to " + remoteAddress);
    }

	/**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
	public void handleRead(SelectionKey key) throws UnsupportedEncodingException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
		SocketChannel keyChannel = (SocketChannel) key.channel();

        int bytesRead = -1;
        connection.buffer = ByteBuffer.allocate(bufferSize);
        try {
            bytesRead = keyChannel.read(connection.buffer);
        } catch (IOException e) {
            logger.error("Cannot read from socket channel");
        }

        if (bytesRead == -1) {
            try {
                keyChannel.close();
            } catch (IOException e) {
                logger.error("Cannot close socket channel");
            }
            key.cancel();
            logger.debug("Finish reading from " + connection.getHttpMessage().getUrl());
            return;
        }

        if (channelIsServerSide(keyChannel, connection)) {
            connection.getHttpMessage().readResponse(connection.buffer);
            sendToClient(key);
        } else {
            connection.getHttpMessage().readRequest(connection.buffer);
            sendToServer(key);
        }
	}

    private void sendToClient(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
            System.out.println("proxy is writing to client");
        }
        writeInChannel(connection.getClientChannel());
    }


    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();

        try {
            if (connection.getServerChannel() == null) {
                InetSocketAddress hostAddress = new InetSocketAddress(connection.getHttpMessage().getUrl().getHost(), 80);
                SocketChannel serverChannel = SocketChannel.open(hostAddress);
                logger.info("Connecting proxy to: " + connection.getHttpMessage().getUrl() + " - CLIENT CHANNEL " + connection.getClientChannel().hashCode());
                serverChannel.configureBlocking(false);
                serverChannel.register(this.selector, SelectionKey.OP_READ, connection);
                connection.setServerChannel(serverChannel);
            }

            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
                System.out.println("Proxy is writing to: " + connection.getHttpMessage().getUrl());
            }
            writeInChannel(connection.getServerChannel());
        }
        catch(ClosedByInterruptException e) {
            logger.error(e.toString());
            System.out.println("ClosedByInterruptException");
        }
        catch(AsynchronousCloseException e) {
            System.out.println("AsynchronousCloseException");
        }
        catch(UnresolvedAddressException e) {
            logger.error(e.toString());
            System.out.println("UnresolvedAddressException");
        }
        catch(UnsupportedAddressTypeException e) {
            logger.error(e.toString());
            System.out.println("UnsupportedAddressTypeException");
        }
        catch(SecurityException e) {
            logger.error(e.toString());
            System.out.println("SecurityException");
        }
        catch(IOException e) {
            logger.error(e.toString());
            System.out.println("IOException");
        }
    }

    public void handleWrite(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            channel.socket().setSendBufferSize(8192);
        } catch (SocketException e) {
            logger.warn("Cannot set buffer size");
        }
        if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
            try {
                System.out.println("socket can send " + channel.socket().getSendBufferSize() + " bytes per write operation");
            } catch (SocketException e) {
                logger.warn("Cannot get buffer size");
            }
        try {
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
                System.out.println("buffer has: " + connection.buffer.remaining() + " remaining bytes");
            channel.write(connection.buffer);
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
                System.out.println("buffer has: " + connection.buffer.remaining() + " remaining bytes");

            if (connection.buffer.hasRemaining()) {
                channel.register(selector, SelectionKey.OP_WRITE, connection);
            } else {
                connection.buffer.clear();
                channel.register(selector, SelectionKey.OP_READ, connection);
            }

        } catch (IOException e) {
            logger.warn("Connection closed by client");
        }
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
    public void writeInChannel(SocketChannel channel) {
        SelectionKey channelKey = channel.keyFor(selector);
        ProxyConnection connection = (ProxyConnection) channelKey.attachment();
        try {
            channel.register(selector, SelectionKey.OP_WRITE, connection);
        } catch (ClosedChannelException e) {
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
                System.out.println("Error registering the key in write mode");
        }
    }
}