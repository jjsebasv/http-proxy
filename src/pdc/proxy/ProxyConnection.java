package pdc.proxy;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ProxyConnection {
	
	private SocketChannel clientChannel;
	private SocketChannel serverChannel;
	private String serverUrl;
	private int serverPort;
		
	public ProxyConnection(SocketChannel clientChannel) {
		this.clientChannel = clientChannel; 
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
