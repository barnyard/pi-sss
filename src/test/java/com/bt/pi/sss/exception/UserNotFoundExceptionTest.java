/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.exception;

import org.junit.Test;

import com.bt.pi.sss.exception.UserNotFoundException;

public class UserNotFoundExceptionTest {
	@Test
	public void defaultConstructorTest(){
		new UserNotFoundException();
		new UserNotFoundException("");
		new UserNotFoundException(new RuntimeException());
		new UserNotFoundException("", new RuntimeException());
	}
}
