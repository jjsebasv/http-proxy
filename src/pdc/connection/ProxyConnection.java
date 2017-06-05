package pdc.connection;

import pdc.config.ProxyConfiguration;
import pdc.connection.Connection;
import pdc.proxy.HttpMessage;
import pdc.proxy.Request;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProxyConnection implements Connection {

	private SocketChannel clientChannel;
	private SocketChannel serverChannel;
	private SelectionKey serverKey;

    public void setClientKey(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }

    private SelectionKey clientKey;

    public SelectionKey getServerKey() {
        return serverKey;
    }

    public ByteBuffer buffer;
	public HttpMessage httpMessage;

	private ConnectionType type;

	public ProxyConnection(Selector selector) {
        this.buffer = ByteBuffer.allocate(Integer.parseInt(proxyConfiguration.getProperty("buffer_size")));
        this.httpMessage = new HttpMessage();
		this.type = ConnectionType.HTTP;
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

    public HttpMessage getHttpMessage() {
        return httpMessage;
    }

    public void endConnection() {
        // TODO - do this function
    }

	public void setServerKey(SelectionKey serverKey) {
		this.serverKey = serverKey;
	}

    public SelectionKey getClientKey() {
	    return this.clientKey;
    }

	public ConnectionType getType() {return this.type;}
}
