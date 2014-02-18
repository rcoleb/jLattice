package com.fhs.jlattice.rsc;

import com.fhs.jlattice.you.impl.Resource;


/**
 * @author Ben.Cole
 *
 */
public class UndefinedResourceException extends Exception {
    /**
     * @param rscName
     */
    public UndefinedResourceException(String rscName) {
        super("Resource with name [" + rscName + "] is undefined.");
    }
    /**
     * @param rscClz
     */
    public UndefinedResourceException(Class<? extends Resource> rscClz) {
        super("Resource of class [" + rscClz.getName() + "] is undefined.");
    }
}
