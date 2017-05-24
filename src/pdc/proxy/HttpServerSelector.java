package pdc.proxy;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import nio.TCPProtocol;


public class HttpServerSelector {

    public static void main(String[] args) throws IOException {
    	System.out.println("Initializing proxy server");

        Selector selector = Selector.open();

        TCPProtocol HttpClientSelectorProtocol = new HttpClientSelectorProtocol(Config.getProxyHost(), Config.getProxyPort(), selector, Config.getBufferSize());

        while (true) {

        	if (selector.select(Config.getTIMEOUT()) == 0) {
                if (HttpServerSelector.isVerbose()) {
                	//System.out.println(".");
                }
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

	public static boolean isVerbose() {
		return Config.isVerbose();
	}

}