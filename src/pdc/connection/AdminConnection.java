package pdc.connection;

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
    private Selector selector;

    public ByteBuffer buffer;
    private AdminState state;

    public static enum AdminState {
        LOGGED_IN,
        NO_STATUS
    }

    public AdminConnection (Selector selector) {
        this.setState(AdminState.NO_STATUS);
        this.selector = selector;
        this.buffer = ByteBuffer.allocate(Integer.valueOf(proxyConfiguration.getProperty("buffer_size")));
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
}
