package pdc.proxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import nio.TCPProtocol;
import pdc.logger.HttpProxyLogger;

public class HttpClientSelectorProtocol implements TCPProtocol {
    private int bufferSize;
    private ServerSocketChannel channel;
    private HttpProxyLogger logger;

    //not delete this variables because we need them for logs
    private String host;
    private int port;

	public HttpClientSelectorProtocol(String host, int port, Selector selector, int bufferSize) throws IOException {
		this.bufferSize = bufferSize;
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
     * @param key
     * @throws IOException
     */
    public void handleAccept(SelectionKey key) throws IOException {
        ProxyConnection connection = new ProxyConnection(key.selector());

        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = keyChannel.accept();
        Socket socket = newChannel.socket();

        newChannel.configureBlocking(false);

        connection.setClientChannel(newChannel);
        connection.setClientKey(key);

        if (HttpServerSelector.isVerbose()) {
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            SocketAddress localAddr = socket.getLocalSocketAddress();
            System.out.println("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        }

        SelectionKey clientKey = newChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, connection);
        clientKey.attach(connection);
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
            logger.debug("Error while reading from channel");
            keyChannel.close();
            key.cancel();
        } else if( bytesRead > 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            // TODO -- Add Metrics of bytes read here

            byte[] data = new byte[bytesRead];
            System.arraycopy(connection.buffer.array(), 0, data, 0, bytesRead);
            String stringRead = new String(data, "UTF-8");

            //connection.request = new Request(stringRead);
            connection.buffer = ByteBuffer.wrap(stringRead.getBytes());
            connection.getHttpMessage().appendMessage(stringRead);
        } else {
            // TODO key channel read the rest?
            logger.debug("Partial reading");
        }
	}

    public void handleWrite(SelectionKey key) throws SocketException, UnsupportedEncodingException {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel clientChannel = connection.getClientChannel();
        SocketChannel serverChannel = connection.getServerChannel();

        ByteBuffer writeBuffer = (ByteBuffer) connection.buffer;

        channel.socket().setSendBufferSize(this.bufferSize);

        if (HttpServerSelector.isVerbose())
            System.out.println("socket can send " + channel.socket().getSendBufferSize() + " bytes per write operation");

        try {
            if(!channel.equals(clientChannel) && !channel.equals(serverChannel) && channel.getRemoteAddress().equals(serverChannel.getRemoteAddress())) {
                connection.setServerChannel(serverChannel);
            }

            if (HttpServerSelector.isVerbose())
                System.out.println("buffer has: " + writeBuffer.remaining() + " remaining bytes");

            if (!writeBuffer.hasRemaining()) {
                writeBuffer.clear();
                writeBuffer.flip();
                if (!writeBuffer.hasRemaining()) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
            }

            handleSendMessage(key);
            key.interestOps(connection.buffer.position() > 0 ? SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.error("Error when writing on channel");
        }
    }

    private void sendToServer(SelectionKey key) {
        ProxyConnection connection = (ProxyConnection) key.attachment();
        try {
            if (connection.getServerChannel() == null) {
                getRemoteServerUrl(connection);
                InetSocketAddress hostAddress = new InetSocketAddress(connection.getServerUrl(), connection.getServerPort());
                SocketChannel serverChannel = SocketChannel.open(hostAddress);
                connection.setServerChannel(serverChannel);
            }

            (connection.getServerChannel()).configureBlocking(false);
            writeInChannel(key, connection.getServerChannel());

            SelectionKey serverKey = (connection.getServerChannel()).register(connection.getSelector(), SelectionKey.OP_READ);
            serverKey.attach(connection);
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
        if (HttpServerSelector.isVerbose()) {
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

        //System.out.println(connection.getHttpMessage().getMessage());
        if (connection.getHttpMessage().isMessageReady()) {
            if (channelIsServerSide(channel, connection)) {
                sendToClient(key);
            } else {
                sendToServer(key);
            }
            connection.setHttpMessage(new HttpMessage());
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
    public void writeInChannel(SelectionKey key, SocketChannel channel) {
    	ProxyConnection connection = (ProxyConnection) key.attachment();
        String stringRead = connection.getHttpMessage().getMessage().toString();

        connection.buffer.clear();
        
        connection.buffer = ByteBuffer.wrap(stringRead.getBytes());
    	try {
    	    // TODO -- Add metrics of transfered bytes
            channel.write(ByteBuffer.wrap(stringRead.getBytes()));
		} catch (IOException e) {
            logger.error("Error when writing on channel");
            System.out.println("Aca error --- " + stringRead);
		}
		connection.buffer.clear();
    }


	public void getRemoteServerUrl(ProxyConnection connection) {
        HttpMessage r = connection.getHttpMessage();
    	connection.setServerUrl(r.getUrl());
    	connection.setServerPort(80);
	}


}