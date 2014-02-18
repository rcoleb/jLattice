package com.fhs.niosrv.rsc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fhs.niosrv.NIOServer;
import com.fhs.niosrv.you.impl.Resource;


/**
 * TODO The synchronization in this class suuuuuuuucks.  Some bits are locked, but which and why isn't defined.  Booooooooo bad coding!
 * 
 * NOTE: Can we replace with object pooling?  Discuss:
 * 
 * @author Ben.Cole
 *
 */
public class Resourcer {
    /**
     * TODO need per-resource access lock
     */
    private static final Object MAP_LOCK = new Object();
    /**
     * 
     */
    private Map<String, Class<? extends Resource>> rscMap;
    /**
     * 
     */
    private Map<String, Resource> rscs;
    /**
     * TODO need a better way to associate names, locks, and esp. instances of Resources.  Also, # allowable locks per indiv. instance; likely to be 1 most often
     */
    private Map<String, Integer> locks;
    /**
     * 
     */
    private NIOServer server;
    
    /**
     * @param myServe
     */
    public Resourcer(NIOServer myServe) {
        this.server = myServe;
        this.rscMap = new ConcurrentHashMap<>();
        this.rscs = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
    }
    
    /**
     * TODO use clz.getName(); specify # at init. creation
     * 
     * @param name
     * @param clz
     * @param createNow
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InitializationException
     */
    public void defineResource(String name, Class<? extends Resource> clz, boolean createNow) throws InstantiationException, IllegalAccessException, InitializationException {
        this.rscMap.put(name, clz);
        if (createNow) {
            Resource rsx = clz.newInstance();
            rsx.init(this.server);
            this.rscs.put(name, rsx);
        }
    }
    
    /**
     * TODO use clz.getName(); specify # at init. creation
     * 
     * @param name
     * @param clz
     * @param createNow
     * @param rscArgs
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InitializationException
     */
    public void defineResource(String name, Class<? extends Resource> clz, boolean createNow, String... rscArgs) throws InstantiationException, IllegalAccessException, InitializationException {
        this.rscMap.put(name, clz);
        if (createNow) {
            Resource rsx = clz.newInstance();
            rsx.init(this.server, rscArgs);
            this.rscs.put(name, rsx);
        }
    }
    
    /**
	 * <strong>CAUTION:</strong> If you pass <code>false</code> for the
	 * <code>block</code> parameter, you <i>will</i> be given the requested
	 * resource. If this resouce is improperly synchronized, this could result
	 * in <strong>very bad things</strong>! Or not. Y'know, depends on how you
	 * use it! Also, the server itself could play a role in this: there may be
	 * multiple Worker threads running, or there may only be one. Up to you!
	 * 
	 * @param name
	 * @param block
	 * @return requested Resource
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InitializationException
	 * @throws UndefinedResourceException
	 */
    public Resource requestResource(String name, boolean block) throws InstantiationException, IllegalAccessException, InitializationException, UndefinedResourceException {
        boolean definedAlready = this.rscs.containsKey(name);
        Resource rsx;
    // have we already created an instance of this resource?
        if (definedAlready) {
            boolean lentAlready = this.locks.containsKey(name);
        // have we already issued this resource at least once (could be that a reference hasn't been returned though)?
            if (lentAlready) {
            // should we block until all issuances have been returned?  If a block fails to return a resource, we could be waiting a long time...
                if (block) {
                    
                    //////
                    /*
					 * TODO MAJOR - allow setting of an instance limit - if
					 * blocked, create a new instance, up to quota. Otherwise,
					 * what to do if quota is reached, but non-blocking has been
					 * requested? Just break the rules and return an instance
					 * anyway?
					 */
                    ///////
                    
                    while (lentAlready) {
                        Thread.yield();
                        lentAlready = this.locks.containsKey(name);
                    }
                }
                // if blocked long enough, not-blocked, or not lent yet, continue:
            }
            // define return value
            rsx = this.rscs.get(name);
        // increment lock-issuance counter
            synchronized(MAP_LOCK) {
                Integer lk = this.locks.get(name);
                if (lk == null) {
                    lk = new Integer(0);
                }
                lk = Integer.valueOf(lk.intValue() + 1);
                this.locks.put(name, lk);
            }
    // if not defined, create an instance, set references and a lock count, and return
        } else {
            Class<? extends Resource> clz;
            clz = this.rscMap.get(name);
            if (clz == null) throw new UndefinedResourceException(name);
            rsx = clz.newInstance();
            rsx.init(this.server);
            this.rscs.put(name, rsx);
            this.locks.put(name, Integer.valueOf(1));
        }
        return rsx;
    }
    
    /**
     * Return a resource.
     * <br />
     * Decrements issued count for this resource.  If the issued number is 1 (only one has been issued), remove the lock.<br />
     * If only one has been issued, and destroy is true, call this resource's destroy() method and remove from defined resource map.
     * 
     * @param rsc
     * @param destroy destroy this resource if it hasn't been issued elsewhere
     * @param force force destroy this resource - <b>VERY DANGEROUS</b>
     * @throws DestructionException
     */
    public void returnResource(Resource rsc, boolean destroy, boolean force) throws DestructionException {
        String name = rsc.getName();
        synchronized(MAP_LOCK) {
            Integer lk = this.locks.get(name);
            if (lk != null) {
                if (lk.intValue() == 1) {
                    this.locks.remove(name);
                    if (destroy) {
                        rsc.destroy(this.server);
                        this.rscs.remove(rsc);
                    }
                } else {
                    if (force) {
                        this.rscs.remove(rsc);
                        rsc.destroy(this.server);
                        this.locks.remove(name);
                    } else {
                        lk = Integer.valueOf(lk.intValue() - 1);
                        this.locks.put(name, lk);
                    }
                }
            }
        }
    }
    
}
