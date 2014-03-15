package com.fhs.jlattice;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fhs.jlattice.rsc.Resourcer;
import com.fhs.jlattice.run.AsyncWriteRunnable;
import com.fhs.jlattice.run.HandlerRunnable;
import com.fhs.jlattice.run.KeyAttachment;
import com.fhs.jlattice.run.ReadRunnable;
import com.fhs.jlattice.run.SelectorRunnable;
import com.fhs.jlattice.you.impl.ExceptionHandler;
import com.fhs.jlattice.you.impl.MessageHandler;
import com.fhs.niosrv.impl.hdlr.EchoMessageHandler;

/**
 * Non-Blocking <code>java.nio</code>nio Server Framework
 * 
 * @author Ben.Cole
 * 
 * TODO MAJOR Add custom uncaughtExceptionHandler to system
 *
 */
public class LatticeServer {
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
    /**
     * 
     */
    Class<? extends MessageHandler> handlerClass = EchoMessageHandler.class;
    ArrayList<HandlerRunnable> handlerPool = new ArrayList<>();
    private volatile int handlerPoolSize = 1;
    
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
    private BlockingQueue<SelectionKey> rKQ;
    /**
     * Message Queue, entries are messages waiting to be processed by a handler
     */
    private BlockingQueue<Message> mKQ;
    /**
     * Write event queue
     */
    private BlockingQueue<SelectionKey> wKQ;
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
    private Map<HandlerRunnable, Thread> handlerThreads = new java.util.HashMap<>();
    /**
     * TODO handlerThread monitor thread - restarts dead threads, performs scaling, etc
     */
    private Thread handlerWrangler;
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
    private LatticeGUI gui;
    
    /**
     * Logging object (Log4j2)
     */
    Logger logger = LogManager.getLogger();
    
