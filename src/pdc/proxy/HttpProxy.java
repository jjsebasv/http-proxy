package pdc.proxy;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import nio.TCPProtocol;
import pdc.admin.Admin;
import pdc.admin.AdminHandler;
import pdc.config.ProxyConfiguration;
import pdc.logger.HttpProxyLogger;


public class HttpProxy {

    public static void main(String[] args) throws IOException {

        ProxyConfiguration proxyConfiguration = ProxyConfiguration.getInstance();
        boolean verbose = Boolean.valueOf(proxyConfiguration.getProperty("verbose"));

        String adminHost = String.valueOf(proxyConfiguration.getProperty("proxy_host"));
        int adminPort = Integer.parseInt((proxyConfiguration.getProperty("admin_port")));

        HttpProxyLogger.getInstance().info("Initializing proxy server");

        Selector selector = Selector.open();

        TCPProtocol httpClientSelectorProtocol = new ClientHandler(selector);
        AdminHandler adminHandler = new AdminHandler(adminHost, adminPort, selector);
        Admin.generateFirstAdmin();


        while (true) {

        	if (selector.select(Integer.valueOf(proxyConfiguration.getProperty("selector_timeout"))) == 0) {
                continue;
            }

        	Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();

                if (key.isAcceptable()) {
                    if (key.channel().equals(adminHandler.getAdminServerChannel())) {
                        adminHandler.handleAccept(key);
                    } else {
                        httpClientSelectorProtocol.handleAccept(key);
                    }
                }

                if (key.isValid() && key.isReadable()) {
                    if (key.channel().equals(adminHandler.getAdminChannel())) {
                        adminHandler.handleRead(key);
                    } else {
                        httpClientSelectorProtocol.handleRead(key);
                    }
                }

                if (key.isValid() && key.isWritable()) {
                	httpClientSelectorProtocol.handleWrite(key);
                }
                keyIter.remove();
            }
        }
    }
}