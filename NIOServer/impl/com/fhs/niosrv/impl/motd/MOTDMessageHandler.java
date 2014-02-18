package com.fhs.niosrv.impl.motd;

import java.nio.charset.Charset;

import com.fhs.niosrv.Message;
import com.fhs.niosrv.impl.resp.StringResponse;
import com.fhs.niosrv.rsc.DestructionException;
import com.fhs.niosrv.rsc.InitializationException;
import com.fhs.niosrv.rsc.UndefinedResourceException;
import com.fhs.niosrv.you.impl.MessageHandler;
import com.fhs.niosrv.you.impl.Resource;
import com.fhs.niosrv.you.impl.Response;

/**
 * Message of the Day server!
 * <br />
 * Responds to any request at /motd.txt with a pithy saying
 * 
 * @author Ben.Cole
 *
 */
public class MOTDMessageHandler implements MessageHandler {
    
    @Override
    public Response<String> messageRecieved(Message msg) {
        String v = new String(msg.message.array(), Charset.forName("UTF-8"));
        Response<String> retMsg = new StringResponse();
        retMsg.setResponse("HTTP/1.1 404 NOT FOUND");
        if (v.startsWith("GET /motd.txt")) {
            String ret = null;
            Resource motdres;
            try {
                motdres = msg.server.getResources().requestResource("motd", false);
                if (motdres instanceof MOTDResource) {
                    ret = ((MOTDResource) motdres).getRandomMOTD(false);
                }
                // Very important to return this resource! Otherwise the server doesn't know we've finished using it...
                msg.server.getResources().returnResource(motdres, false, false);
            } catch (InstantiationException e) {
                msg.server.getExceptionHandler().handleResourceException(msg.server, e);
            } catch (DestructionException e) {
                msg.server.getExceptionHandler().handleResourceException(msg.server, e);
            } catch (IllegalAccessException e) {
                msg.server.getExceptionHandler().handleResourceException(msg.server, e);
            } catch (InitializationException e) {
                msg.server.getExceptionHandler().handleResourceException(msg.server, e);
            } catch (UndefinedResourceException e) {
                msg.server.getExceptionHandler().handleResourceException(msg.server, e);
            }
            retMsg.setResponse(ret);
        }
        return retMsg;
    }
    
}
