package com.fhs.niosrv.run;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.niosrv.Message;
import com.fhs.niosrv.NIOServer;

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
    private NIOServer myServe;
    
    Logger logger = LogManager.getLogger();
    
    private static final int POLL_WAIT = 1000;
    
    /**
     * 
     * @param server
     */
    public AsyncWriteRunnable(NIOServer server) { this.myServe = server; }

    @Override
    public void run() {
    	ArrayList<SelectionKey> batch = new ArrayList<>();
        while(this.myServe.isRunning()) {
        	batch = new ArrayList<>(this.myServe.getWriteQueue().size());
        	int count = this.myServe.getWriteQueue().drainTo(batch);
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
				/*SelectionKey retKey = */sc.register(this.myServe.getSelector(), SelectionKey.OP_WRITE, attach);
			} catch (ClosedChannelException exc) {
                this.myServe.getExceptionHandler().handleClosedChannelException(this.myServe, key, exc);
			}
    		return false;
    	}
    	return true;
    }
    
}
