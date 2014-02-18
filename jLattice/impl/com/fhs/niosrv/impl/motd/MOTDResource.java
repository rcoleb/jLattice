package com.fhs.niosrv.impl.motd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.rsc.DestructionException;
import com.fhs.jlattice.rsc.InitializationException;
import com.fhs.jlattice.you.impl.Resource;

/**
 * Resource for loading a Message-Of-The-Day file and providing Messages
 * <br />
 * Currently requires a window session, as this prompts for file location using JOptionPane.  
 * Later implementations will retrieve the console from the server and prompt through that.
 * <br />
 * @author Ben.Cole
 *
 */
public class MOTDResource implements Resource {
    /**
     * MOTD file location - MOTD file must be new-line delimited
     */
    String fileLoc;
    /**
     * loaded MOTDs
     */
    String[] motds;
    
    @Override
    public String getName() {
        return "motd";
    }
    
    @Override
    public void init(LatticeServer server) throws InitializationException {
        this.fileLoc = JOptionPane.showInputDialog("Please Enter MOTD File location:");
        loadStrings();
    }
    
    @Override
    public void init(LatticeServer server, String[] args) throws InitializationException {
    	this.fileLoc = args[0];
    	loadStrings();
    }
    
    @Override
    public void destroy(LatticeServer server) throws DestructionException {
        this.motds = null;
    }
    
    /**
     * Load MOTD Strings
     */
    private void loadStrings() {
        if (this.fileLoc != null) {
            Path path = Paths.get(this.fileLoc);
            try {
                List<String> strs = Files.readAllLines(path, StandardCharsets.UTF_8);
                this.motds = new String[strs.size()];
                this.motds = strs.toArray(this.motds);
            } catch (IOException e) {
                this.motds = new String[]{"Your web admin hasn't specified an MOTD file!  Better get on that..."};
                // we're avoiding all printing to console - e.printStackTrace();
                // if anything, we should store a reference to the NIOServer and 
                // then request the ExceptionHandler and have it act appropriately...
            }
        }
    }
    
    /**
     * Get a random MOTD String
     * 
     * @param reload Re-load MOTD strings?
     * @return
     */
    public String getRandomMOTD(boolean reload) {
        if (reload) loadStrings();
        int ind = (new Random()).nextInt(this.motds.length)+1;
        return this.motds[ind];
    }
    
}
