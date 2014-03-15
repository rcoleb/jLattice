package com.fhs.jlattice.run;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.Message;
import com.fhs.jlattice.LatticeServer;

/**
 * <strong>CAUTION: THIS CLASS IS UNTESTED. USE AT OWN RISK. TEST WITH LARGE MESSAGES AND SMALL WRITEBUFFER SIZES BEFORE DEPLOYING!</strong><br />
 * 
* <code>Runnable</code> sub-class; Write queue consumer<br />
* <br />
* Polls write queue and, if a message (<code>msg</code>) is queued, performs the following:
*  <ol>
*          <li>If <code>msg</code> is <code>null</code>, close the connection.</li>
*          <li>If <code>msg</code> is a {@link Message}<code>&lt?&gt</code> object:
*          <br />&nbsp&nbsp&nbsp&nbsp--&gt retrieve <code>byte[]</code>-encoded message,</li>
*          <li>Otherwise, call <code>toString().getBytes()</code> to obtain <code>byte[]</code> </li>
*  </ol>
*  And finally transmit <code>byte[]</code> array:<br />
*  <ul>
*  		<li>If <code>byte[]</code> message is smaller than the set writebuffer size, write the entire message and close the connection.</li>
*  		<li>If <code>byte[]</code> message is larger than the set writebuffer size, then: starting at the writeMarker index set in the {@link KeyAttachment} object, write out either an entire writebuffer's worth of bytes, or write out the remainder of the message.  If some of the message remains to be written, update the writemarker and re-register to key to be written.  Otherwise, close the connection. </li>
*  </ul>
	
* <br />
* @author Ben.Cole
* 
*/
public final class AsyncWriteRunnable implements Runnable {
    /**
     * Containing NIOServer
     */
    private LatticeServer myServe;
    
    Logger logger = LogManager.getLogger();
    
    private static final int POLL_WAIT = 1000;
    
    /**
     * 
     * @param server
     */
    public AsyncWriteRunnable(LatticeServer server) { this.myServe = server; }

    @Override
    public void run() {
    	ArrayList<SelectionKey> batch = new ArrayList<>();
        while(this.myServe.isRunning()) {
        	int elemCount = this.myServe.getWriteQueue().size();
        	batch = new ArrayList<>(elemCount + 75); // add magic# 75 for good measure
        	// TODO analyze draining - possibly better to limit max drain?  
        	// does it really matter though? - we re-register anyway if the response is too large
        	// ahh, but that could be an issue, especially since we only have one write consumer:
        	// if the queue is huge, we may write the beginning of a message, and then wait a relatively
        	// long time before we write the rest of the message.
        	// SO: options are (a) limit queue; (b) multiple write consumers [tricky, have to manage socket write sync?]
        	// I'm thinking (a) for now, but I'll sleep on it...for a while
        	int count = this.myServe.getWriteQueue().drainTo(batch, elemCount); // never drain more than we expected
        	
        	if (count == 0) {
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
	            KeyAttachment attach = (KeyAttachment) key.attachment();
	            
	            boolean close = false;
	            if (attach.response != null) {
	                byte[] msgBytes = attach.response.getEncodedResponse();
	                attach.encodedResp = msgBytes;
	                if (msgBytes.length > this.myServe.getWriteBufferSize()) {
	                    close = writeAsync(key);
	                } else {
	                    write(key);
	                    close = true;
	                }
	            }
	            
	            if (close) {
		            try {
		                this.myServe.closeConnection(key);
		            } catch (IOException e) {
		                this.myServe.getExceptionHandler().handleCloseException(this.myServe, e);
		            }
	            }
        	}
        }
    }
    
    /**
     * Write a byte[] message that is smaller or equal in size to the size of the write buffer 
     * 
     * @param msgBytes
     * @param sc
     * @param key
     */
    @SuppressWarnings("resource")
	private void write(SelectionKey key) {
    	SocketChannel sc = (SocketChannel) key.channel();
        try {
            sc.configureBlocking(false);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
        ByteBuffer wb = ByteBuffer.allocate(this.myServe.getWriteBufferSize());
        wb.put(((KeyAttachment)key.attachment()).encodedResp);
        wb.flip();
        try {
            int b = sc.write(wb);
            this.logger.info("Wrote " + b + " bytes to client " + ((KeyAttachment)key.attachment()).remoteAddr);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
        sc = null;
    }
    
    @SuppressWarnings("resource")
	private boolean writeAsync(SelectionKey key) {
    	SocketChannel sc = (SocketChannel) key.channel();
        try {
            sc.configureBlocking(false);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
        int wBuff = this.myServe.getWriteBufferSize();
        ByteBuffer wb = ByteBuffer.allocate(wBuff);
        KeyAttachment attach = ((KeyAttachment)key.attachment());
        int start = attach.writeMarker;
        // do we have more than enough content remaining to fill another buffer? -- set the end marker accordingly
        int end = attach.encodedResp.length - start > wBuff ? start + wBuff : attach.encodedResp.length;
        byte[] subArr = Arrays.copyOfRange(attach.encodedResp, start, end);
    	wb.put(subArr);
    	wb.flip();
    	try {
    		int b = sc.write(wb);
    		String logMsg = "Wrote " + b + " bytes to client " + attach.remoteAddr + "; ";
    		if (end < attach.encodedResp.length) {
    			logMsg += (attach.encodedResp.length - end) + " remaining";
    		}
    		this.logger.info(logMsg);
    		if (b != (end - start)) {
    			this.logger.info("CAUTION: should have written "
    							+ (end - start)
    							+ " bytes to client "
    							+ attach.remoteAddr
    							+ " but instead wrote "
    							+ b
    							+ " bytes");
    		}
    	} catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
    	}
    	if (end < attach.encodedResp.length) {
    		attach.writeMarker = end;
            try {
            	// TODO Log re-register key
				/*SelectionKey retKey = */sc.register(this.myServe.getSelector(), SelectionKey.OP_WRITE, attach);
			} catch (ClosedChannelException exc) {
                this.myServe.getExceptionHandler().handleClosedChannelException(this.myServe, key, exc);
			}
    		return false;
    	}
    	return true;
    }
    
}
