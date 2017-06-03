package pdc.connection;

import pdc.config.ProxyConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by sebastian on 5/25/17.
 */
public interface Connection {

    ProxyConfiguration proxyConfiguration = ProxyConfiguration.getInstance();

    public void setClientChannel(SocketChannel channel);

    public SocketChannel getClientChannel();

    public void endConnection() throws IOException;
}
