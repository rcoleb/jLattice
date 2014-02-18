package com.fhs.jlattice.you.impl;

import com.fhs.jlattice.Message;

/**
 * @author Ben.Cole
 *
 */
public interface MessageHandler {
    /**
     * @param msg
     * @return a proper response
     */
    public Response<?> messageRecieved(Message msg);
    
}
