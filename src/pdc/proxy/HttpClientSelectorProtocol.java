package pdc.proxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import nio.TCPProtocol;

public class HttpClientSelectorProtocol implements TCPProtocol {
    private ConcurrentHashMap<SocketChannel, ProxyConnection> clientToProxyChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
    private ConcurrentHashMap<SocketChannel, ProxyConnection> proxyToClientChannelMap = new ConcurrentHashMap<SocketChannel, ProxyConnection>();
	private Selector selector;
    private int bufferSize;
    private final ByteBuffer bufferForRead;
    private InetSocketAddress listenAddress;
    private ServerSocketChannel channel;

	public HttpClientSelectorProtocol(String host, int port, Selector selector, int bufferSize) throws IOException {
    	this.selector = selector;
		this.bufferSize = bufferSize;
    	bufferForRead = ByteBuffer.allocate(bufferSize);
		listenAddress = new InetSocketAddress(host, port);
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(listenAddress);
        channel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public ServerSocketChannel getChannel() {
		return channel;
	}

	public void setChannel(ServerSocketChannel channel) {
		this.channel = channel;
	}
	
	/**
     * Accept connections to XMPP Proxy
     * @param key
     * @throws IOException
     */
    public void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
        SocketChannel newChannel = keyChannel.accept();
        newChannel.configureBlocking(false);
        Socket socket = newChannel.socket();
        if (HttpServerSelector.isVerbose()) {
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            SocketAddress localAddr = socket.getLocalSocketAddress();
            System.out.println("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        }
        newChannel.register(this.selector, SelectionKey.OP_READ);
        clientToProxyChannelMap.put(newChannel, new ProxyConnection(newChannel));
    }
	

    /**
     * Send data to HTTP Client
     * @param s
     * @param channel
     */
    private void sendToClient(String s, SocketChannel channel) {
    	if (HttpServerSelector.isVerbose()) {
			System.out.println("proxy is writing to client the string: " + s);
    	}
    	writeInChannel(s, channel);
    }
	
	/**
     * Read from the socket channel
     * @param key
     * @throws IOException
     */
	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel keyChannel = (SocketChannel) key.channel();
        bufferForRead.clear();
		int bytesRead = -1;
        bytesRead = keyChannel.read(bufferForRead);
        String side = channelIsServerSide(keyChannel) ? "server" : "client";
        
        if (bytesRead == -1) {
        	// DO SOMETHING
        	keyChannel.close();
            key.cancel();
            return;
        }

		byte[] data = new byte[bytesRead];
		System.arraycopy(bufferForRead.array(), 0, data, 0, bytesRead);
		String stringRead = new String(data, "UTF-8");
		
    	try {
    		System.out.println("==============");
            System.out.println(side + " wrote");
            System.out.println(stringRead);
            if (channelIsServerSide(keyChannel)) {
            	ProxyConnection connection = proxyToClientChannelMap.get(keyChannel);
            	sendToClient(stringRead, connection.getClientChannel());
            } else {
                ProxyConnection connection = clientToProxyChannelMap.get(keyChannel);
                sendToServer(stringRead, connection);
            }
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
     * Ask if a channel is server side
     * @param channel
     * @return
     */
    private boolean channelIsServerSide(SocketChannel channel) {
    	return proxyToClientChannelMap.get(channel) != null; 
    }

	private void sendToServer(String stringRead, ProxyConnection connection) {
		try {
	    	if (connection.getServerChannel() == null) {
	    		getRemoteServerUrl(connection, stringRead);
	    		InetSocketAddress hostAddress = new InetSocketAddress(connection.getServerUrl(), connection.getServerPort());
	            SocketChannel serverChannel = SocketChannel.open(hostAddress);
	            serverChannel.configureBlocking(false);
	            serverChannel.register(this.selector, SelectionKey.OP_READ);
	            connection.setServerChannel(serverChannel);
	            proxyToClientChannelMap.put(serverChannel, connection);
	    	}
	        writeInChannel(stringRead, connection.getServerChannel());
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
	}
	
	/**
     * Write data to a specific channel
     */
    public void writeInChannel(String s, SocketChannel channel) {
    	ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
    	SelectionKey channelKey = channel.keyFor(selector);
    	try {
			channel.register(selector, SelectionKey.OP_WRITE);
			channelKey.attach(buffer);
		} catch (ClosedChannelException e) {
			System.out.println(e.getStackTrace());
		}
    }
    
    public void handleWrite(SelectionKey key) throws SocketException, UnsupportedEncodingException {
    	ByteBuffer buffer = (ByteBuffer) key.attachment();
    	SocketChannel channel = (SocketChannel) key.channel();
    	channel.socket().setSendBufferSize(this.bufferSize);
 
    	if (HttpServerSelector.isVerbose())
    		System.out.println("socket can send " + channel.socket().getSendBufferSize() + " bytes per write operation");

    	try {
    		if (HttpServerSelector.isVerbose())
    			System.out.println("buffer has: " + buffer.remaining() + " remaining bytes");
			channel.write(buffer);
			if (HttpServerSelector.isVerbose())
				System.out.println("buffer has: " + buffer.remaining() + " remaining bytes");
			
			if (buffer.hasRemaining()) {
				channel.register(selector, SelectionKey.OP_WRITE);
				SelectionKey channelKey = channel.keyFor(selector);
				channelKey.attach(buffer);
			} else {
				channel.register(selector, SelectionKey.OP_READ);
				buffer.clear();
			}
			
		} catch (IOException e) {
			// TODO: LOG ERROR
		}
    }

	public void getRemoteServerUrl(ProxyConnection connection, String request) {
    	String uri = request.toString().split("\r\n", 2)[0].split(" ")[1];
    	String host = request.toString().split("\r\n", 2)[1].split(" ")[1];
    	String url;
    	if (uri.startsWith("/")) {
    		url = host + uri;
    		url = url.split("://")[1];
    		System.out.println(url);
    	} else {
    		url = uri.split("://")[1];
    		url = url.substring(0, url.length() - 1);
    	}
    	connection.setServerUrl(url);
    	connection.setServerPort(80);
	}
}