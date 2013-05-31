package com.bt.pi.sss.response;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.sss.response.ListAllMyBucketsResult;

public class ListAllMyBucketsResultTest {

	// for JAXB
	@Test
	public void testListAllMyBucketsResult() {
		assertNotNull(new ListAllMyBucketsResult());
	}

}
