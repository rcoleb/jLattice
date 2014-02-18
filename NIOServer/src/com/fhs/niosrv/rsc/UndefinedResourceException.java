package com.fhs.niosrv.rsc;

import com.fhs.niosrv.you.impl.Resource;


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
