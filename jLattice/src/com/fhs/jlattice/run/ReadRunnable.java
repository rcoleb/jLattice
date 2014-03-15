package com.fhs.jlattice.run;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.Message;
import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.run.KeyAttachment;

/**
* Runnable sub-class; Read queue consumer<br />
* <br />
* Polls read queue and, if a connection is queued, reads the bytes from the connection and passes the message on to the assigned MessageHandler.
* 
* @author Ben.Cole
* 
*/
public final class ReadRunnable implements Runnable {
    /**
     * Containing NIOServer
     */
    private LatticeServer myServer;
    
    Logger logger = LogManager.getLogger();

    private static final int POLL_WAIT = 500;
    
    /**
     * 
     * @param server
     */
    public ReadRunnable(LatticeServer server) { this.myServer = server; }

    @Override
    public void run() {
    	ArrayList<SelectionKey> batch = new ArrayList<>();
        while(this.myServer.isRunning()) {
        	batch = new ArrayList<>(this.myServer.getReadQueue().size());
        	int cnt = this.myServer.getReadQueue().drainTo(batch);
        	if (cnt == 0) {
            	try {
            		synchronized(Thread.currentThread()) {
            			Thread.currentThread().wait(POLL_WAIT);
            		}
				} catch (InterruptedException exc) {
					// do nothing
				}
        		continue;
        	}
        	for (SelectionKey key : batch) {
        		// TODO MOOOOORRRREEE LOGGGINNNNGGGGGGGGGGG
	            @SuppressWarnings("resource")
				SocketChannel sc = (SocketChannel) key.channel();
	            int readBytes = -2;
	            ArrayList<byte[]> buffers = new ArrayList<>();
	            do {
	                ByteBuffer buf = ByteBuffer.allocate(this.myServer.getReadBufferSize());
	                try {
	                    readBytes = sc.read(buf);
	                    if (readBytes <= 0) {
	                    	break;
	                    }
						// if we've read fewer bytes than allocated, create a smaller buffer and set as the original buffer
						// also, we've reached the end of the stream at this point
						if (readBytes < this.myServer.getReadBufferSize()) {
						    ByteBuffer buf2 = ByteBuffer.allocate(readBytes);
						    buf2.put((ByteBuffer) buf.flip());
						    buf = buf2;
						}
						// add backing array to list of read byte[] arrays
						buffers.add((byte[]) buf.flip().array());
	                } catch (IOException e) {
	                    this.myServer.getExceptionHandler().handleReadException(this.myServer, key, e);
	                }
	            } while(readBytes == this.myServer.getReadBufferSize()); // while we're reading in enough to fill the buffer - if not, end of stream reached
	            
	            // create a final buffer of the exact message size, put all message bytes in the buffer, and pass on
	            ByteBuffer finalBuffer = ByteBuffer.allocate((this.myServer.getReadBufferSize() * (buffers.size() - 1)) + readBytes);
	            for (byte[] buff : buffers) {
	                finalBuffer.put(buff);
	            }
	            finalBuffer.rewind();
	            
	            // Process message and return
	            Message msg = new Message();
	            msg.server = this.myServer;
	            msg.key = key;
	            msg.message = finalBuffer;
	            this.logger.info("Incoming message object reified for client " + ((KeyAttachment)key.attachment()).remoteAddr);
	            
	            this.myServer.getMessageQueue().offer(msg);
	            
	            // TODO nullify/release resources
        	}
        }
    }
}
