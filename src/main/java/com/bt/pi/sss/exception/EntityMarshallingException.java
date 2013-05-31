/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

public class EntityMarshallingException extends  RuntimeException {

	private static final long serialVersionUID = 4598055759488126158L;

	public EntityMarshallingException(String message, Throwable t) {
		super(message, t);
	}

}
