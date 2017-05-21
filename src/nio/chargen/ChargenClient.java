package nio.chargen;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.IOException;

/**
 * Created by sebastian on 5/21/17.
 */
public class ChargenClient {
    private static int DEFAULT_PORT = 19;
    private static String DEFAULT_HOST = "localhost";
    private static int BUFFER_SIZE = 74;

    public static void main(String[] args) {

        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;

        if (args.length > 0) {
            host = args[0];
            if ( args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (RuntimeException ex) {
                    port = DEFAULT_PORT;
                }
            }
        }


        try {
            SocketAddress address = new InetSocketAddress(host, port);
            SocketChannel client = SocketChannel.open(address);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            WritableByteChannel out = Channels.newChannel(System.out);

            while (client.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
