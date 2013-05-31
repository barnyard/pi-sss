/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * JAXB pojo to represent the S3 MetadataEntry
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MetadataEntry {
    @XmlElement(name = "Key")
    private String key;
    @XmlElement(name = "Value")
    private String value;

    public MetadataEntry() {
    }

    public MetadataEntry(String k, String v) {
        this.key = k;
        this.value = v;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
