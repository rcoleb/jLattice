package com.fhs.niosrv;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import java.awt.Color;


/**
 * Simplistic start/stop log display GUI for a generic NIOServer instance
 * 
 * @date Feb 9, 2014
 * @author Ben
 *
 */
public class NIOSGUI {

	JFrame frame;
	
	NIOServer server;
	JTextField fieldPort;

	JScrollPane scrollPane;

	JTextArea logConsole;

	JButton btnStart;

	JButton btnStop;
	JButton btnClear;
	JButton btnDestroy;
	JButton btnQuit;
	
	ExecutorService offEDTRunner = Executors.newSingleThreadExecutor();

	/**
	 * # of lines to read from the end of the log file and display in the text area
	 */
	public static final int LINE_BUFFER = 50;
	
	/**
	 * Create the application.
	 */
	public NIOSGUI() {
		initialize();
	}
	
	Action actionStart = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					if (NIOSGUI.this.server.isRunning()) return;
			        NIOSGUI.this.btnClear.doClick();
					// check if port field populated
					String portStr = NIOSGUI.this.fieldPort.getText();
					if (portStr.length() == 0) {
						// error
					}
					int port = Integer.parseInt(portStr);
					NIOSGUI.this.server.init(port);
					NIOSGUI.this.server.run();
					
