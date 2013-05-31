package com.bt.pi.sss.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.sss.BucketManager;
import com.bt.pi.sss.client.PisssClient.BucketAccess;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BucketObjectNotFoundException;
import com.bt.pi.sss.exception.NoSuchBucketException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(IOUtils.class)
@PowerMockIgnore({ "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class PisssClientTest {
    @InjectMocks
    private PisssClient pisssClient = new PisssClient();
    @Mock
    private BucketManager bucketManager;
    private String bucketName = "test";
    private String userName = "fred";
    @Mock
    private User user;
    @Mock
    private ObjectMetaData objectMetaData;
    private String objectName = "manifest.xml";
    private String xml = "<manifest/>";

    @Before
    public void setUp() throws Exception {
        when(this.user.getUsername()).thenReturn(this.userName);
        when(this.bucketManager.readObject(eq(userName), eq(bucketName), eq(objectName))).thenReturn(objectMetaData);
        when(objectMetaData.getInputStream()).thenReturn(new ByteArrayInputStream(xml.getBytes()));
    }

    @Test
    public void testGetObjectFromBucket() throws Exception {
        // setup

        // act
        String result = this.pisssClient.getObjectFromBucket(bucketName, objectName, user);

        // assert
        assertEquals(xml, result);
    }

    @Test
    public void testGetFileFromBucket() throws Exception {
        // setup
        File target = File.createTempFile("unittesting", null);
        target.deleteOnExit();

        // act
        File result = this.pisssClient.getFileFromBucket(bucketName, objectName, user, target.getAbsolutePath());

        // assert
        assertEquals(xml, FileUtils.readFileToString(result));
    }

    @Test(expected = NotAuthorizedException.class)
    public void testGetObjectFromBucketServiceReturns401() throws Exception {
        // setup
        AccessDeniedException accessDeniedException = new AccessDeniedException();
        when(this.bucketManager.readObject(eq(userName), eq(bucketName), eq(objectName))).thenThrow(accessDeniedException);

        // act
        this.pisssClient.getObjectFromBucket(bucketName, objectName, user);
    }

    @Test(expected = PisssClientException.class)
    public void testGetObjectFromBucketServiceThrowsIOException() throws Exception {
        // setup
        PowerMockito.mockStatic(IOUtils.class);
        when(IOUtils.toString(isA(InputStream.class))).thenThrow(new IOException());

        // act
        this.pisssClient.getObjectFromBucket(bucketName, objectName, user);
    }

    @Test(expected = PisssClientException.class)
    public void testGetFileFromBucketServiceThrowsIOException() throws Exception {
        // setup
        File target = File.createTempFile("unittesting", null);
        target.deleteOnExit();
        PowerMockito.mockStatic(IOUtils.class);
        when(IOUtils.copyLarge(isA(InputStream.class), isA(FileOutputStream.class))).thenThrow(new IOException());

        // act
        this.pisssClient.getFileFromBucket(bucketName, objectName, user, target.getAbsolutePath());
    }

    @Test(expected = NoSuchBucketException.class)
    public void testGetObjectFromBucketServiceReturnsNoSuchBucket() throws Exception {
        // setup
        NoSuchBucketException noSuchBucketException = new NoSuchBucketException();
        when(this.bucketManager.readObject(eq(userName), eq(bucketName), eq(objectName))).thenThrow(noSuchBucketException);

        // act
        this.pisssClient.getObjectFromBucket(bucketName, objectName, user);
    }

    @Test(expected = BucketObjectNotFoundException.class)
    public void testGetObjectFromBucketServiceReturnsNoSuchObject() throws Exception {
        // setup
        BucketObjectNotFoundException bucketObjectNotFoundException = new BucketObjectNotFoundException();
        when(this.bucketManager.readObject(eq(userName), eq(bucketName), eq(objectName))).thenThrow(bucketObjectNotFoundException);

        // act
        this.pisssClient.getObjectFromBucket(bucketName, objectName, user);
    }

    @Test
    public void testGetBucketAccessPublicRead() throws Exception {
        // setup
        when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(CannedAclPolicy.PUBLIC_READ);

        // act
        BucketAccess result = this.pisssClient.getBucketAccess(bucketName, user);

        // assert
        assertEquals(BucketAccess.PUBLIC, result);
    }

    @Test
    public void testGetBucketAccessUPublicReadAndWrite() throws Exception {
        // setup
        when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(CannedAclPolicy.PUBLIC_READ_WRITE);

        // act
        BucketAccess result = this.pisssClient.getBucketAccess(bucketName, user);

        // assert
        assertEquals(BucketAccess.PUBLIC, result);
    }

    @Test
    public void testGetBucketAccessPrivate() throws Exception {
        // setup
        when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(CannedAclPolicy.PRIVATE);

        // act
        BucketAccess result = this.pisssClient.getBucketAccess(bucketName, user);

        // assert
        assertEquals(BucketAccess.PRIVATE, result);
    }

    @Test
    public void testGetBucketAccessOwnerFullControl() throws Exception {
        // setup
        when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(CannedAclPolicy.BUCKET_OWNER_FULL_CONTROL);

        // act
        BucketAccess result = this.pisssClient.getBucketAccess(bucketName, user);

        // assert
        assertEquals(BucketAccess.PRIVATE, result);
    }

    @Test
    public void testGetBucketAccessAwsExecRead() throws Exception {
        // setup
        when(this.bucketManager.getCannedAclPolicy(userName, bucketName)).thenReturn(CannedAclPolicy.AWS_EXEC_READ);

        // act
        BucketAccess result = this.pisssClient.getBucketAccess(bucketName, user);

        // assert
        assertEquals(BucketAccess.PRIVATE, result);
    }
}
