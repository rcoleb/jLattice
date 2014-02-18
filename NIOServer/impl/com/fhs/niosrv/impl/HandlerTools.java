package com.fhs.niosrv.impl;

import java.nio.charset.Charset;

import com.fhs.jlattice.Message;

public final class HandlerTools {
	/**
	 * Parse message read-bytes into a String, using UTF-8 encoding
	 *
	 * @param msg
	 * @return
	 */
	public static String parseMessage(Message msg) {
		String v = new String(msg.message.array(), Charset.forName("UTF-8"));
		return v;
	}
	/**
	 * Parse message read-bytes into a String, using the provided charset
	 *
	 * @param msg
	 * @param charset
	 * @return
	 */
	public static String parseMessage(Message msg, Charset charset) {
		String v = new String(msg.message.array(), charset);
		return v;
	}
	
}
