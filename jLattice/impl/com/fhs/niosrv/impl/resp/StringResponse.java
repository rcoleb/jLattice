package com.fhs.niosrv.impl.resp;

import com.fhs.jlattice.you.impl.Response;

/**
 * @author Ben.Cole
 *
 */
public class StringResponse extends Response<String> {
    /**
     * Default constructor
     */
    public StringResponse() { /*  */ }
    /**
     * @param rsp
     */
    public StringResponse(String rsp) {
        this.setResponse(rsp);
    }
    @Override
    public byte[] getEncodedResponse() {
        return this.resp.getBytes();
    }
}
