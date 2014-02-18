package com.fhs.niosrv;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.niosrv.impl.hdlr.EchoMessageHandler;
import com.fhs.niosrv.rsc.Resourcer;
import com.fhs.niosrv.run.AsyncWriteRunnable;
import com.fhs.niosrv.run.HandlerRunnable;
import com.fhs.niosrv.run.KeyAttachment;
import com.fhs.niosrv.run.ReadRunnable;
import com.fhs.niosrv.run.SelectorRunnable;
import com.fhs.niosrv.you.impl.ExceptionHandler;
import com.fhs.niosrv.you.impl.MessageHandler;

/**
 * Non-Blocking <code>java.nio</code>nio Server Framework
 * 
 * @author Ben.Cole
 * 
 * TODO MAJOR Add custom uncaughtExceptionHandler to system
 *
 */
public class NIOServer implements Runnable {
    /** Default port number */
    private static final int DEF_PORT = 12380;
    /** Default read buffer size */
    private static final int DEF_RBUFF_SZ = 1028;
    /** Default write buffer size */
    private static final int DEF_WBUFF_SZ = 1028;
    
    /** ExceptionHandler */
    private ExceptionHandler excpHandler = new ExceptionHandler.DefaultExceptionHandlerImpl();
    /** Lock object for synchronizing ExceptionHandler access */
    private static final Object EXCP_LOCK = new Object();
    /** MessageHandler */
    private MessageHandler msgHandler = new EchoMessageHandler();
    /** Lock object for synchronizing MessageHandler access */
    private static final Object MSG_LOCK = new Object();
    
    /** Selector */
    private Selector selector;
    /** ServerSocketChannel */
    private ServerSocketChannel ssc;
    /**
     * Private flag -> should we run the main select thread/loop?
     */
    private volatile boolean doMainLoop = false;
    /**
     * watched variable - describes if server should be running or not
     */
    private volatile boolean isRunning = false;
    /**
     * Has the server been initialized?
     */
    private volatile boolean isInited = false;
    
    /** port number */
    private int myport = DEF_PORT;
    /** read buffer size */
    private int rbuff = DEF_RBUFF_SZ;
    /** write buffer size */
    private int wbuff = DEF_WBUFF_SZ;
    /**
     * Read event queue
     */
    private BlockingQueue<SelectionKey> rKQ = new LinkedBlockingQueue<>();
    /**
     * Message Queue, entries are messages waiting to be processed by a handler
     */
    private BlockingQueue<Message> mKQ = new LinkedBlockingQueue<>();
    /**
     * Write event queue
     */
    private BlockingQueue<SelectionKey> wKQ = new LinkedBlockingQueue<>();
    /**
     * Primary Selector thread
     */
    private Thread runThread;
    /**
     * Thread for processing read events and passing messages off to MessageHandlers
     */
    private Thread readThread;
    /**
     * Thread for processing messages and sending them to be written
     */
    private Thread hdlrThread;
    /**
     * Thread for consuming write events and writing responses to connections
     */
    private Thread writeThread;
    
    /**
     * Resources available to handler classes
     */
    private Resourcer resources;
    /**
     * Server GUI 
     */
    private NIOSGUI gui;
    
    /**
     * Logging object (Log4j2)
     */
    Logger logger = LogManager.getLogger();
    
    /**
     * Constructor
     */
    public NIOServer() { /**/ }
    
    /**
     * @return run the main select loop
     */
    public boolean doMainLoop() {
    	return this.doMainLoop;
    }
    
    /**
     * @return isInited
     */
    public boolean isInited() {
    	return this.isInited;
    }
    
    /**
     * @return isRunning
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * @return the current port number
     */
    public int getPort() {
    	return this.myport;
    }
    
    /**
     * @return the rKQ
     */
	public BlockingQueue<SelectionKey> getReadQueue() {
        return this.rKQ;
    }
	
    /**
     * @return the mKQ
     */
	public BlockingQueue<Message> getMessageQueue() {
		return this.mKQ;
	}

    /**
     * @return the wKQ
     */
    public BlockingQueue<SelectionKey> getWriteQueue() {
        return this.wKQ;
    }

    /**
     * @return read buffer size
     */
    public int getReadBufferSize() {
        return this.rbuff;
    }

    /**
     * @return write buffer size
     */
    public int getWriteBufferSize() {
        return this.wbuff;
    }