					NIOSGUI.this.started();
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	Action actionStop = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					if (!NIOSGUI.this.server.isRunning()) {
						NIOSGUI.this.stopped();
						return;
					}
					NIOSGUI.this.server.stop();
					NIOSGUI.this.stopped();
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	Action actionClear = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					StringBuilder appender = new StringBuilder(NIOSGUI.this.logConsole.getText());
					appender.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
					NIOSGUI.this.logConsole.setText(appender.toString());
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	Action actionEmptyLog = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					try(RandomAccessFile file = new RandomAccessFile(new File(System.getProperty("user.dir") + "\\logs\\app.log"), "rws")) {
						file.setLength(0);
					} catch (IOException exc) {
						exc.printStackTrace();
					}
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	Action actionQuit = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					NIOSGUI.this.btnStop.doClick();
					NIOSGUI.this.server.destroy();
					NIOSGUI.this.frame.setVisible(false);
					System.exit(0);
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	Action actionDestroy = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Runnable runn = new Runnable() {
				@Override
				public void run() {
					// PROBS NEVER USE THIS... KTHXBAI
					System.exit(1);
				}
			};
			NIOSGUI.this.offEDTRunner.execute(runn);
		}
	};
	
	
	WatchService watcher;
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.frame = new JFrame();
		this.frame.setBounds(100, 100, 1200, 500);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new MigLayout("", "[][][][grow]", "[][][][grow][][][][]"));
		
		JLabel lblNioserver = new JLabel("NIOServer ");
		lblNioserver.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 11));
		this.frame.getContentPane().add(lblNioserver, "flowy,cell 0 0 2 1");
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		this.frame.getContentPane().add(separator, "cell 2 0 1 8,growy");
		
		JLabel lblLogConsole = new JLabel("Log Console:");
		this.frame.getContentPane().add(lblLogConsole, "flowx,cell 3 0,growx");
		
		JLabel lblPort = new JLabel("Port:");
		this.frame.getContentPane().add(lblPort, "cell 0 1,alignx trailing");
		
		this.scrollPane = new JScrollPane();
		this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.frame.getContentPane().add(this.scrollPane, "cell 3 1 1 7,grow");
		
		this.logConsole = new JTextArea();
		this.logConsole.setEditable(false);
		this.scrollPane.setViewportView(this.logConsole);
		
		this.fieldPort = new JTextField();
		this.frame.getContentPane().add(this.fieldPort, "cell 0 2 2 1,growx");
		this.fieldPort.setColumns(10);
		
		this.btnStart = new JButton(this.actionStart);
		this.btnStart.setText("Start");
		this.frame.getContentPane().add(this.btnStart, "cell 0 4 2 1,growx");
		
		this.btnStop = new JButton(this.actionStop);
		this.btnStop.setText("Stop");
		this.frame.getContentPane().add(this.btnStop, "cell 0 5 2 1,growx");
		
		this.btnClear = new JButton(this.actionClear);
		this.btnClear.setText("Clear Window");
		this.frame.getContentPane().add(this.btnClear, "cell 3 0,alignx right");
		
		this.btnDestroy = new JButton(this.actionDestroy);
		this.btnDestroy.setText("Destroy");
		this.btnDestroy.setForeground(new Color(220, 20, 60));

		this.btnQuit = new JButton(this.actionQuit);
		this.btnQuit.setText("Quit");
		this.frame.getContentPane().add(this.btnQuit, "cell 0 6 2 1,growx");
		this.frame.getContentPane().add(this.btnDestroy, "flowx,cell 0 7 2 1,growx");
		
		this.btnEmptyLogFile = new JButton(this.actionEmptyLog);
		this.btnEmptyLogFile.setText("Empty Log FIle");
		this.frame.getContentPane().add(this.btnEmptyLogFile, "cell 3 0");
	}
	
	protected void attachServer(final NIOServer nios) {
		this.server = nios;
		this.fieldPort.setText(String.valueOf(nios.getPort()));
		if (nios.isRunning())  {
			this.started();
			final String lines = readLastLogLines();
			NIOSGUI.this.logConsole.setText(lines);	        
		}
		
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
			
			
			final Path logPath = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "\\logs\\");
			final WatchKey key = logPath.register(this.watcher, StandardWatchEventKinds.ENTRY_MODIFY);
			
			Thread logUpdateThread = new Thread(new Runnable() {
				private volatile boolean isRunning = false;
				@Override
				public void run() {
					while(true) {
						WatchKey myKey;
						myKey = NIOSGUI.this.watcher.poll();
						if (this.isRunning() || ((myKey != null && myKey.pollEvents().isEmpty()) && key.pollEvents().isEmpty()) ) {
							Thread.yield();
						}
						setRunning(true);
						
						final String lines = readLastLogLines();
						updateLogConsole(lines);
						
						setRunning(false);
					}
				}
				public boolean isRunning() {
					return this.isRunning;
				}
				public void setRunning(boolean isRunning) {
					this.isRunning = isRunning;
				}
			});
			logUpdateThread.setDaemon(true);
			logUpdateThread.start();
		} catch (IOException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		
	}
	
	/**
	 * Pass-through/convenience method
	 * @param b
	 */
	public void setVisible(boolean b) {
		this.frame.setVisible(b);
	}
	
	protected void started() {
		this.btnStart.setEnabled(false);
		this.btnStop.setEnabled(true);
		this.fieldPort.setEditable(false);
	}
	
	protected void stopped() {
		this.btnStop.setEnabled(false);
		this.btnStart.setEnabled(true);
		this.fieldPort.setEditable(true);
	}
	
	long temp_prevLogLength = 0;
	private JButton btnEmptyLogFile;
	String readLastLogLines() {
		try(RandomAccessFile file = new RandomAccessFile(new File(System.getProperty("user.dir") + "\\logs\\app.log"), "r")) {
			long fileLength = file.length() - 1;
			if (fileLength <= 0) return "";
	        StringBuilder sb = new StringBuilder();
	        
	        int line = 0;

	        for(long filePointer = fileLength; filePointer > this.temp_prevLogLength; filePointer--){
	        	file.seek( filePointer );
	            int readByte = file.readByte();

	            if( readByte == 0xA && (line == LINE_BUFFER || filePointer == this.temp_prevLogLength)) { // line feed
					break;
	            } else if( readByte == 0xD ) { // carriage return
	                line = line + 1;
	                if (filePointer == this.temp_prevLogLength - 1) {
                        continue;
                    } else if (line == LINE_BUFFER) {
						break;
	                }
	            }
	            sb.append( ( char ) readByte );
	        }
	        this.temp_prevLogLength = fileLength;
	        file.close();
	        String lastLine = sb.reverse().toString().trim();
	        if (lastLine.length() > 0) {
	        	lastLine += "\n";
	        }
	        sb = null;
	        return lastLine;
		} catch (IOException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		return this.logConsole.getText();
	}
	
	void updateLogConsole(final String newLines) {
		if (newLines.length() == 0) return;
		try {
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
			        NIOSGUI.this.logConsole.setText(NIOSGUI.this.logConsole.getText()+newLines);
				}
			});
		} catch (InvocationTargetException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		} catch (InterruptedException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}
	// C:\Users\Ben\workspace\ClickServer\src\com\fhs\clksrv\HotSpots.json
}
