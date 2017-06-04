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
            System.out.println("Address already in use");
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
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
                System.out.println("Accepted new client connection from " + localAddress + " to " + remoteAddress);
            }
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

        try {
            long bytesRead = keyChannel.read(connection.buffer);

            String side = channelIsServerSide(keyChannel, connection)? "server" : "client";
            System.out.println("Bytes read " + bytesRead + " from " + side);

            if (bytesRead == -1) { // Did the other end close?
                keyChannel.close();
            } else if (bytesRead > 0) {
               if (channelIsServerSide(keyChannel, connection)) {
                   connection.getHttpMessage().readResponse(connection.buffer);
                   sendToClient(key);
               } else {
                   connection.getHttpMessage().readRequest(connection.buffer);
                   sendToServer(key);
               }
               logger.debug("Finish reading from " + connection.getHttpMessage().getUrl());
            }
        }
        catch (IOException e) {
            logger.error("Cannot read from socket channel");
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        // Prepare buffer for writing
        //connection.buffer.flip();
        SocketChannel channel = (SocketChannel) key.channel();

        // DELETE THIS
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(connection.buffer);
        String side = channelIsServerSide(channel, connection)? "server" : "client";
        System.out.println("Sending this to " + side);
        System.out.println(charBuffer.toString());
        connection.buffer.flip();
        connection.buffer.rewind();
        // UNTIL HERE

        long bytesWritten = channel.write(connection.buffer);
        System.out.println("Bytes written " + bytesWritten + " to " + side);

        if (!connection.buffer.hasRemaining()) { // Buffer completely written?
            // Nothing left, so no longer interested in writes
            key.interestOps(OP_READ);
        } else if (key.interestOps() != OP_READ) {
            // We now can read again
            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose")))
                System.out.println("buffer has: " + connection.buffer.remaining() + " remaining bytes");
            key.interestOps(OP_READ | OP_WRITE);
        }
        connection.buffer.compact(); // Make room for more data to be read in
    }

    private void sendToClient(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        writeInChannel(connection.getClientKey());
    }


    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();

        try {
            if (connection.getServerChannel() == null) {
                InetSocketAddress hostAddress = new InetSocketAddress(connection.getHttpMessage().getUrl().getHost(), 80);
                SocketChannel serverChannel = SocketChannel.open(hostAddress);
                logger.info("Connecting proxy to: " + connection.getHttpMessage().getUrl() + " - CLIENT CHANNEL " + connection.getClientChannel().hashCode());
                serverChannel.configureBlocking(false);
                SelectionKey serverKey = serverChannel.register(key.selector(), OP_READ, connection);
                connection.setServerKey(serverKey);
                connection.setServerChannel(serverChannel);
            }

            if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
                System.out.println("Proxy is writing to: " + connection.getHttpMessage().getUrl());
            }
            writeInChannel(connection.getServerKey());
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
    public void writeInChannel(SelectionKey key) {
        key.interestOps(OP_WRITE);
    }
}