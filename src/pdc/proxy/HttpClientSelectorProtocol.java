package pdc.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
    private InetSocketAddress listenAddress;
    private ServerSocketChannel channel;

	public HttpClientSelectorProtocol(String host, int port, Selector selector, int bufferSize) throws IOException {
    	this.selector = selector;
		this.bufferSize = bufferSize;
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

	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
		newChannel.configureBlocking(false);
		newChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        Socket socket = newChannel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        SocketAddress localAddr = socket.getLocalSocketAddress();
        System.out.println("Accepted new client connection from " + localAddr + " to " + remoteAddr);
        clientToProxyChannelMap.put(newChannel, new ProxyConnection(newChannel));
	}

	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel keyChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();

		int bytesRead = keyChannel.read(buffer);

		byte[] data = new byte[bytesRead];
		System.arraycopy(buffer.array(), 0, data, 0, bytesRead);
		String stringRead = new String(data, "UTF-8");
		System.out.println("Read by client");
		System.out.println(stringRead);
		
    	try {
        	ProxyConnection connection = clientToProxyChannelMap.get(keyChannel);
        	sendToServer(stringRead, connection);
		} catch (Exception e) {
			// TODO: handle exception
		}


		if (bytesRead == -1) {
			keyChannel.close();
		} else if (bytesRead > 0) {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
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
			System.out.println("Que rompimo");
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
			System.out.println("Error a escribir");
		}
    }
    

	public void handleWrite(SelectionKey key) throws IOException {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		buffer.flip();
		SocketChannel clientChannel = (SocketChannel) key.channel();
		clientChannel.write(buffer);
		if (!buffer.hasRemaining()) {
			key.interestOps(SelectionKey.OP_READ);
		}
		buffer.compact();
	}
	
	public void getRemoteServerUrl(ProxyConnection connection, String request) {
    	String uri = request.toString().split("\n", 2)[0].split(" ")[1];
    	String host = request.toString().split("\n", 2)[1].split(" ")[1];
    	String url;
    	if (uri.startsWith("/")) {
    		url = host + uri;
    	} else {
    		url = uri;
    	}
    	System.out.println("REMOTE SERVER: " + url);
    	connection.setServerUrl(url);
    	connection.setServerPort(80);
	}
}