package com.fhs.niosrv.impl.hdlr;

import java.nio.ByteBuffer;

import com.fhs.niosrv.Message;
import com.fhs.niosrv.impl.resp.ByteBufferResponse;
import com.fhs.niosrv.you.impl.MessageHandler;
import com.fhs.niosrv.you.impl.Response;

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
