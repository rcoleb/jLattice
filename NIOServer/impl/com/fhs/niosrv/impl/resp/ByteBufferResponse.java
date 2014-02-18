package com.fhs.niosrv.impl.resp;

import java.nio.ByteBuffer;

import com.fhs.jlattice.you.impl.Response;

/**
 * @author Ben.Cole
 *
 */
public class ByteBufferResponse extends Response<ByteBuffer>  {
    @Override
    public byte[] getEncodedResponse() {
        return this.resp.array();
    }
}
