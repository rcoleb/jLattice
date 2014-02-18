package com.fhs.niosrv;

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
    public NIOServer server;
    /**
     * 
     */
    public SelectionKey key;
    /**
     * 
     */
    public ByteBuffer message;
    
}
