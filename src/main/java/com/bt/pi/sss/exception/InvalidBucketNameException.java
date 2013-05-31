/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

public class InvalidBucketNameException extends BucketException {
	private static final long serialVersionUID = 1L;

	public InvalidBucketNameException(String message) {
		super(message);
	}
}
