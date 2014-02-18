package com.fhs.niosrv.impl.hdlr;

import com.fhs.jlattice.Message;
import com.fhs.jlattice.you.impl.MessageHandler;
import com.fhs.jlattice.you.impl.Response;
import com.fhs.niosrv.impl.resp.StringResponse;

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
