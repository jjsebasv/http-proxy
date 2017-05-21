package pdc.proxy;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import nio.TCPProtocol;


public class HttpServerSelector {
    private static final int BUFFER_SIZE = 256;
    private static final int TIMEOUT = 3000;
    private static final int PROXY_PORT = 9090;
    private static final String PROXY_HOST = "127.0.0.1";

    public static void main(String[] args) throws IOException {
    	System.out.println("Initializating proxy server");
    	
		InetSocketAddress hostAddress = new InetSocketAddress("http://www.google.com", 80);
        SocketChannel serverChannel = SocketChannel.open(hostAddress);

        Selector selector = Selector.open();
            
        TCPProtocol HttpClientSelectorProtocol = new HttpClientSelectorProtocol(PROXY_HOST, PROXY_PORT, selector, BUFFER_SIZE);

        while (true) {

        	if (selector.select(TIMEOUT) == 0) {
                //System.out.print(".");
                continue;
            }

        	Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
        	
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();

                if (key.isAcceptable()) {
                	HttpClientSelectorProtocol.handleAccept(key);
                }

                if (key.isReadable()) {
                	HttpClientSelectorProtocol.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                	HttpClientSelectorProtocol.handleWrite(key);
                }
                keyIter.remove();
            }
        }
    }
}