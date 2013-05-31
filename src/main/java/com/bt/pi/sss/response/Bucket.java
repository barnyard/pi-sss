/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import javax.xml.bind.annotation.XmlElement;

import com.bt.pi.app.common.entities.BucketMetaData;

/**
 * JAXB pojo to represent an S3 bucket
 */
public class Bucket {
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "CreationDate")
    private String creationDate;

    public Bucket() {
        // needed for JAXB
    }

    public Bucket(BucketMetaData bucketMetaData) {
        this.name = bucketMetaData.getName();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        this.creationDate = df.format(bucketMetaData.getCreationDate().getTime());
    }

    public String getName() {
        return name;
    }

    public String getCreationDate() {
        return creationDate;
    }
}
