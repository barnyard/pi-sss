package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.sss.response.Permission;

public class PermissionTest {

	@Test
	public void testValueOf() {
		for (Permission p : Permission.values()) {
			Permission result = Permission.valueOf(p.toString());
			assertEquals(p, result);
		}
	}
}
