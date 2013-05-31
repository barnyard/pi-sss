/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.SimpleTimeZone;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.bt.pi.sss.entities.ObjectMetaData;

/**
 * JAXB pojo to represent the S3 ListEntry
 */
/*
 * <xsd:complexType name="ListEntry"> <xsd:sequence> <xsd:element name="Key" type="xsd:string"/> <xsd:element name="LastModified" type="xsd:dateTime"/> <xsd:element name="ETag" type="xsd:string"/>
 * <xsd:element name="Size" type="xsd:long"/> <xsd:element name="Owner" type="tns:CanonicalUser" minOccurs="0"/> <xsd:element name="StorageClass" type="tns:StorageClass"/> </xsd:sequence>
 * </xsd:complexType>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ListEntry {
    @XmlElement(name = "Key")
    private String key;
    @XmlElement(name = "LastModified")
    private String lastModified;
    @XmlElement(name = "ETag")
    private String eTag;
    @XmlElement(name = "Size")
    private long size;
    @XmlElement(name = "Owner")
    private CanonicalUser owner;
    @XmlElement(name = "StorageClass")
    private StorageClass storageClass;

    public ListEntry() {
    }

    public ListEntry(ObjectMetaData objectMetaData) {
        this.key = objectMetaData.getName();
        setLastModified(objectMetaData.getLastModified());
        this.size = objectMetaData.getSize();
        this.eTag = objectMetaData.getETag();
    }

    private void setLastModified(Calendar cal) {
        if (null != cal) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(new SimpleTimeZone(0, "GMT"));
            this.lastModified = df.format(cal.getTime());
        }
    }

    public String getKey() {
        return key;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getETag() {
        return eTag;
    }

    public long getSize() {
        return size;
    }

    public CanonicalUser getOwner() {
        return owner;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }
}
