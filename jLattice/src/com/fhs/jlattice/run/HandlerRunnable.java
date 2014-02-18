package com.fhs.jlattice.run;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.Message;
import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.you.impl.MessageHandler;
import com.fhs.jlattice.you.impl.Response;

/**
 * @author Ben
 *
 */
public class HandlerRunnable implements Runnable {
    /**
     * Containing NIOServer
     */
    private LatticeServer myServer;
    
    Logger logger = LogManager.getLogger();
    
    private static final int POLL_WAIT = 3000;
    private static final Object MSG_LOCK = new Object();
    /**
     * 
     * @param server
     */
    public HandlerRunnable(LatticeServer server) { this.myServer = server; }

	@Override
	public void run() {
        while(this.myServer.isRunning()) {
        	Message msg = null;
			try {
				// lock, so that we'll never peek, get scooped, and end up waiting forever for take()  
				synchronized(MSG_LOCK) {
					msg = this.myServer.getMessageQueue().peek();
					if (msg == null) {
		            	try {
		            		synchronized(Thread.currentThread()) {
		            			Thread.currentThread().wait(POLL_WAIT);
		            		}
						} catch (InterruptedException exc) {
							// do nothing
						}
						continue;
					}
					msg = this.myServer.getMessageQueue().take();
				}
			} catch (InterruptedException exc) {
				// nothing
			}
			if (msg == null) {
            	try {
            		synchronized(Thread.currentThread()) {
            			Thread.currentThread().wait(POLL_WAIT);
            		}
				} catch (InterruptedException exc) {
					// do nothing
				}
				continue;
			}
			// TODO replace getting from server with local instance - this runnable will itself be pooled, not the individual handler instances
			MessageHandler handler = this.myServer.getMessageHandler();
	        Response<?> retObj = handler.messageRecieved(msg);
	        // TODO post-processing? [for that matter, what about pre-processing?]
	        // TODO release handler instance/return to server
	        this.logger.info("Message object processed by MessageHandler; response received for client " + ((KeyAttachment)msg.key.attachment()).remoteAddr);
	        ((KeyAttachment)msg.key.attachment()).response = retObj;
	        // re-register key for a WRITE operation and attach return object
        	SelectionKey oldKey = msg.key;
	        try {
	            SelectionKey retKey = oldKey.channel().register(this.myServer.getSelector(), SelectionKey.OP_WRITE, oldKey.attachment());
	            // update message to reflect new key
	            msg.key = retKey;
	            msg.key.attach(oldKey.attachment());
	            this.myServer.getSelector().wakeup();
	        } catch (ClosedChannelException e) {
	            this.myServer.getExceptionHandler().handleClosedChannelException(this.myServer, oldKey, e);
	        }
        }
	}

}
