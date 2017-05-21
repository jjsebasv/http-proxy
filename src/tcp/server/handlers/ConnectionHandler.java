package tcp.server.handlers;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by sebastian on 5/21/17.
 */
public interface ConnectionHandler {
    void handle(Socket s) throws IOException;
}
