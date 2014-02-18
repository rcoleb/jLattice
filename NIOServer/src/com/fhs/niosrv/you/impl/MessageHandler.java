package com.fhs.niosrv.you.impl;

import com.fhs.niosrv.Message;

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
