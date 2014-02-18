package com.fhs.niosrv.impl.hdlr;

import java.nio.ByteBuffer;

import com.fhs.jlattice.Message;
import com.fhs.jlattice.you.impl.MessageHandler;
import com.fhs.jlattice.you.impl.Response;
import com.fhs.niosrv.impl.resp.ByteBufferResponse;

/**
 * @author Ben.Cole
 *
 */
public class EchoMessageHandler implements MessageHandler {
    @Override
    public Response<ByteBuffer> messageRecieved(Message msg) {
        Response<ByteBuffer> retMsg = new ByteBufferResponse();
        retMsg.setResponse(msg.message);
        return retMsg;
    }
}
