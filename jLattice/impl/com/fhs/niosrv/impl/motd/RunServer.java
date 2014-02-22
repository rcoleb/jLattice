package com.fhs.niosrv.impl.motd;

import java.io.IOException;

import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.rsc.InitializationException;

public class RunServer {
    public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, InitializationException {
        LatticeServer serv= new LatticeServer();
        serv.setMessageHandlerClass(MOTDMessageHandler.class);
        serv.getResources().defineResource("motd", MOTDResource.class, true);
        serv.init();
        serv.run();
    }
}
