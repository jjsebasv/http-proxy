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

    //not delete this variables because we need them for logs
    private String host;
    private int port;

	public HttpClientSelectorProtocol(String host, int port, Selector selector) throws IOException {
        proxyConfiguration = ProxyConfiguration.getInstance();
		this.bufferSize = Integer.parseInt(proxyConfiguration.getProperty("buffer_size"));
        this.port = port;
        this.host = host;
        InetSocketAddress listenAddress = new InetSocketAddress(host, port);
        channel = ServerSocketChannel.open();
        channel.socket().bind(listenAddress);
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        logger = HttpProxyLogger.getInstance();
        logger.info("Client proxy started");
	}
	
	public ServerSocketChannel getChannel() {
		return channel;
	}

	public void setChannel(ServerSocketChannel channel) {
		this.channel = channel;
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
        Socket socket = newChannel.socket();

        try {
            newChannel.configureBlocking(false);
        } catch (IOException e) {
            this.logger.error("Cannot configure the channel as non blocking");
        }

        connection.setClientChannel(newChannel);
        connection.setClientKey(key);

        SocketAddress remoteAddress = socket.getRemoteSocketAddress();
        SocketAddress localAddress = socket.getLocalSocketAddress();
        if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
            System.out.println("Accepted new client connection from " + localAddress + " to " + remoteAddress);
        }
        this.logger.info("Accepted new client connection from " + localAddress + " to " + remoteAddress);

        try {
            SelectionKey clientKey = newChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, connection);
            clientKey.attach(connection);
        } catch (ClosedChannelException e) {
            this.logger.error("Closed channel " + newChannel.toString());
        }
    }

	/**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
	public void handleRead(SelectionKey key) throws IOException {
	    ProxyConnection connection = (ProxyConnection) key.attachment();
		SocketChannel keyChannel = (SocketChannel) key.channel();

        int bytesRead = -1;
        connection.buffer = ByteBuffer.allocate(bufferSize);
        bytesRead = keyChannel.read(connection.buffer);

        if (bytesRead == -1) {
            logger.debug("Finish reading from " + connection.getServerUrl());
            keyChannel.close();
            key.cancel();
        } else if( bytesRead > 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            // TODO -- Add Metrics of bytes read here

            // TODO -- Delete this block
            byte[] data = new byte[bytesRead];
            System.arraycopy(connection.buffer.array(), 0, data, 0, bytesRead);
            String stringRead = new String(data, "UTF-8");
            System.out.println("Vamo a leer");
            System.out.print(stringRead);
            // Until here

            connection.buffer = ByteBuffer.wrap(stringRead.getBytes());

            if (channelIsServerSide(keyChannel, connection)) {
                connection.getHttpMessage().readResponse(connection.buffer);
            } else {
                connection.getHttpMessage().readRequest(connection.buffer);
            }

        }
	}

    public void handleWrite(SelectionKey key) throws SocketException, UnsupportedEncodingException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        connection.buffer.rewind();
        //connection.buffer.flip();

        System.out.println("Vamo a escribir");
        String v = new String(connection.buffer.array(), "UTF-8");
        System.out.print(v);
        // Until here

        handleSendMessage(key);
        // dudoso
        key.interestOps(connection.buffer.position() > 0 ? SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ);
    }

    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        try {
            if (connection.getServerChannel() == null) {
                getRemoteServerUrl(connection);
                InetSocketAddress hostAddress = new InetSocketAddress(connection.getServerUrl(), connection.getServerPort());
                SocketChannel serverChannel = SocketChannel.open(hostAddress);
                logger.info("Connecting proxy to: " + connection.getHttpMessage().getHaders().get("Host"));
                connection.setServerChannel(serverChannel);
            }

            (connection.getServerChannel()).configureBlocking(false);
            writeInChannel(key, connection.getServerChannel());

            SelectionKey serverKey = (connection.getServerChannel()).register(connection.getSelector(), SelectionKey.OP_READ);
            serverKey.attach(connection);
        } catch (UnresolvedAddressException e) {
            logger.error("Unresolved host: " + connection.getServerUrl());
        } catch (Exception e) {
            logger.error("Error when writing on channel");
        }
    }

    /**
     * Send data to HTTP Client
     * @param key
     */
    private void sendToClient(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        if (Boolean.valueOf(proxyConfiguration.getProperty("verbose"))) {
            System.out.println("proxy is writing to client the string: " + connection.getHttpMessage().getMessage());
        }
        writeInChannel(key, connection.getClientChannel());
    }

    /**
     *
     * Decides whether the message should be sent to the server or to the client.
     *
     * @param key
     *
     */
    private void handleSendMessage(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (channelIsServerSide(channel, connection)) {
            sendToClient(key);
        } else {
            sendToServer(key);
        }
        connection.setHttpMessage(new HttpMessage());
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
    public void writeInChannel(SelectionKey key, SocketChannel channel) {
    	ProxyConnection connection = (ProxyConnection) key.attachment();

    	try {
    	    // TODO -- Add metrics of transfered bytes
            //connection.buffer.flip();
            int bytesWritten = channel.write(connection.buffer);
            //System.out.println("Bytes written " + bytesWritten);
		} catch (IOException e) {
            logger.error("Error when writing on channel " + connection.getServerUrl());
		}
		connection.buffer.clear();
    }


	public void getRemoteServerUrl(ProxyConnection connection) {
        HttpMessage r = connection.getHttpMessage();
    	connection.setServerUrl(r.getHaders().get("Host"));
    	connection.setServerPort(80);
	}
}