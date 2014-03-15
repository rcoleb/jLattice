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
    
    private static final int POLL_WAIT = 3000;
    /**
     * Containing NIOServer
     */
    private LatticeServer myServer;
    
    private MessageHandler handler;
    
    private volatile boolean killFlag = false;
    private volatile boolean isDead = false;
    private final int ID;
    private final String name;
    
    Logger logger = LogManager.getLogger();
    /**
     * 
     * @param server
     * @param id 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public HandlerRunnable(LatticeServer server, int id) throws InstantiationException, IllegalAccessException {
    	this.ID = id;
    	this.myServer = server; 
    	this.handler = server.getMessageHandlerClass().newInstance();
    	this.name = this.handler.getClass().getName();
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
				// there's no point taking up cycles polling if we can wait for a signal
				msg = this.myServer.getMessageQueue().take();
			} catch (InterruptedException exc) {
				// nothing
			}
			// Gotta cover our bases:
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
	        
			// TODO pre-processing?
			Response<?> retObj = this.handler.messageRecieved(msg);
	        // TODO post-processing?
	        
	        this.logger.info("Message processed by <" + this.name + "_"+this.ID+ "> for client " + ((KeyAttachment)msg.key.attachment()).remoteAddr);
	        ((KeyAttachment)msg.key.attachment()).response = retObj;
	        // re-register key for a WRITE operation and attach return object
        	SelectionKey oldKey = msg.key;
	        try {
	            SelectionKey retKey = oldKey.channel().register(this.myServer.getSelector(), SelectionKey.OP_WRITE, oldKey.attachment());
	            // update message to reflect new key
	            msg.key = retKey;
	            msg.key.attach(oldKey.attachment());
	            // let it know there's something to do!
	            this.myServer.getSelector().wakeup();
	        } catch (ClosedChannelException e) {
	            this.myServer.getExceptionHandler().handleClosedChannelException(this.myServer, oldKey, e);
	        }
        }
        // aaaaaand we're done, forever
        this.isDead = true;
	}

}
