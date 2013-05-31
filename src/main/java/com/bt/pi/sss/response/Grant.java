/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;

public class Grant {
	@XmlElement(name = "Grantee")
	private Grantee grantee;
	@XmlElement(name = "Permission")
	private Permission permission;
	
	public Grant() {
		// for JAXB
	}

	public Grant(String userName, Permission aPermission) {
		this.grantee = new CanonicalUser(userName, userName);
		this.permission = aPermission;
	}

	public Grant(URI groupName, Permission aPermission) {
		this.grantee = new Group(groupName);
		this.permission = aPermission;
	}
	
	public Grantee getGrantee() {
		return grantee;
	}

	public Permission getPermission() {
		return permission;
	}
}
