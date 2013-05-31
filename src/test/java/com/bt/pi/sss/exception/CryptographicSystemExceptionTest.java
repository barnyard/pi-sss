/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

import org.junit.Test;

import com.bt.pi.sss.exception.CryptographicSystemException;

public class CryptographicSystemExceptionTest {
	@Test
	public void defaultConstructorTest(){
		new CryptographicSystemException();
		new CryptographicSystemException("");
		new CryptographicSystemException(new RuntimeException());
		new CryptographicSystemException("", new RuntimeException());
	}
}
