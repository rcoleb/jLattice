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
    
    private static final int POLL_WAIT = 500;
    private static final Object MSG_LOCK = new Object();
    
    /**
     * Containing NIOServer
     */
    private LatticeServer myServer;
    
    private MessageHandler handler;
    private String handlerName;
    
    private volatile boolean killFlag = false;
    private volatile boolean isDead = false;
    
    Logger logger = LogManager.getLogger();
    /**
     * 
     * @param server
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public HandlerRunnable(LatticeServer server) throws InstantiationException, IllegalAccessException { 
    	this.myServer = server; 
    	this.handler = server.getMessageHandlerClass().newInstance();
    	this.handlerName = this.handler.getClass().getName();
	}
    
    /**
     * Set this HandlerRunnable's internal kill-flag, allowing graceful shutdown
     */
    public void kill() {
    	if (this.killFlag) return; 
    	this.killFlag = true;
    }

    /**
     * @return whether or not this handler is still processing it's run method
     */
    public boolean isDead() {
    	return this.isDead;
    }
    
    @Override
	public void run() {
        while(this.myServer.isRunning() && !this.killFlag) {
        	Message msg = null;
			try {
				// lock, so that we'll never peek, get scooped, and end up waiting forever for take()  
//				synchronized(MSG_LOCK) {
//					msg = this.myServer.getMessageQueue().peek();
//					if (msg == null) {
//		            	try {
//		            		synchronized(Thread.currentThread()) {
//		            			Thread.currentThread().wait(POLL_WAIT);
//		            		}
//						} catch (InterruptedException exc) {
//							// do nothing
//						}
//						continue;
//					}
					msg = this.myServer.getMessageQueue().take();
//				}
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
	        
			Response<?> retObj = this.handler.messageRecieved(msg);
	        // TODO post-processing? [for that matter, what about pre-processing?]
	        
	        this.logger.info("Message object processed by " + this.handlerName + "; response received for client " + ((KeyAttachment)msg.key.attachment()).remoteAddr);
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
        this.isDead = true;
	}

}
