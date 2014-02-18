package com.fhs.jlattice.you.impl;

import com.fhs.jlattice.LatticeServer;
import com.fhs.jlattice.rsc.DestructionException;
import com.fhs.jlattice.rsc.InitializationException;


/**
 * Interface defining a 'Resource', where a 'Resource' is an external Object, or set of Objects, that can be accessed in a thread-safe manner.
 * Built to complement NIOServer.java, where Resource can be requested by MessageHandlers while they are processing messages.  This is a way to 
 * provide access to resources such as database connection pools, and other such things.
 * <br /><br />
 * A resource must have a no-op constructor!
 * <br />
 * @author Ben.Cole
 *
 */
public interface Resource {
    /**
     * @return name used to refer to this resource
     */
    public String getName();
    /**
     * Initialize this resource.  This should be a no-op if already called.
     * @param server 
     * 
     * @throws InitializationException
     */
    public void init(LatticeServer server) throws InitializationException;
    /**
    * Initialize this resource with the given arguments.  This should be a no-op if already called.
    * @param server 
    * @param args
    * 
    * @throws InitializationException
    */
    public void init(LatticeServer server, String[] args) throws InitializationException;
    /**
     * Destroy this resource.  May not be required if the resource is non-consumable.
     * @param server 
     * 
     * @throws DestructionException
     */
    public void destroy(LatticeServer server) throws DestructionException;
}
