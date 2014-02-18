package com.fhs.niosrv;

import java.io.IOException;

public class RunServer {
    public static void main(String args[]) throws IOException {
        NIOServer serv= new NIOServer();
        // set exceptionhandler:
//        serv.setExceptionHandler(new ExceptionHandler.DefaultExceptionHandlerImpl());
        // set messagehandler:
//        serv.setMessageHandler(new MessageHandler.HelloWorldMessageHandler());
        serv.init();
        serv.run();
    }
}
