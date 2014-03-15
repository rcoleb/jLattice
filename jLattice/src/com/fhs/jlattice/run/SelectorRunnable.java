package com.fhs.jlattice.run;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.LatticeServer;

/**
 * @author Ben.Cole
 *
 */
public final class SelectorRunnable implements Runnable {
    /**
     * Containing NIOServer
     */
    private LatticeServer myServe;
    private Logger logger = LogManager.getLogger();
    private static final int SELECTOR_SLEEP = 3000; // 3 seconds
    /**
     * 
     * @param server
     */
    public SelectorRunnable(LatticeServer server) { this.myServe = server; }
    
    /**
	 * Warning suppressed because although we accept a new connection, we don't
	 * do anything with it in this method; this is not a resource leak (or
	 * rather, it is an intentionally-designed resource leak).
	 */
    @SuppressWarnings("resource")
	@Override
    public void run() {
    	this.logger.trace("NIO Selector started...");
        while(this.myServe.doMainLoop()) {
            int evts;
            try {
                evts = this.myServe.getSelector().select(SELECTOR_SLEEP);//selectNow();
            } catch (IOException e) {
                this.myServe.getExceptionHandler().handleSelectException(this.myServe, e);
                continue;
            }
            if (evts == 0) continue;
            Set<SelectionKey> selectedKeys = this.myServe.getSelector().selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                
                // use boolean methods instead of bitwise OR-ing with interestOps() -- cuz I'm lazy and boolean forms hurt
                // only accept new connections if the server is running - we'll process everything while 
                // the server is initialized, in case there are trailing reads/handlers/writes
                if(key.isAcceptable() && this.myServe.isRunning()) {
                    // a connection was accepted by a ServerSocketChannel
                    SocketChannel newConnection;
                    try {
                        newConnection = this.myServe.getSSC().accept();
                        newConnection.configureBlocking(false);
                        KeyAttachment att = new KeyAttachment();
                        att.remoteAddr = newConnection.getRemoteAddress().toString();
                        this.logger.info("Client connected from " + att.remoteAddr);
                        key.attach(att);
                        try {
                            // register as a connection event
                            newConnection.register(this.myServe.getSelector(), SelectionKey.OP_CONNECT);
                            // notify selector to return
                            this.myServe.getSelector().wakeup();
                            // register the channel as ready to be read
                            newConnection.register(this.myServe.getSelector(), SelectionKey.OP_READ).attach(att);
                            // notify selector to return
                            // although we technically only need to do this once, I'm doing it twice here given the 
                            // possibility of altering this method to not handle both at once;
                            this.myServe.getSelector().wakeup();
                        } catch (ClosedChannelException e) {
                            this.myServe.getExceptionHandler().handleClosedChannelException(this.myServe, key, e);
                        }
                    } catch (IOException e) {
                        this.myServe.getExceptionHandler().handleAcceptConfigException(this.myServe, key, e);
                    }
                } else if (key.isConnectable()) {
                    this.myServe.doConnectable(key);
                } else if (key.isReadable()) {
                    this.myServe.doRead(key);
                } else if (key.isWritable()) {
                    this.myServe.doWrite(key);
                }
                
            }
        }
    }
    
}
