package com.fhs.niosrv.impl.motd;

import java.io.IOException;

import com.fhs.niosrv.NIOServer;
import com.fhs.niosrv.rsc.InitializationException;

public class RunServer {
    public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, InitializationException {
        NIOServer serv= new NIOServer();
        serv.setMessageHandler(new MOTDMessageHandler());
        serv.getResources().defineResource("motd", MOTDResource.class, true);
        serv.init();
        serv.run();
    }
}