    /**
     * @return an instance of ExceptionHandler
     */
    public ExceptionHandler getExceptionHandler() {
        ExceptionHandler excp;
        synchronized (EXCP_LOCK) {
            excp = this.excpHandler;
        }
        return excp;
    }

    /**
     * @return an instance of MessageHandler
     */
    public MessageHandler getMessageHandler() {
        MessageHandler msg;
        synchronized (MSG_LOCK) {
            msg = this.msgHandler;
        }
        return msg;
    }
    
    /**
     * @return Resourcer 
     */
    public Resourcer getResources() {
        if (this.resources == null) {
            this.resources = new Resourcer(this);
        }
        return this.resources;
    }
    
    /**
     * @param buff the size to set the read buffer
     */
    public void setReadBufferSize(int buff) {
    	this.logger.info("Read buffer set to " + buff);
        this.rbuff = buff;
    }

    /**
     * @param buff the size to set the write buffer
     */
    public void setWriteBufferSize(int buff) {
    	this.logger.info("Write buffer set to {}" + buff);
        this.wbuff = buff;
    }

    /** 
     * TODO:
     *  Instead of setting ExceptionHandler instance, set ExceptionHandler class and use Object pooling to retrieve instances
     * @param exceptionHandler
     */
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        synchronized (EXCP_LOCK) {
            this.excpHandler = exceptionHandler;
        }
    	this.logger.info("ExceptionHandler set to "  + exceptionHandler.getClass().getName());
    }

    /** 
     * TODO:
     *  Instead of setting MessageHandler instance, set MessageHandler class and use Object pooling to retrieve instances
     * @param messageHandler
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        synchronized (MSG_LOCK) {
            this.msgHandler = messageHandler;
        }
    	this.logger.info("MessageHandler set to " +  messageHandler.getClass().getName());
    }

    /**
     * Same as <code>init(NIOServer.DEFAULT_PORT)</code><br />
     * No-op if already <code>init</code>-ed
     * 
     * @throws IOException
     */
    public void init() {
        if (this.isInited) return;
        try {
			this.selector = Selector.open();
	        this.ssc = ServerSocketChannel.open();
	        this.getSSC().configureBlocking(false);
	        InetSocketAddress addr = new InetSocketAddress(this.myport);
	        this.getSSC().bind(addr);
	        /*SelectionKey key = */this.getSSC().register(this.getSelector(), SelectionKey.OP_ACCEPT);
	        this.isInited = true;
	        StringBuilder logMsg = new StringBuilder("Initialized NIOServer on port ")
	        								.append(this.myport)
	        								.append(" with r/w buffer sizes ")
	        								.append(this.rbuff)
	        								.append("/")
	        								.append(this.wbuff);
	        this.logger.info(logMsg.toString());
		} catch (IOException exc) {
			this.getExceptionHandler().handleInitException(this, exc);
		}
    }
    
    /**
     * @param port Port number to listen to
     * @throws IOException
     */
    public void init(int port) {
        if (this.isInited) return;
        this.myport = port;
        this.init();
    }
    
    /**
     * @param port
     * @param readBufferSize
     * @param writeBufferSize
     */
    public void init(int port, int readBufferSize, int writeBufferSize) {
        if (this.isInited) return;
        this.rbuff = readBufferSize;
        this.wbuff = writeBufferSize;
        this.myport = port;
        this.init();        
    }
    
    /**
     * Although we override the run method, this class isn't designed (yet) to be run in it's own thread.  It starts sub-threads itself, and then returns.
     */
    @Override
    public synchronized void run() {
    	this.doMainLoop = true;
    	this.isRunning = true;
        this.logger.info("NIOServer start initiated...");
    	String runName = "NIOServer-readthread-"+this.myport;
    	String hdlName = "NIOServer-handlerthread-"+this.myport;
    	String readName = "NIOServer-writethread-"+this.myport;
    	String writeName = "NIOServer-runthread-"+this.myport;
    	
        this.runThread = new Thread(new SelectorRunnable(this), runName);
        this.readThread = new Thread(new ReadRunnable(this), readName);
        // TODO # of handler threads == number of handlers, curr => 1
        this.hdlrThread = new Thread(new HandlerRunnable(this), hdlName);
        this.writeThread = new Thread(new AsyncWriteRunnable(this), writeName);
        this.logger.info("...threads created...");
        
        this.readThread.setName(runName);
        this.readThread.setDaemon(false);
        this.readThread.start();
        this.logger.info("...read thread initiated...");
        
        this.hdlrThread.setName(hdlName);
        this.hdlrThread.setDaemon(false);
        this.hdlrThread.start();
        this.logger.info("...handler thread(s) initiated...");
        
        this.writeThread.setName(readName);
        this.writeThread.setDaemon(false);
        this.writeThread.start();
        this.logger.info("...write thread initiated...");
        
        this.runThread.setName(writeName);
        this.runThread.setDaemon(false);
        this.runThread.start();
        this.logger.info("...selector thread initiated...");
        
        /*
         * TODO : allow CLI interaction - not just gui control [arg option --no-gui]
         */
        this.logger.info("...NIOServer started!");
    }

    /**
     * Gracefully stop accepting new connections.
     * @throws IOException 
     */
    public synchronized void stop() {
    	if (!this.isRunning) return;
        this.isRunning = false;
        this.logger.info("Shutdown initiated...");
        this.logger.info("waiting for threads...");
        while(this.hdlrThread.isAlive() || this.readThread.isAlive() || this.writeThread.isAlive()) {
        	Thread.yield();
        }
        this.doMainLoop = false;
        
        try {
            this.logger.info("stopping main loop thread...");
			this.runThread.join();
		} catch (InterruptedException exc1) {
			// ignore
		}

        this.logger.info("Closing external ports...");
        try {
	        this.selector.close();
	        this.ssc.close();
        } catch (IOException exc) {
        	this.getExceptionHandler().handleShutdownException(this, exc);
        }
        this.selector = null;
        this.ssc = null;
        this.runThread = null;
        this.readThread = null;
        this.hdlrThread = null;
        this.writeThread = null;
        this.isInited = false;
        this.logger.info("Shutdown complete!");
    }
    
    /**
     * Convenience method to differentiate between a graceful shutdown and a hard destroy.
     */
    public synchronized void destroy() {
    	this.stop();
    	this.logger.info("SERVER DESTROYED!  ------------------------------->  TROGDOR PREVAILS!");
    	this.logger.info("BURNINATINGTHECOUNTRYSIDEBURNINATINGTHEPEOPLETROGDOOOOOOOOOOOOOOOOOOOR");
    }
    
    /**
     * Read from the SocketChannel associated with the given SelectionKey
     * 
     * @param key SelectionKey 
     */
    public void doRead(SelectionKey key) {
        // remove from selector's list of keys -- avoid multiple select-for-reads
        key.cancel();
        this.getReadQueue().offer(key);
    }

    /**
     * @param key SelectionKey
     */
    public void doConnectable(SelectionKey key) {
        key.cancel();
        
        // Schedule for read, if not already
        if (!key.isReadable()) {
            try {
                /*SelectionKey newKey = */key.channel().register(this.getSelector(), SelectionKey.OP_READ, key.attachment());
            } catch (ClosedChannelException e) {
                this.getExceptionHandler().handleClosedChannelException(this, key, e);
            }
        }
    }

    /**
     * Remove key from Selector, then add to write queue. This method, and the write-queue consumer-thread, assumes that the message to be written is attached to the key, and will be retrieved with
     * <code>key.attachment()</code>.
     * 
     * @param key
     *            SelectionKey
     */
    public void doWrite(SelectionKey key) {
        key.cancel();
        this.getWriteQueue().offer(key);
    }

    /**
     * Close a connection
     * 
     * @param key
     * @throws IOException
     */
    public void closeConnection(SelectionKey key) throws IOException {
        this.logger.info("Disconnected from " + ((KeyAttachment)key.attachment()).remoteAddr);
        ((SocketChannel)key.channel()).shutdownInput();
        ((SocketChannel)key.channel()).shutdownOutput();
        ((SocketChannel)key.channel()).close();
        key.cancel();
    }

    /**
     * @return the ssc
     */
    public ServerSocketChannel getSSC() {
        return this.ssc;
    }

    /**
     * @return the attached GUI console for this server.  May be null if <code>enableConsole</code> hasn't been called.
     */
    public NIOSGUI getGUI() {
    	return this.gui;
    }
    
    /**
     * @return the selector
     */
    public Selector getSelector() {
        return this.selector;
    }

	/**
	 * Create and show the attached GUI console for this server.
	 */
	public void enableConsole() {
		final NIOServer nios = this;
		nios.gui = new NIOSGUI();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				nios.getGUI().attachServer(nios);
				nios.getGUI().setVisible(true);
				nios.logger.info("GUI Console enabled!");
			}
		});
	}
	
	
	
}

