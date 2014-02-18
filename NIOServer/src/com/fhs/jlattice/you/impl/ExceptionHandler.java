package com.fhs.jlattice.you.impl;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.rsc.DestructionException;
import com.fhs.jlattice.rsc.InitializationException;
import com.fhs.jlattice.rsc.UndefinedResourceException;

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
	public void handleInitException(LatticeServer srv, IOException e);
	
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown from <code>selectNow()</code>
     */
    public void handleSelectException(LatticeServer srv, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>accept()</code> or <code>configureBlocking()</code>
     */
    public void handleAcceptConfigException(LatticeServer srv, SelectionKey key, IOException e);

    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from methods used to identify the connection (e.g. <code>getRemoteAddress()</code>)
     */
    public void handleIdentifyException(LatticeServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>key.channel().read()</code>
     */
    public void handleReadException(LatticeServer srv, SelectionKey key, IOException e);

    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown from <code>key.channel().write()</code>
     */
    public void handleWriteException(LatticeServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param key <code>SelectionKey</code>
     * @param e <code>IOException</code> thrown when attempting to register with the internal <code>Selector</code>
     */
    public void handleClosedChannelException(LatticeServer srv, SelectionKey key, IOException e);
    
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown when attempting to close a connection
     */
    public void handleCloseException(LatticeServer srv, IOException e);
    /**
     * @param srv <code>Server</code>
     * @param e <code>IOException</code> thrown when attempting to shutdown the server
     */
    public void handleShutdownException(LatticeServer srv, IOException e);
    /**
     * @param srv 
     * @param e
     */
    public void handleResourceException(LatticeServer srv, DestructionException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(LatticeServer srv, InstantiationException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(LatticeServer srv, IllegalAccessException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(LatticeServer srv, InitializationException e);
    /**
     * @param srv
     * @param e
     */
    public void handleResourceException(LatticeServer srv, UndefinedResourceException e);
    
    /**
     * Prints stack traces to standard output
     * <br />
     * @author Ben.Cole
     */
    public class DefaultExceptionHandlerImpl implements ExceptionHandler {
    	private Logger logger = LogManager.getLogger(getClass());
    	@Override public void handleInitException(LatticeServer srv, IOException e) { this.logger.error("InitException", e); }
        @Override public void handleSelectException(LatticeServer srv, IOException e) { this.logger.error("SelectException", e); }
        @Override public void handleAcceptConfigException(LatticeServer srv, SelectionKey key, IOException e) { this.logger.error("AcceptConfigException", e); }
        @Override public void handleIdentifyException(LatticeServer srv, SelectionKey key, IOException e) { this.logger.error("IdentifyException", e); }
        @Override public void handleReadException(LatticeServer srv, SelectionKey key, IOException e)  { this.logger.error("ReadException", e); }
        @Override public void handleWriteException(LatticeServer srv, SelectionKey key, IOException e)  { this.logger.error("WriteException", e); }
        @Override public void handleClosedChannelException(LatticeServer srv, SelectionKey key, IOException e)  { this.logger.error("ClosedChannelException", e); }
        @Override public void handleCloseException(LatticeServer srv, IOException e)  { this.logger.error("CloseException", e); }
        @Override public void handleShutdownException(LatticeServer srv, IOException e)  { this.logger.error("ShutdownException", e); }
        @Override public void handleResourceException(LatticeServer srv, DestructionException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(LatticeServer srv, InstantiationException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(LatticeServer srv, IllegalAccessException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(LatticeServer srv, InitializationException e) { this.logger.error("ResourceException", e); }
        @Override public void handleResourceException(LatticeServer srv, UndefinedResourceException e) { this.logger.error("ResourceException", e); }
    }

    
}

