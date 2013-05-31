package com.bt.pi.sss.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.sss.exception.BucketObjectReadException;

public class BucketObjectReadExceptionTest {

	@Test
	public void testBucketObjectReadException() {
		String message = "oh!";
		assertNotNull(new BucketObjectReadException(message));
	}

}
