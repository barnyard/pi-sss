/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * JAXB pojo to represent an S3 Canonical User
 */
@XmlType(name = "CanonicalUser")
public class CanonicalUser extends User {
    @XmlElement(name = "ID")
    private String id;
    @XmlElement(name = "DisplayName")
    private String displayName;

    public CanonicalUser() {
        // needed for JAXB
    }

    public CanonicalUser(String anId, String aDisplayName) {
        this.id = anId;
        this.displayName = aDisplayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