    /**
     * Constructor
     */
    public LatticeServer() { /**/ }
    
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
     * @return the set message handler class
     */
    public Class<? extends MessageHandler> getMessageHandlerClass() {
    	return this.handlerClass;
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
     * @param handlerClass class of custom MessageHandler object
     */
    public void setMessageHandlerClass(Class<? extends MessageHandler> handlerClass) {
    	this.handlerClass = handlerClass;
    }
    
    /**
     * @param size Desired number of handler threads
     */
    public void setMessageHandlerPoolSize(int size) {
    	this.handlerPoolSize = size;
    	//this.scaleHandlers(size);
    }
    
   /* 
    private void scaleHandlers(int poolSz) {
    	if (poolSz <= 0) return;
    	if (this.handlerPool.size() == poolSz) return;
    	if (this.handlerPool.size() > poolSz) {
    		scaleDown(poolSz);
    	} else {
    		scaleUp(poolSz);
    	}
    }
    
    private void scaleDown(int poolSz) {
    	int killCnt = this.handlerPool.size() - poolSz;
    	for (int i = 0; i < killCnt; i++) {
    		this.handlerPool.get(i).kill();
    	}
    	int deadCnt = 0;
    	while (deadCnt < killCnt) {
    		for (int i = this.handlerPool.size(); i > 0; i--) {
    			if (this.handlerPool.get(i).isDead()) {
    				this.handlerPool.remove(i);
    				deadCnt++;
    			}
    		}
    	}
    }*/
    
    /**
     * Same as <code>init(NIOServer.DEFAULT_PORT)</code><br />
     * No-op if already <code>init</code>-ed
     * 
     * @throws IOException
     */
    public void init() {
        if (this.isInited) return;
        rKQ = new LinkedBlockingQueue<>();
        mKQ = new LinkedBlockingQueue<>();
        wKQ = new LinkedBlockingQueue<>();
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
    public synchronized void run() {
    	this.doMainLoop = true;
    	this.isRunning = true;
        this.logger.info("NIOServer start initiated...");
    	String runName = "NIOServer-runthread-"+this.myport;
    	String readName = "NIOServer-readthread-"+this.myport;
    	String writeName = "NIOServer-writethread-"+this.myport;
    	
    	// TODO alter startup order to initialize writeThread first, then handlers, then readthread, then, last, runThread
        this.runThread = new Thread(new SelectorRunnable(this), runName);
        this.readThread = new Thread(new ReadRunnable(this), readName);
        setupHandlers();
        this.writeThread = new Thread(new AsyncWriteRunnable(this), writeName);
        this.logger.info("...threads created...");
        
        this.readThread.setName(runName);
        this.readThread.setDaemon(false);
        this.readThread.start();
        this.logger.info("...read thread initiated...");
        
        int cnt = 0;
        for (Thread handlr : this.handlerThreads.values()) {
        	handlr.start();
            this.logger.info("...handler thread " + cnt + " initiated...");
            cnt++;
        }
        
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
    
    private void setupHandlers() {
    	for (int i = 0; i < this.handlerPoolSize; i++) {
    		HandlerRunnable runn;
			try {
				runn = new HandlerRunnable(this, i);
	    		Thread handlerThread = new Thread(runn);
	    		handlerThread.setName("messageHandler-" + this.myport + "-" + i);
	    		handlerThread.setDaemon(false);
	    		this.handlerPool.add(runn);
	    		this.handlerThreads.put(runn, handlerThread);
			} catch (InstantiationException | IllegalAccessException exc) {
				// TODO handle exception
			}
    	}
        
    }
    
    private boolean checkHandlers() {
    	boolean ret = false;
    	for (Thread thread : this.handlerThreads.values()) {
    		if (thread.isAlive()) {
    			ret = true;
    			break;
    		}
    	}
    	return ret;
    }
    
    private void doInterrupt() {
    	if (runThread.isAlive()) {
    		runThread.interrupt();
    	}
    	for (Thread thread : this.handlerThreads.values()) {
    		if (thread.isAlive()) {
    			thread.interrupt();
    		}
    	}
    	if (writeThread.isAlive()) {
    		writeThread.interrupt();
    	}
    	if (readThread.isAlive()) {
    		readThread.interrupt();
    	}
    }
    
    /**
     * <strong>INTERNAL USE ONLY</strong><br />
     * Gracefully stop accepting new connections.
     * @throws IOException 
     */
    public synchronized void stop() {
    	if (!this.isRunning) return;
        this.isRunning = false;
        this.logger.info("Shutdown initiated...waiting for threads...");
        while(checkHandlers() || this.readThread.isAlive() || this.writeThread.isAlive()) {
        	doInterrupt();
        	Thread.yield();
        }
        this.doMainLoop = false;
        
        try {
            this.logger.info("...stopping main loop thread...");
			this.runThread.join();
		} catch (InterruptedException exc1) {
			// ignore
		}

        // technically possible for errors to occur if the join() to runThread was interrupted
        try {
        	this.logger.trace("...closing selector...");
	        this.selector.close();
        	this.logger.trace("...closing socket channel...");
	        this.ssc.close();
        } catch (IOException exc) {
        	this.getExceptionHandler().handleShutdownException(this, exc);
        }
        this.selector = null;
        this.ssc = null;
        this.runThread = null;
        this.readThread = null;
        this.handlerThreads.clear();
        this.handlerPool.clear();
        this.writeThread = null;
        this.isInited = false;
        this.logger.info("Shutdown complete!");
    }
    
    /**
     * <strong>INTERNAL USE ONLY</strong><br />
     * Convenience method to differentiate between a graceful shutdown and a hard destroy.
     */
    public synchronized void destroy() {
    	this.stop();
    	this.logger.info("SERVER DESTROYED!  ------------------------------->  TROGDOR PREVAILS!");
    	this.logger.info("BURNINATINGTHECOUNTRYSIDEBURNINATINGTHEPEOPLETROGDOOOOOOOOOOOOOOOOOOOR");
    }
    
    /**
	 * <strong>INTERNAL USE ONLY</strong><br />
	 * @return run the main select loop
	 */
	public boolean doMainLoop() {
		return this.doMainLoop;
	}

	/**
     * <strong>INTERNAL USE ONLY</strong><br />
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
     * <strong>INTERNAL USE ONLY</strong><br />
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
     * <strong>INTERNAL USE ONLY</strong><br />
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
     * <strong>INTERNAL USE ONLY</strong><br />
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
     * <strong>INTERNAL USE ONLY</strong><br />
     * @return the ssc
     */
    public ServerSocketChannel getSSC() {
        return this.ssc;
    }

    /**
     * @return the attached GUI console for this server.  May be null if <code>enableConsole</code> hasn't been called.
     */
    public LatticeGUI getGUI() {
    	return this.gui;
    }
    
    /**
     * <strong>INTERNAL USE ONLY</strong><br />
     * @return the selector
     */
    public Selector getSelector() {
        return this.selector;
    }

	/**
	 * Create and show the attached GUI console for this server.
	 */
	public void enableConsole() {
		final LatticeServer nios = this;
		nios.gui = new LatticeGUI();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				nios.getGUI().attachServer(nios);
				nios.getGUI().setVisible(true);
				nios.logger.info("GUI Console enabled!");
			}
		});
	}
	
	
	
}

