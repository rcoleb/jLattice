package com.fhs.niosrv.run;

import com.fhs.niosrv.you.impl.Response;

/**
 * Context object for NIO selector key attachments.  Used to keep track of meta-data, such as connection details or a response
 * 
 * @author Ben
 *
 */
public class KeyAttachment {
	/** lazily-loaded (on connection) remote address; use to avoid reifying address multiple times */
	public String remoteAddr;
	/** (possibly null) response object */
	public Response<?> response;
	/** lazy-loaded byte-encoded response */
	public byte[] encodedResp;
	/** multi-write marker */
	public int writeMarker;
}
