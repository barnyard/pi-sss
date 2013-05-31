package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXB;

import org.junit.Test;

import com.bt.pi.app.common.entities.CannedAclPolicy;

public class AccessControlPolicyTest {
    private String bucketOwner = "bucketOwner";
    private String objectOwner = "objectOwner";

    @Test
    public void testAccessControlPolicy() {
        assertNotNull(new AccessControlPolicy());
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyPrivate() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.PRIVATE;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy);

        // assert
        assertEquals(bucketOwner, result.getOwner().getId());
        assertEquals(bucketOwner, result.getOwner().getDisplayName());

        assertEquals(1, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(bucketOwner, canonicalUser.getId());
        assertEquals(bucketOwner, canonicalUser.getDisplayName());
        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyPublicRead() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.PUBLIC_READ;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy);

        // assert
        assertEquals(bucketOwner, result.getOwner().getId());
        assertEquals(bucketOwner, result.getOwner().getDisplayName());

        assertEquals(2, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(bucketOwner, canonicalUser.getId());
        assertEquals(bucketOwner, canonicalUser.getDisplayName());

        assertEquals(Permission.READ, result.getAccessControlList().get(1).getPermission());
        assertTrue(result.getAccessControlList().get(1).getGrantee() instanceof Group);
        Group group = (Group) result.getAccessControlList().get(1).getGrantee();
        assertEquals("http://acs.amazonaws.com/groups/global/AllUsers", group.getURI().toString());
        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyPublicReadWrite() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.PUBLIC_READ_WRITE;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy);

        // assert
        assertEquals(bucketOwner, result.getOwner().getId());
        assertEquals(bucketOwner, result.getOwner().getDisplayName());

        assertEquals(3, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(bucketOwner, canonicalUser.getId());
        assertEquals(bucketOwner, canonicalUser.getDisplayName());

        assertEquals(Permission.READ, result.getAccessControlList().get(1).getPermission());
        assertTrue(result.getAccessControlList().get(1).getGrantee() instanceof Group);
        Group group1 = (Group) result.getAccessControlList().get(1).getGrantee();
        assertEquals("http://acs.amazonaws.com/groups/global/AllUsers", group1.getURI().toString());

        assertEquals(Permission.WRITE, result.getAccessControlList().get(2).getPermission());
        assertTrue(result.getAccessControlList().get(2).getGrantee() instanceof Group);
        Group group2 = (Group) result.getAccessControlList().get(2).getGrantee();
        assertEquals("http://acs.amazonaws.com/groups/global/AllUsers", group2.getURI().toString());

        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyAuthenticatedRead() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.AUTHENTICATED_READ;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy);

        // assert
        assertEquals(bucketOwner, result.getOwner().getId());
        assertEquals(bucketOwner, result.getOwner().getDisplayName());

        assertEquals(2, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(bucketOwner, canonicalUser.getId());
        assertEquals(bucketOwner, canonicalUser.getDisplayName());

        assertEquals(Permission.READ, result.getAccessControlList().get(1).getPermission());
        assertTrue(result.getAccessControlList().get(1).getGrantee() instanceof Group);
        Group group1 = (Group) result.getAccessControlList().get(1).getGrantee();
        assertEquals("http://acs.amazonaws.com/groups/global/AuthenticatedUsers", group1.getURI().toString());

        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyBucketOwnerRead() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.BUCKET_OWNER_READ;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy, objectOwner);

        // assert
        assertEquals(objectOwner, result.getOwner().getId());
        assertEquals(objectOwner, result.getOwner().getDisplayName());

        assertEquals(2, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser1 = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(objectOwner, canonicalUser1.getId());
        assertEquals(objectOwner, canonicalUser1.getDisplayName());

        assertEquals(Permission.READ, result.getAccessControlList().get(1).getPermission());
        assertTrue(result.getAccessControlList().get(1).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser2 = (CanonicalUser) result.getAccessControlList().get(1).getGrantee();
        assertEquals(bucketOwner, canonicalUser2.getId());
        assertEquals(bucketOwner, canonicalUser2.getDisplayName());

        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyBucketOwnerFullControl() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.BUCKET_OWNER_FULL_CONTROL;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy, objectOwner);

        // assert
        assertEquals(objectOwner, result.getOwner().getId());
        assertEquals(objectOwner, result.getOwner().getDisplayName());

        assertEquals(2, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser1 = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(objectOwner, canonicalUser1.getId());
        assertEquals(objectOwner, canonicalUser1.getDisplayName());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(1).getPermission());
        assertTrue(result.getAccessControlList().get(1).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser2 = (CanonicalUser) result.getAccessControlList().get(1).getGrantee();
        assertEquals(bucketOwner, canonicalUser2.getId());
        assertEquals(bucketOwner, canonicalUser2.getDisplayName());

        JAXB.marshal(result, System.out);
    }

    @Test
    public void testAccessControlPolicyStringCannedAclPolicyAwsExecRead() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.AWS_EXEC_READ;

        // act
        AccessControlPolicy result = new AccessControlPolicy(bucketOwner, cannedAclPolicy);

        // assert
        assertEquals(bucketOwner, result.getOwner().getId());
        assertEquals(bucketOwner, result.getOwner().getDisplayName());

        assertEquals(1, result.getAccessControlList().size());

        assertEquals(Permission.FULL_CONTROL, result.getAccessControlList().get(0).getPermission());
        assertTrue(result.getAccessControlList().get(0).getGrantee() instanceof CanonicalUser);
        CanonicalUser canonicalUser = (CanonicalUser) result.getAccessControlList().get(0).getGrantee();
        assertEquals(bucketOwner, canonicalUser.getId());
        assertEquals(bucketOwner, canonicalUser.getDisplayName());
        JAXB.marshal(result, System.out);
    }
}
