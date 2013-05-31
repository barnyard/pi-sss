/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

public class CryptographicSystemException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public CryptographicSystemException() {}
	public CryptographicSystemException(String message){
		super(message);
	}
	public CryptographicSystemException(Throwable cause){
		super(cause);
	}
	public CryptographicSystemException(String message, Throwable cause){
		super(message,cause);
	}
}
