package pdc.proxy;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import nio.TCPProtocol;


public class HttpServerSelector {
    private static final int BUFSIZE = 256;
    private static final int TIMEOUT = 3000;
    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException {
    	System.out.println("Initializating proxy server");

        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            
        TCPProtocol protocol = new HttpClientSelectorProtocol(BUFSIZE, selector);

        while (true) {

        	if (selector.select(TIMEOUT) == 0) {
                //System.out.print(".");
                continue;
            }

        	Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
        	
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();

                if (key.isAcceptable()) {
                    protocol.handleAccept(key);
                }

                if (key.isReadable()) {
                    protocol.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    protocol.handleWrite(key);
                }
                keyIter.remove();
            }
        }
    }
}