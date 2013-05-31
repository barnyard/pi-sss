/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

public abstract class BucketException extends RuntimeException {
	private static final long serialVersionUID = 8222679690343918053L;

	public BucketException() {
		super();
	}
	
	public BucketException(String message) {
		super(message);
	}
}
