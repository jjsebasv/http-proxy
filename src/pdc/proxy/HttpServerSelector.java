package pdc.proxy;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import nio.TCPProtocol;
import pdc.admin.AdminHandler;
import pdc.config.ProxyConfiguration;
import pdc.logger.HttpProxyLogger;


public class HttpServerSelector {

    public static void main(String[] args) throws IOException {

        ProxyConfiguration proxyConfiguration = ProxyConfiguration.getInstance();
        boolean verbose = Boolean.valueOf(proxyConfiguration.getProperty("verbose"));
        String proxyHost = String.valueOf(proxyConfiguration.getProperty("proxy_host"));
        int proxyPort = Integer.parseInt(proxyConfiguration.getProperty("proxy_port"));

        String adminHost = String.valueOf(proxyConfiguration.getProperty("proxy_host"));
        int adminPort = Integer.parseInt((proxyConfiguration.getProperty("admin_port")));

        if(verbose) {
            System.out.println("Initializing proxy server");
        }
        HttpProxyLogger.getInstance().info("Initializing proxy server");

        Selector selector = Selector.open();

        TCPProtocol HttpClientSelectorProtocol = new HttpClientSelectorProtocol(proxyHost, proxyPort, selector);
        AdminHandler adminHandler = new AdminHandler(adminHost, adminPort, selector);

        while (true) {

        	if (selector.select(Integer.valueOf(proxyConfiguration.getProperty("selector_timeout"))) == 0) {
                continue;
            }

        	Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();

                if (key.isAcceptable()) {
                    if (adminHandler.getAdminServerChannel().equals(key.channel())) {
                        adminHandler.handleAccept(key);
                    } else {
                        HttpClientSelectorProtocol.handleAccept(key);
                    }
                }

                if (key.isReadable()) {
                    if (adminHandler.getAdminChannel().equals(key.channel())) {
                        adminHandler.handleRead(key);
                    } else {
                        HttpClientSelectorProtocol.handleRead(key);
                    }
                }

                if (key.isValid() && key.isWritable()) {
                	HttpClientSelectorProtocol.handleWrite(key);
                }
                keyIter.remove();
            }
        }
    }
}