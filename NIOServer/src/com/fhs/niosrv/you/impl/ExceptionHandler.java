package com.fhs.niosrv.you.impl;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.niosrv.NIOServer;
import com.fhs.niosrv.rsc.DestructionException;
import com.fhs.niosrv.rsc.InitializationException;
import com.fhs.niosrv.rsc.UndefinedResourceException;

/**
 * TODO Change exception handling;
 * 
 * Possible Ideas:
 * 		= register exception handlers for specific (or not) exception class trees
 * 			- class tree cannot account for exception groupings
 * 
 * 
 * @author Ben.Cole
 *
 */
public interface ExceptionHandler {

    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown from <code>init()</code>
     */
	public void handleInitException(NIOServer srv, IOException e);
	
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown from <code>selectNow()</code>
     */
    public void handleSelectException(NIOServer srv, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>accept()</code> or <code>configureBlocking()</code>
     */
    public void handleAcceptConfigException(NIOServer srv, SelectionKey key, IOException e);

    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from methods used to identify the connection (e.g. <code>getRemoteAddress()</code>)
     */
    public void handleIdentifyException(NIOServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>key.channel().read()</code>
     */
    public void handleReadException(NIOServer srv, SelectionKey key, IOException e);

    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>key.channel().write()</code>
     */
    public void handleWriteException(NIOServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown when attempting to register with the internal <code>Selector</code>
     */
    public void handleClosedChannelException(NIOServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown when attempting to close a connection
     */
    public void handleCloseException(NIOServer srv, IOException e);
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown when attempting to shutdown the server
     */
    public void handleShutdownException(NIOServer srv, IOException e);
    /**
     * @param srv 
     * @param e
     */
    public void handleResourceException(NIOServer srv, DestructionException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(NIOServer srv, InstantiationException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(NIOServer srv, IllegalAccessException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(NIOServer srv, InitializationException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(NIOServer srv, UndefinedResourceException e);
    
    /**
     * Prints stack traces to standard output
     * <br />
     * @author Ben.Cole
     */
    public class DefaultExceptionHandlerImpl implements ExceptionHandler {
    	private Logger logger = LogManager.getLogger(getClass());
    	@Override public void handleInitException(NIOServer srv, IOException e) { this.logger.error("InitException", e); }
        @Override public void handleSelectException(NIOServer srv, IOException e) { this.logger.error("SelectException", e); }
        @Override public void handleAcceptConfigException(NIOServer srv, SelectionKey key, IOException e) { this.logger.error("AcceptConfigException", e); }
        @Override public void handleIdentifyException(NIOServer srv, SelectionKey key, IOException e) { this.logger.error("IdentifyException", e); }
        @Override public void handleReadException(NIOServer srv, SelectionKey key, IOException e)  { this.logger.error("ReadException", e); }
        @Override public void handleWriteException(NIOServer srv, SelectionKey key, IOException e)  { this.logger.error("WriteException", e); }
        @Override public void handleClosedChannelException(NIOServer srv, SelectionKey key, IOException e)  { this.logger.error("ClosedChannelException", e); }
        @Override public void handleCloseException(NIOServer srv, IOException e)  { this.logger.error("CloseException", e); }
        @Override public void handleShutdownException(NIOServer srv, IOException e)  { this.logger.error("ShutdownException", e); }
        @Override public void handleResourceException(NIOServer srv, DestructionException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(NIOServer srv, InstantiationException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(NIOServer srv, IllegalAccessException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(NIOServer srv, InitializationException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(NIOServer srv, UndefinedResourceException e) { this.logger.error("ResourceException", e); }
    }

    
}

