package com.fhs.niosrv.impl.hdlr;

import com.fhs.niosrv.Message;
import com.fhs.niosrv.impl.resp.StringResponse;
import com.fhs.niosrv.you.impl.MessageHandler;
import com.fhs.niosrv.you.impl.Response;

/**
 * @author Ben.Cole
 *
 */
public class HelloWorldMessageHandler implements MessageHandler {
    @Override
    public Response<String> messageRecieved(Message msg) {
        Response<String> retMsg = new StringResponse();
        retMsg.setResponse("Hello World!");
        return retMsg;
    }
}
