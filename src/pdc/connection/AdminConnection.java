package pdc.connection;

import pdc.admin.AdminResponses;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by sebastian on 5/25/17.
 */
public class AdminConnection implements Connection {

    private SocketChannel clientChannel;
    private SocketChannel serverChannel;

    private SelectionKey serverKey;
    private SelectionKey clientKey;

    private Selector selector;

    public AdminResponses serverResponse;

    public ByteBuffer buffer;
    private AdminState state;

    private ConnectionType type;

    public static enum AdminState {
        LOGGED_IN,
        NO_STATUS
    }

    public AdminConnection (Selector selector) {
        this.setState(AdminState.NO_STATUS);
        this.selector = selector;
        this.buffer = ByteBuffer.allocate(Integer.valueOf(proxyConfiguration.getProperty("buffer_size")));
        this.type = ConnectionType.ADMIN;
    }

    public AdminState getState() {
        return state;
    }

    public void setState(AdminState state) {
        this.state = state;
    }

    public void endConnection() throws IOException {
        // TODO - Do this function
    }

    public void setClientChannel(SocketChannel channel) {
        this.clientChannel = channel;
    }

    public SocketChannel getClientChannel() {
        return this.clientChannel;
    }

    public SocketChannel getServerChannel() {
        return serverChannel;
    }

    public SelectionKey getClientKey() {
        return clientKey;
    }

    public void setClientKey(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }

    public void setServerChannel(SocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }

    public SelectionKey getServerKey() {
        return serverKey;
    }

    public void setServerKey(SelectionKey serverKey) {
        this.serverKey = serverKey;
    }

    public ConnectionType getType() {return this.type;}
}
