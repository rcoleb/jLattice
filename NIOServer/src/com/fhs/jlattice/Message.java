package com.fhs.jlattice;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Message recieved by server, passed to MessageHandler 
 * 
 * @author Ben.Cole
 */
public class Message {
    /**
     * 
     */
    public LatticeServer server;
    /**
     * 
     */
    public SelectionKey key;
    /**
     * 
     */
    public ByteBuffer message;
    
}
