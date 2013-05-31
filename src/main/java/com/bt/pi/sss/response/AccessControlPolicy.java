/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.app.common.entities.CannedAclPolicy;

/**
 * JAXB pojo to represent the S3 AccessControlPolicy
 */
@XmlRootElement(name = "AccessControlPolicy")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccessControlPolicy {
    private static final String HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_AUTHENTICATED_USERS = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
    private static final String HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_ALL_USERS = "http://acs.amazonaws.com/groups/global/AllUsers";
    private static final Log LOG = LogFactory.getLog(AccessControlPolicy.class);
    @XmlElement(name = "Owner")
    private CanonicalUser owner;
    @XmlElementWrapper(name = "AccessControlList")
    @XmlElement(name = "Grant")
    private List<Grant> grantList;

    public AccessControlPolicy() {
        // for JAXB
    }

    public AccessControlPolicy(String bucketOwner, CannedAclPolicy cannedAclPolicy) {
        this(bucketOwner, cannedAclPolicy, null);
    }

    // TODO: set id and user properly in CanonicalUser
    public AccessControlPolicy(String bucketOwner, CannedAclPolicy cannedAclPolicy, String objectOwner) {
        LOG.debug(String.format("AccessControlPolicy(%s, %s, %s)", bucketOwner, cannedAclPolicy, objectOwner));
        switch (cannedAclPolicy) {
        case PRIVATE:
        case AWS_EXEC_READ:
            this.owner = new CanonicalUser(bucketOwner, bucketOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(bucketOwner, Permission.FULL_CONTROL));
            break;
        case PUBLIC_READ:
            this.owner = new CanonicalUser(bucketOwner, bucketOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(bucketOwner, Permission.FULL_CONTROL));
            this.grantList.add(new Grant(URI.create(HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_ALL_USERS), Permission.READ));
            break;
        case PUBLIC_READ_WRITE:
            this.owner = new CanonicalUser(bucketOwner, bucketOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(bucketOwner, Permission.FULL_CONTROL));
            this.grantList.add(new Grant(URI.create(HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_ALL_USERS), Permission.READ));
            this.grantList.add(new Grant(URI.create(HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_ALL_USERS), Permission.WRITE));
            break;
        case AUTHENTICATED_READ:
            this.owner = new CanonicalUser(bucketOwner, bucketOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(bucketOwner, Permission.FULL_CONTROL));
            this.grantList.add(new Grant(URI.create(HTTP_ACS_AMAZONAWS_COM_GROUPS_GLOBAL_AUTHENTICATED_USERS), Permission.READ));
            break;
        case BUCKET_OWNER_READ:
            this.owner = new CanonicalUser(objectOwner, objectOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(objectOwner, Permission.FULL_CONTROL));
            this.grantList.add(new Grant(bucketOwner, Permission.READ));
            break;
        case BUCKET_OWNER_FULL_CONTROL:
            this.owner = new CanonicalUser(objectOwner, objectOwner);
            this.grantList = new ArrayList<Grant>();
            this.grantList.add(new Grant(objectOwner, Permission.FULL_CONTROL));
            this.grantList.add(new Grant(bucketOwner, Permission.FULL_CONTROL));
            break;
        default:
            break;
        }
    }

    public CanonicalUser getOwner() {
        return this.owner;
    }

    public List<Grant> getAccessControlList() {
        return this.grantList;
    }
}
