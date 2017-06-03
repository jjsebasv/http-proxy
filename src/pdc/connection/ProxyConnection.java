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
	public ByteBuffer buffer;
	public HttpMessage httpMessage;

	public ProxyConnection(Selector selector) {
        this.buffer = ByteBuffer.wrap(new byte[Integer.valueOf(proxyConfiguration.getProperty("buffer_size"))]);
        this.httpMessage = new HttpMessage();
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
}
