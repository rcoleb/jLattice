package com.fhs.jlattice;

import java.io.IOException;

public class RunServer {
    public static void main(String args[]) throws IOException {
        LatticeServer serv= new LatticeServer();
        // set exceptionhandler:
//        serv.setExceptionHandler(new ExceptionHandler.DefaultExceptionHandlerImpl());
        // set messagehandler:
//        serv.setMessageHandler(new MessageHandler.HelloWorldMessageHandler());
        serv.init();
        serv.run();
    }
}
