/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Grantee")
@XmlSeeAlso({
    Group.class,
    User.class
}) // SeeAlso is very important! marshal doesn't work without it!
public abstract class Grantee {
}