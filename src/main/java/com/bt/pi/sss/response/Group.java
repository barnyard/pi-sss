/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Group")
public class Group extends Grantee
{
    @XmlElement(name = "URI")
    private URI uri;

    public Group() {
	}

    public Group(URI aUri) {
		this.uri = aUri;
	}

    public URI getURI() {
        return uri;
    }
}