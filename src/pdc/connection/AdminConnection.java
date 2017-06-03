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
    // FIXME : Este serverChannel que es? No se usa para nada y sin origin server no tiene sentido
    private SocketChannel serverChannel;
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

    @Override
    public void endConnection() throws IOException {
        // TODO - Do this function
    }

    @Override
    public void setClientChannel(SocketChannel channel) {
        this.clientChannel = channel;
    }

    @Override
    public void setServerChannel(SocketChannel channel) {
        this.serverChannel = channel;
    }

    @Override
    public SocketChannel getClientChannel() {
        return this.clientChannel;
    }

    @Override
    public SocketChannel getServerChannel() {
        return this.serverChannel;
    }
}
