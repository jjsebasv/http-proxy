package pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ProxyConnection {

    Config configFile = new Config();

	private SocketChannel clientChannel;
    private SelectionKey clientKey;

    public SelectionKey getClientKey() {
        return clientKey;
    }

    public void setClientKey(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }

    private SocketChannel serverChannel;
	private String serverUrl;
	private int serverPort;

    public ByteBuffer buffer;
    private Selector selector;
		
	public ProxyConnection(SocketChannel clientChannel) {
		this.clientChannel = clientChannel; 
	}

	public ProxyConnection(Selector selector) {
        this.selector = selector;
        this.buffer = ByteBuffer.wrap(new byte[configFile.getBufferSize()]);
    }

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public SocketChannel getClientChannel() {
		return clientChannel;
	}
	
	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}
	
	public SocketChannel getServerChannel() {
		return serverChannel;
	}
	
	public void setServerChannel(SocketChannel serverChannel) {
		this.serverChannel = serverChannel;
	}
}
