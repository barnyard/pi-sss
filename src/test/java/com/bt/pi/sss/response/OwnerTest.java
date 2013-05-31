package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.sss.response.CanonicalUser;

public class OwnerTest {

	@Test
	public void testOwner() {
		assertNotNull(new CanonicalUser());
	}

	@Test
	public void testOwnerStringString() {
		// act
		CanonicalUser result = new CanonicalUser("id", "name");
		
		// assert
		assertEquals("id", result.getId());
		assertEquals("name", result.getDisplayName());
	}
}
