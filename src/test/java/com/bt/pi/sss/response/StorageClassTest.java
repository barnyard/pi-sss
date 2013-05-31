package com.bt.pi.sss.response;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.sss.response.StorageClass;

public class StorageClassTest {
	@Test
	public void testValues() {
		
		// act
		StorageClass[] values = StorageClass.values();
		
		// assert
		assertEquals(2, values.length);
	}

	@Test
	public void testValueOf() {
		// act/assert
		for (StorageClass storageClass: StorageClass.values()) {
			StorageClass result = StorageClass.valueOf(storageClass.toString());
			assertEquals(result, storageClass);
		}
	}
}
