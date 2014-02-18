package com.fhs.jlattice.you.impl;

/**
 * @author Ben.Cole
 *
 * @param <T> Message Type
 */
public abstract class Response<T> {
    
    /**
     * Actual message Object
     */
    protected T resp;
    /**
     * Default Constructor
     */
    public Response() {}
    /**
     * Construct a message
     * @param rsp
     */
    public Response(T rsp) {
        this();
        this.resp = rsp;
    }

    /** 
     * Set the content of this Message
     * @param rsp
     * @return this Message
     */
    public Response<T> setResponse(T rsp) {
        this.resp = rsp;
        return this;
    }
    
    /**
     * @return the byte[] form of this Message object's internal message
     */
    public abstract byte[] getEncodedResponse();
    
}
