package com.fhs.niosrv.run;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import com.fhs.niosrv.Message;
import com.fhs.niosrv.NIOServer;

/**
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
*  TODO implement re-registering key with OP_WRITE with msgBytes and index offset (NON-BLOCKING, DAMMIT! :) )<br />
* <br />
* @author Ben.Cole
* 
*/
public final class WriteRunnable implements Runnable {
    /**
     * Containing NIOServer
     */
    private NIOServer myServe;

    /**
     * 
     * @param server
     */
    public WriteRunnable(NIOServer server) { this.myServe = server; }

    @Override
    public void run() {
        while(this.myServe.isRunning()) {
            SelectionKey key = this.myServe.getWriteQueue().poll();
            if (key == null) continue;
            /** we close this in a few lines */
            @SuppressWarnings("resource") 
			SocketChannel sc = (SocketChannel) key.channel();
            KeyAttachment attach = (KeyAttachment) key.attachment();
            
            if (attach.response != null) {
                byte[] msgBytes = attach.response.getEncodedResponse();
                if (msgBytes.length > this.myServe.getWriteBufferSize()) {
                    writeMult(msgBytes, sc, key);
                } else {
                    write(msgBytes, sc, key);
                }
            }

            try {
                this.myServe.closeConnection(key);
                sc = null;
            } catch (IOException e) {
                this.myServe.getExceptionHandler().handleCloseException(this.myServe, e);
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
    private void write(byte[] msgBytes, SocketChannel sc, SelectionKey key) {
        try {
            sc.configureBlocking(false);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
        ByteBuffer wb = ByteBuffer.allocate(this.myServe.getWriteBufferSize());
        wb.put(msgBytes);
        wb.flip();
        try {
            int b = sc.write(wb);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
    }
    
    /**
     * Write a byte[] message that is larger in size to the size of the write buffer, and must be split up
     * 
     * @param msgBytes
     * @param sc
     * @param key
     */
    private void writeMult(byte[] msgBytes, SocketChannel sc, SelectionKey key) {

        try {
            sc.configureBlocking(true);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
        ByteBuffer[] retArr = new ByteBuffer[(msgBytes.length / this.myServe.getWriteBufferSize())+1];
        
        for (int index = 0; index < retArr.length; index++) {
                retArr[index] = ByteBuffer.allocate(this.myServe.getWriteBufferSize());
                int start = index * this.myServe.getWriteBufferSize();
                byte[] subArr = Arrays.copyOfRange(msgBytes, start, start + this.myServe.getWriteBufferSize());
                retArr[index] = retArr[index].put(subArr);
                retArr[index] = (ByteBuffer) retArr[index].flip();
        }
        
        try {
            long b = 0l;
            for (int i = 0; i < retArr.length; i++) {
                b += sc.write(retArr[i]);
            }
            // TODO why doesn't multi-write work?
            // long b = sc.write(retArr);
        } catch (IOException e) {
            this.myServe.getExceptionHandler().handleWriteException(this.myServe, key, e);
        }
    }
}
