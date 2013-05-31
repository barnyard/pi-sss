package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.exception.AccessDeniedException;

@RunWith(MockitoJUnitRunner.class)
public class BucketMetaDataHelperTest {

    @InjectMocks
    BucketMetaDataHelper bucketMetaDataHelper = new BucketMetaDataHelper();
    @Mock
    DhtClientFactory dhtClientFactory;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    private BlockingDhtWriter blockingWriter;
    @Mock
    PId bucketPiId;
    @Mock
    PId userPiId;
    @Mock
    private BlockingDhtReader blockingReader;

    @Mock
    private Regions regions;

    @Mock
    private PId regionsId;

    @Mock
    private BlockingDhtCache blockingDhtCache;

    private final String defaultLocation = "DEFAULT_LOCATION";

    private static final String userName = "UserA";
    private static final String bucketName = "BucketA";

    @Before
    public void setUp() throws Exception {

        bucketMetaDataHelper.setDhtClientFactory(dhtClientFactory);
        bucketMetaDataHelper.setPiIdBuilder(piIdBuilder);

        this.bucketMetaDataHelper.setDefaultBucketLocation(defaultLocation);

        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingWriter);
        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingReader);
        when(piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())))).thenReturn(bucketPiId);
        when(piIdBuilder.getPId(isA(BucketMetaData.class))).thenReturn(bucketPiId);
        when(piIdBuilder.getPId(User.getUrl(userName))).thenReturn(userPiId);
        when(piIdBuilder.getRegionsId()).thenReturn(regionsId);
        when(blockingDhtCache.get(regionsId)).thenReturn(regions);

    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetUsernameOnCreatingANewBucket() {

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                BucketMetaData updatedBucket = (BucketMetaData) resolver.update(null, null);
                assertEquals(bucketName, updatedBucket.getName());
                assertEquals(userName, updatedBucket.getUsername());

                return null;
            }
        }).when(blockingWriter).update(eq(bucketPiId), (PiEntity) anyObject(), isA(UpdateResolver.class));

        // act
        BucketMetaData bucketMetaData = bucketMetaDataHelper.createBucketMetaData(userName, bucketName, null, CannedAclPolicy.PRIVATE);
        bucketMetaDataHelper.addBucket(bucketMetaData);

        // assert
        verify(blockingDhtCache).update(eq(bucketPiId), isA(UpdateResolver.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetUsernameOnCreateBucketIfBucketExistsAndDeleted() {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                BucketMetaData existingBucket = new BucketMetaData(bucketName);
                existingBucket.setDeleted(true);

                BucketMetaData updatedBucket = (BucketMetaData) resolver.update(existingBucket, null);
                assertEquals(bucketName, updatedBucket.getName());
                assertEquals(userName, updatedBucket.getUsername());

                return null;
            }
        }).when(blockingWriter).update(eq(bucketPiId), (PiEntity) anyObject(), isA(UpdateResolver.class));

        // act
        BucketMetaData bucketMetaData = bucketMetaDataHelper.createBucketMetaData(userName, bucketName, null, CannedAclPolicy.PRIVATE);
        bucketMetaDataHelper.addBucket(bucketMetaData);

        // assert
        verify(blockingDhtCache).update(eq(bucketPiId), isA(UpdateResolver.class));
    }

    @Test
    public void shouldReturnFalseIfBucketDoesntExist() {
        // act
        boolean existsForUser = this.bucketMetaDataHelper.bucketAlreadyExistsForUser(bucketName, userName);

        // assert
        assertFalse(existsForUser);
    }

    @Test
    public void shouldReturnTrueIfBucketExistsForUser() {
        // setup
        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername(userName);

        when(blockingDhtCache.get(bucketPiId)).thenReturn(existingBucket);

        // act
        boolean existsForUser = this.bucketMetaDataHelper.bucketAlreadyExistsForUser(bucketName, userName);

        // assert
        assertTrue(existsForUser);
    }

    @Test
    public void shouldReturnFalseIfBucketExistsForADifferentUser() {
        // setup
        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername("UserB");

        when(blockingReader.get(bucketPiId)).thenReturn(existingBucket);

        // act
        boolean existsForUser = this.bucketMetaDataHelper.bucketAlreadyExistsForUser(bucketName, userName);

        // assert
        assertFalse(existsForUser);
    }

    @Test
    public void shouldAllowWriteAccessIfUserOwnsTheBucket() {
        // setup
        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername(userName);

        User existingUser = new User();
        existingUser.setUsername(userName);
        existingUser.getBucketNames().add(bucketName);

        when(blockingReader.get(userPiId)).thenReturn(existingUser);
        when(blockingReader.get(bucketPiId)).thenReturn(existingBucket);

        // act
        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);
    }

    @Test
    public void shouldAllowWriteAccessIfUserDontOwnTheBucketButBucketHasPublicReadWritePolicy() {
        // setup
        User existingUser = new User();
        existingUser.setUsername(userName);
        when(blockingReader.get(userPiId)).thenReturn(existingUser);

        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername(userName);
        existingBucket.setCannedAccessPolicy(CannedAclPolicy.PUBLIC_READ_WRITE);
        when(blockingDhtCache.get(bucketPiId)).thenReturn(existingBucket);

        // act
        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);

        // assert
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldNotAllowWriteAccessIfUserDontOwnTheBucketAndBucketHasPrivateReadWritePolicy() {
        // setup
        User existingUser = new User();
        existingUser.setUsername(userName);
        when(blockingReader.get(userPiId)).thenReturn(existingUser);

        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername("anotherUser");
        existingBucket.setCannedAccessPolicy(CannedAclPolicy.PRIVATE);
        when(blockingDhtCache.get(bucketPiId)).thenReturn(existingBucket);

        // act
        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);

        // assert
    }

    @Test
    public void shouldAllowWriteAccessIfUserDontOwnTheBucketButBucketMetaDataHasUser() {
        // setup
        User existingUser = new User();
        existingUser.setUsername(userName);
        when(blockingReader.get(userPiId)).thenReturn(existingUser);

        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setUsername(userName);
        when(blockingDhtCache.get(bucketPiId)).thenReturn(existingBucket);

        // act
        this.bucketMetaDataHelper.checkBucketWriteAccess(userName, bucketName);

    }

    @Test(expected = AccessDeniedException.class)
    public void shouldNotAllowReadAccessIfUserDontOwnBucketAndBucketHasPrivateReadWritePolicy() {
        // setup
        User existingUser = new User();
        existingUser.setUsername(userName);
        when(blockingReader.get(userPiId)).thenReturn(existingUser);

        BucketMetaData existingBucket = new BucketMetaData(bucketName);
        existingBucket.setCannedAccessPolicy(CannedAclPolicy.PRIVATE);
        when(blockingDhtCache.get(bucketPiId)).thenReturn(existingBucket);

        // act
        this.bucketMetaDataHelper.checkBucketReadAccess(userName, bucketName);
    }

    @Test
    public void shouldCreateBucketMetaDataWithDefaultLocationIfNoneIsGiven() {
        // setup

        // act

        BucketMetaData bucketMetaData = bucketMetaDataHelper.createBucketMetaData(userName, bucketName, null, CannedAclPolicy.PRIVATE);

        // assert
        assertEquals(defaultLocation, bucketMetaData.getLocation());
    }

    @Test
    public void shouldCreateBucketMetaDataWithGivenLocation() {
        // setup
        String bucketLocation = "BUCKET_LOCATION";
        // act

        BucketMetaData bucketMetaData = bucketMetaDataHelper.createBucketMetaData(userName, bucketName, bucketLocation, CannedAclPolicy.PRIVATE);

        // assert
        assertEquals(bucketLocation, bucketMetaData.getLocation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotUseDefaultBucketLocationConstantIfDefaultBucketLocationFieldIsSet() {
        // setup
        String defaultBucketLocation = "BUCKET_LOCATION";
        bucketMetaDataHelper.setDefaultBucketLocation(defaultBucketLocation);
        Map<String, Region> mockRegionsMap = mock(Map.class);
        when(regions.getRegions()).thenReturn(mockRegionsMap);
        when(mockRegionsMap.containsKey(defaultBucketLocation)).thenReturn(true);
        // act
        bucketMetaDataHelper.validateRegion(null);
        // assert
        verify(mockRegionsMap).containsKey(defaultBucketLocation);

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfUserExceedsMaximumNumberOfBucketsPerUser() {
        // setup
        User user = new User();
        for (int i = 0; i <= 5; i++) {
            user.getBucketNames().add("bucket" + i);
        }

        bucketMetaDataHelper.setMaxBucketsPerUser(5);

        // act
        bucketMetaDataHelper.checkUserBucketsCount(user);
    }
}
