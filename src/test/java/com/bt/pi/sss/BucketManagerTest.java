package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.CannedAclPolicy;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.AccessDeniedException;
import com.bt.pi.sss.exception.BucketAlreadyExistsException;
import com.bt.pi.sss.exception.BucketAlreadyOwnedByUserException;
import com.bt.pi.sss.exception.BucketNotEmptyException;
import com.bt.pi.sss.exception.BucketObjectCreationException;
import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.exception.NoSuchBucketException;
import com.bt.pi.sss.filter.AuthenticationFilter;
import com.bt.pi.sss.util.FileSystemBucketUtils;
import com.bt.pi.sss.util.NameValidator;

public class BucketManagerTest {
    private BucketManager bucketManager;
    private String storageDirectory = System.getProperty("java.io.tmpdir");
    private String userName = "testUser";
    private String bucketName = "testBucket";
    private String objectName1 = "testObject1";
    private String objectName2 = "testObject2";
    private List<BucketMetaData> bucketList;
    private String data = "this is a test file";
    private InputStream inputStream;
    private PiIdBuilder piIdBuilder;
    private DhtClientFactory dhtClientFactory;
    private User user;
    private User anotherUser;
    private PId userId;
    private PId anotherUserId;
    private BlockingDhtReader reader;

    private BlockingDhtCache cache;
    private Regions regions;
    private PId bucketId, bucketId2;
    private String anotherBucketName = "anotherBucketName";
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;
    private String anotherUserName = "anotherTestUser";

    @Before
    public void setUp() throws Exception {
        this.bucketManager = new BucketManager();
        this.bucketManager.setNameValidator(mock(NameValidator.class));

        BucketMetaDataHelper aBucketMetaDataHelper = new BucketMetaDataHelper();
        aBucketMetaDataHelper.setMaxBucketsPerUser(5);

        this.bucketManager.setBucketMetaDataHelper(aBucketMetaDataHelper);
        this.piIdBuilder = mock(PiIdBuilder.class);
        aBucketMetaDataHelper.setPiIdBuilder(this.piIdBuilder);

        this.dhtClientFactory = mock(DhtClientFactory.class);
        aBucketMetaDataHelper.setDhtClientFactory(this.dhtClientFactory);
        FileSystemBucketUtils utils = new FileSystemBucketUtils();
        utils.setBucketRootDirectory(storageDirectory);
        this.bucketManager.setFileSystemBucketUtils(utils);

        deleteBucket();

        bucketList = new ArrayList<BucketMetaData>();
        inputStream = new ByteArrayInputStream(data.getBytes());
        user = new User(userName, null, null);
        anotherUser = new User(anotherUserName, null, null);
        userId = mock(PId.class);
        anotherUserId = mock(PId.class);

        reader = mock(BlockingDhtReader.class);
        when(this.dhtClientFactory.createBlockingReader()).thenReturn(reader);
        when(this.piIdBuilder.getPId(User.getUrl(userName))).thenReturn(userId);
        when(this.piIdBuilder.getPId(User.getUrl(anotherUserName))).thenReturn(anotherUserId);

        cache = mock(BlockingDhtCache.class);
        regions = new Regions();
        regions.addRegion(new Region("TEST_REGION", 1, "", ""));
        regions.addRegion(new Region("UK", 2, "", ""));
        PId dummyId = mock(PId.class);
        when(dummyId.getIdAsHex()).thenReturn(Integer.toHexString(1111));
        aBucketMetaDataHelper.setBlockingDhtCache(cache);
        when(piIdBuilder.getRegionsId()).thenReturn(dummyId);
        when(cache.get(dummyId)).thenReturn(regions);

        bucketId = mock(PId.class);
        bucketId2 = mock(PId.class);

        when(this.piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())))).thenReturn(bucketId);
        when(this.piIdBuilder.getPId(isA(BucketMetaData.class))).thenReturn(bucketId);
        when(piIdBuilder.getPId(eq(BucketMetaData.getUrl(anotherBucketName.toLowerCase(Locale.getDefault()))))).thenReturn(bucketId2);
        when(reader.get(userId)).thenReturn(user);
        when(reader.get(anotherUserId)).thenReturn(anotherUser);

        scatterGatherContinuationRunner = new ScatterGatherContinuationRunner();
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
        scatterGatherContinuationRunner.setScheduledExecutorService(scheduledExecutorService);

    }

    @After
    public void after() throws Exception {
        deleteBucket();
    }

    private void deleteBucket() throws Exception {
        File directory = new File(String.format("%s/%s", storageDirectory, bucketName));
        FileUtils.deleteDirectory(directory);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBucket() {
        // setup
        when(reader.get(userId)).thenReturn(user);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                Object update = resolver.update(null, null);
                assertTrue(update instanceof BucketMetaData);
                BucketMetaData bucketMetaData = (BucketMetaData) update;
                assertEquals(CannedAclPolicy.PRIVATE, bucketMetaData.getCannedAclPolicy());
                assertEquals(bucketName, bucketMetaData.getName());
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);

        // assert
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).isDirectory());
        assertTrue(user.getBucketNames().contains(bucketName));
        verify(cache).update(eq(bucketId), isA(UpdateResolver.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBucketAlreadyDeletedInDht() {
        // setup
        when(reader.get(userId)).thenReturn(user);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        bucketMetaData.setDeleted(true);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);

        // assert
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).isDirectory());
        assertTrue(user.getBucketNames().contains(bucketName));
        verify(cache).update(eq(bucketId), isA(UpdateResolver.class));
        assertFalse(bucketMetaData.isDeleted());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBucketWithValidRegion() {
        // setup
        when(reader.get(userId)).thenReturn(user);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                Object update = resolver.update(null, null);
                assertTrue(update instanceof BucketMetaData);
                BucketMetaData bucketMetaData = (BucketMetaData) update;
                assertEquals(CannedAclPolicy.PRIVATE, bucketMetaData.getCannedAclPolicy());
                assertEquals(bucketName, bucketMetaData.getName());
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, "TEST_REGION");

        // assert
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).isDirectory());
        assertTrue(user.getBucketNames().contains(bucketName));
        verify(cache).update(eq(bucketId), isA(UpdateResolver.class));

    }

    @SuppressWarnings("unchecked")
    @Test(expected = InvalidArgumentException.class)
    public void testCreateBucketWithInvalidRegion() {
        // setup
        when(reader.get(userId)).thenReturn(user);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                Object update = resolver.update(null, null);
                assertTrue(update instanceof BucketMetaData);
                BucketMetaData bucketMetaData = (BucketMetaData) update;
                assertEquals(CannedAclPolicy.PRIVATE, bucketMetaData.getCannedAclPolicy());
                assertEquals(bucketName, bucketMetaData.getName());
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, "INVALID_TEST_REGION");

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateAnotherBucket() {
        // setup
        user.getBucketNames().add("existingBucket");
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                Object update = resolver.update(null, null);
                assertTrue(update instanceof BucketMetaData);
                BucketMetaData bucketMetaData = (BucketMetaData) update;
                assertEquals(CannedAclPolicy.PRIVATE, bucketMetaData.getCannedAclPolicy());
                assertEquals(bucketName, bucketMetaData.getName());
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);

        // assert
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).isDirectory());
        assertTrue(user.getBucketNames().contains(bucketName));
        assertTrue(user.getBucketNames().contains("existingBucket"));
        verify(cache).update(eq(bucketId), isA(UpdateResolver.class));
    }

    @Test(expected = BucketAlreadyExistsException.class)
    public void testCreateBucketAlreadyExistsOnFileSystem() {
        // setup
        createBucketDir(bucketName);

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);
    }

    @Test
    public void shouldAddBucketToUserIfItAlreadyExistsInDhtButNotInUserRecord() {
        // setup
        BucketMetaDataHelper mockedBucketMetaDataHelper = mock(BucketMetaDataHelper.class);
        this.bucketManager.setBucketMetaDataHelper(mockedBucketMetaDataHelper);

        when(mockedBucketMetaDataHelper.bucketAlreadyExistsForUser(bucketName, userName)).thenReturn(true);
        BucketMetaData bucketMetaData = mock(BucketMetaData.class);
        when(mockedBucketMetaDataHelper.createBucketMetaData(userName, bucketName, null, null)).thenReturn(bucketMetaData);
        when(mockedBucketMetaDataHelper.getUser(userName)).thenReturn(user);
        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);

        // assert
        verify(mockedBucketMetaDataHelper, never()).addBucket(bucketMetaData);
        verify(mockedBucketMetaDataHelper).addBucketToUser(userName, bucketName, null);
    }

    @Test(expected = BucketAlreadyExistsException.class)
    public void shouldThrowBucketAlreadyExistsExceptionIfAnotherUserHasTheBucket() {
        // setup
        BucketMetaDataHelper mockedBucketMetaDataHelper = mock(BucketMetaDataHelper.class);
        this.bucketManager.setBucketMetaDataHelper(mockedBucketMetaDataHelper);
        when(mockedBucketMetaDataHelper.getUser(userName)).thenReturn(user);

        FileSystemBucketUtils mockedFileSystemBucketUtils = mock(FileSystemBucketUtils.class);
        this.bucketManager.setFileSystemBucketUtils(mockedFileSystemBucketUtils);

        when(mockedBucketMetaDataHelper.userOwnsBucket(userName, bucketName)).thenReturn(false);
        doThrow(new BucketAlreadyExistsException()).when(mockedFileSystemBucketUtils).create(isA(String.class));

        // act - trying to create the same bucket with another user
        this.bucketManager.createBucket(userName, bucketName, null, null);

        // assert
        verify(mockedBucketMetaDataHelper, never()).addBucketToUser(userName, bucketName, null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BucketAlreadyExistsException.class)
    public void testCreateBucketAlreadyExistsinDht() {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver continuation = (UpdateResolver) invocation.getArguments()[1];
                continuation.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);
    }

    @Test(expected = BucketAlreadyOwnedByUserException.class)
    public void testCreateBucketAlreadyOwnedByUser() {
        // setup
        user.getBucketNames().add(bucketName);

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBucketAlreadyExistsAndOwnedByUserButNewCannedAcl() {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PRIVATE);
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver<BucketMetaData> callback = (UpdateResolver<BucketMetaData>) invocation.getArguments()[1];
                callback.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.createBucket(userName, bucketName, CannedAclPolicy.PUBLIC_READ, null);

        // assert
        assertEquals(CannedAclPolicy.PUBLIC_READ, bucketMetaData.getCannedAclPolicy());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBucketAlreadyExistsButNotOwnedByUserButNewCannedAcl() {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver<BucketMetaData> callback = (UpdateResolver<BucketMetaData>) invocation.getArguments()[1];
                callback.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        when(cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.createBucket(anotherUserName, bucketName, CannedAclPolicy.PRIVATE, null);

        // assert
        assertEquals(CannedAclPolicy.PRIVATE, bucketMetaData.getCannedAclPolicy());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteBucket() {
        // setup
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.deleteBucket(userName, bucketName);

        // assert
        assertFalse(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertEquals(0, user.getBucketNames().size());
        assertTrue(bucketMetaData.isDeleted());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteOneBucketOutOfTwo() {
        // setup
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);
        user.getBucketNames().add("SecondBucketName");
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        // act
        this.bucketManager.deleteBucket(userName, bucketName);

        // assert
        assertFalse(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertEquals(1, user.getBucketNames().size());
        assertTrue(user.getBucketNames().contains("SecondBucketName"));
        assertTrue(bucketMetaData.isDeleted());
    }

    @Test(expected = BucketNotEmptyException.class)
    public void testDeleteBucketNotEmpty() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/testFile", storageDirectory, bucketName)), "the cat sat on the mat");
        user.getBucketNames().add(bucketName);

        // act
        try {
            this.bucketManager.deleteBucket(userName, bucketName);
        } catch (BucketNotEmptyException e) {
            // assert
            assertTrue(new File(String.format("%s/%s/testFile", storageDirectory, bucketName)).exists());
            assertEquals(1, user.getBucketNames().size());
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteBucketNotOwner() throws Exception {
        // setup
        createBucketDir(bucketName);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        try {
            this.bucketManager.deleteBucket(userName, bucketName);
        } catch (AccessDeniedException e) {
            // assert
            assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteBucketIfItDoesntExistInUserEntityButExistsAndNotDeletedInDht() {
        // setup
        createBucketDir(bucketName);

        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        bucketMetaData.setUsername(userName);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(user, user);
                return null;
            }
        }).when(cache).update(eq(userId), isA(UpdateResolver.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[1];
                resolver.update(bucketMetaData, bucketMetaData);
                return null;
            }
        }).when(cache).update(eq(bucketId), isA(UpdateResolver.class));

        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.deleteBucket(userName, bucketName);

        // assert
        assertFalse(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
        assertEquals(0, user.getBucketNames().size());
        assertTrue(bucketMetaData.isDeleted());
    }

    @Test(expected = NoSuchBucketException.class)
    public void testDeleteBucketNotFound() throws Exception {
        // setup

        // act
        this.bucketManager.deleteBucket(userName, bucketName);
    }

    @Test(expected = NoSuchBucketException.class)
    public void testGetListOfFilesInBucketDeletedInDht() throws Exception {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        bucketMetaData.setDeleted(true);

        when(this.reader.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.getListOfFilesInBucket(userName, bucketName);
    }

    @Test
    public void testGetListOfFilesInBucket() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        createFilePair(objectName2);
        user.getBucketNames().add(bucketName);

        // act
        SortedSet<ObjectMetaData> result = this.bucketManager.getListOfFilesInBucket(userName, bucketName);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(ObjectMetaData.fromName(objectName1)));
        assertTrue(result.contains(ObjectMetaData.fromName(objectName2)));
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetListOfFilesInBucketNotOwner() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/testFile", storageDirectory, bucketName)), "the cat sat on the mat");
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.getListOfFilesInBucket(userName, bucketName);
    }

    @Test(expected = NoSuchBucketException.class)
    public void testGetListOfFilesInBucketNotFound() throws Exception {
        // setup

        // act
        this.bucketManager.getListOfFilesInBucket(userName, bucketName);
    }

    @Test
    public void testGetListOfFilesInBucketNotOwnerButHasPublicReadAccess() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        createFilePair(objectName2);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        SortedSet<ObjectMetaData> result = this.bucketManager.getListOfFilesInBucket("fred", bucketName);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(ObjectMetaData.fromName(objectName1)));
        assertTrue(result.contains(ObjectMetaData.fromName(objectName2)));
    }

    @Test
    public void testGetListOfFilesInBucketNotOwnerButHasPublicReadWriteAccess() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        createFilePair(objectName2);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        SortedSet<ObjectMetaData> result = this.bucketManager.getListOfFilesInBucket("fred", bucketName);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(ObjectMetaData.fromName(objectName1)));
        assertTrue(result.contains(ObjectMetaData.fromName(objectName2)));
    }

    @Test
    public void testGetListOfFilesInBucketNotOwnerButHasAuthenticatedReadAccess() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        createFilePair(objectName2);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        SortedSet<ObjectMetaData> result = this.bucketManager.getListOfFilesInBucket("fred", bucketName);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(ObjectMetaData.fromName(objectName1)));
        assertTrue(result.contains(ObjectMetaData.fromName(objectName2)));
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetListOfFilesInBucketAnonymousAuthenticatedReadAccess() throws Exception {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.getListOfFilesInBucket(AuthenticationFilter.ANONYMOUS_USER, bucketName);
    }

    @Test
    public void testStoreObject() throws Exception {
        // setup
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);

        // act
        this.bucketManager.storeObject(userName, bucketName, objectName1, inputStream, null, null, null, null);

        // assert
        String fileContents = FileUtils.readFileToString(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)));
        assertEquals(data, fileContents);
    }

    @Test
    public void testStoreObjectOverwrites() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), "the cat sat on the mat");
        user.getBucketNames().add(bucketName);

        // act
        this.bucketManager.storeObject(userName, bucketName, objectName1, inputStream, null, null, null, null);

        // assert
        String fileContents = FileUtils.readFileToString(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)));
        assertEquals(data, fileContents);
    }

    @Test(expected = BucketObjectCreationException.class)
    public void testStoreObjectExceptionWritingFile() {
        // setup
        user.getBucketNames().add(bucketName);

        // act
        try {
            this.bucketManager.storeObject(userName, bucketName, objectName1, inputStream, null, null, null, null);
        } catch (BucketObjectCreationException e) {
            // assert
            assertTrue(e.getMessage().contains("unable to overwrite old version of file"));
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testStoreObjectBucketNotOwnedByUser() {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.storeObject(userName, bucketName, objectName1, inputStream, null, null, null, null);
    }

    @Test(expected = NoSuchBucketException.class)
    public void testStoreObjectBucketNotFound() {
        // setup

        // act
        this.bucketManager.storeObject(userName, bucketName, objectName1, inputStream, null, null, null, null);
    }

    @Test(expected = InvalidArgumentException.class)
    public void testStoreObjectBadObjectName() {
        // setup
        createBucketDir(bucketName);
        bucketList.add(new BucketMetaData(bucketName));

        NameValidator validator = mock(NameValidator.class);
        this.bucketManager.setNameValidator(validator);

        Mockito.doThrow(new InvalidArgumentException("blah")).when(validator).validateObjectName(Matchers.isA(String.class));

        // act
        this.bucketManager.storeObject(userName, bucketName, objectName1 + ".metadata.json", inputStream, null, null, null, null);
    }

    @Test(expected = AccessDeniedException.class)
    public void testStoreObjectUserNotBucketOwnerButAndBucketHasPublicRead() throws Exception {
        // setup
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);
        createBucketDir(bucketName);

        // act
        try {
            this.bucketManager.storeObject("fred", bucketName, objectName1, inputStream, null, null, null, null);
        } catch (AccessDeniedException e) {
            // assert
            assertFalse(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
            throw e;
        }
    }

    @Test
    public void testStoreObjectUserNotBucketOwnerAndBucketHasPublicReadWrite() throws Exception {
        // setup
        createBucketDir(bucketName);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.storeObject("fred", bucketName, objectName1, inputStream, null, null, null, null);

        // assert
        String fileContents = FileUtils.readFileToString(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)));
        assertEquals(data, fileContents);
    }

    @Test
    public void testReadObject() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        user.getBucketNames().add(bucketName);

        // act
        ObjectMetaData result = this.bucketManager.readObject(userName, bucketName, objectName1);

        // assert
        assertResultData(result);
    }

    @Test(expected = NoSuchBucketException.class)
    public void testReadObjectBucketNotFound() throws Exception {
        // setup

        // act
        this.bucketManager.readObject(userName, bucketName, objectName1);
    }

    @Test(expected = AccessDeniedException.class)
    public void testReadObjectNotOwner() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PRIVATE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.readObject("fred", bucketName, objectName1);
    }

    private void createBucketDir(String bucketName) {
        new File(String.format("%s/%s", storageDirectory, bucketName)).mkdir();
    }

    @Test
    public void testReadObjectNotOwnerButBucketHasPublicRead() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        ObjectMetaData result = this.bucketManager.readObject("fred", bucketName, objectName1);

        // assert
        assertResultData(result);
    }

    private void createFilePair(String objectName) throws Exception {
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName)), data);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", storageDirectory, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{}");
    }

    private void createFilePair() throws Exception {
        createFilePair(objectName1);
    }

    @Test
    public void testReadObjectNotOwnerButBucketHasPublicReadWrite() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        ObjectMetaData result = this.bucketManager.readObject("fred", bucketName, objectName1);

        // assert
        assertResultData(result);
    }

    @Test
    public void testReadObjectNotOwnerButBucketHasAuthenticatedRead() throws Exception {
        // setup
        createBucketDir(bucketName);
        createFilePair();
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        ObjectMetaData result = this.bucketManager.readObject("fred", bucketName, objectName1);

        // assert
        assertResultData(result);
    }

    @SuppressWarnings("unchecked")
    private void assertResultData(ObjectMetaData objectMetaData) throws Exception {
        List<String> readLines = IOUtils.readLines(objectMetaData.getInputStream());
        assertEquals(1, readLines.size());
        assertEquals(data, readLines.get(0));
    }

    @Test(expected = AccessDeniedException.class)
    public void testReadObjectNotOwnerAndIsAnonymous() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.readObject(AuthenticationFilter.ANONYMOUS_USER, bucketName, objectName1);
    }

    @Test
    public void testDeleteObject() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        user.getBucketNames().add(bucketName);

        // act
        this.bucketManager.deleteObject(userName, bucketName, objectName1);

        // assert
        assertFalse(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
    }

    // note that Amazon says no error is thrown in this case
    @Test
    public void testDeleteObjectNotFound() throws Exception {
        // setup
        createBucketDir(bucketName);
        user.getBucketNames().add(bucketName);

        // act
        this.bucketManager.deleteObject(userName, bucketName, objectName1);

        // assert
        assertTrue(new File(String.format("%s/%s", storageDirectory, bucketName)).exists());
    }

    @Test(expected = NoSuchBucketException.class)
    public void testDeleteObjectBucketNotFound() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);

        // act
        try {
            this.bucketManager.deleteObject(userName, bucketName, objectName1);
        } catch (NoSuchBucketException e) {
            assertTrue(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteObjectNotOwner() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PRIVATE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        try {
            this.bucketManager.deleteObject("fred", bucketName, objectName1);
        } catch (AccessDeniedException e) {
            // assert
            assertTrue(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteObjectNotOwnerButBucketPublicRead() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        try {
            this.bucketManager.deleteObject("fred", bucketName, objectName1);
        } catch (AccessDeniedException e) {
            // assert
            assertTrue(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
            throw e;
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteObjectNotOwnerButBucketAuthenticatedRead() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.AUTHENTICATED_READ);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        try {
            this.bucketManager.deleteObject("fred", bucketName, objectName1);
        } catch (AccessDeniedException e) {
            // assert
            assertTrue(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
            throw e;
        }
    }

    @Test
    public void testDeleteObjectNotOwnerButBucketHasPublicReadWrite() throws Exception {
        // setup
        createBucketDir(bucketName);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)), data);
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PUBLIC_READ_WRITE);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);

        // act
        this.bucketManager.deleteObject("fred", bucketName, objectName1);

        // assert
        assertFalse(new File(String.format("%s/%s/%s", storageDirectory, bucketName, objectName1)).exists());
    }

    @Test
    public void testGetBucketList() {
        // setup
        user.getBucketNames().add(bucketName);
        user.getBucketNames().add(anotherBucketName);
        final BucketMetaData bucketMetaData1 = new BucketMetaData(bucketName);
        final BucketMetaData bucketMetaData2 = new BucketMetaData(anotherBucketName);

        setupGetBucketListExpectations(bucketMetaData1, bucketMetaData2);

        // act
        List<BucketMetaData> result = this.bucketManager.getBucketList(userName);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(BucketMetaData.fromName(bucketName)));
        assertTrue(result.contains(BucketMetaData.fromName(anotherBucketName)));
    }

    @SuppressWarnings("unchecked")
    private void setupGetBucketListExpectations(final BucketMetaData bucketMetaData1, final BucketMetaData bucketMetaData2) {

        DhtReader dhtReader = mock(DhtReader.class);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                PId id = (PId) invocation.getArguments()[0];
                if (id.equals(bucketId))
                    return bucketMetaData1;
                if (id.equals(bucketId2))
                    return bucketMetaData2;
                return null;
            }
        }).when(cache).get(isA(PId.class));
    }

    @Test
    public void testGetBucketListNone() {
        // setup
        this.bucketList.add(new BucketMetaData(bucketName));

        // act
        List<BucketMetaData> result = this.bucketManager.getBucketList("fred");

        // assert
        assertEquals(0, result.size());
    }

    @Test
    public void testGetCannedAclPolicy() {
        // setup
        CannedAclPolicy cannedAclPolicy = CannedAclPolicy.BUCKET_OWNER_FULL_CONTROL;
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, cannedAclPolicy);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);
        user.getBucketNames().add(bucketName);

        // act
        CannedAclPolicy result = this.bucketManager.getCannedAclPolicy(userName, bucketName);

        // assert
        assertEquals(cannedAclPolicy, result);
    }

    @Test
    public void testGetBucketLocation() {
        // setup
        String location = "home";
        final BucketMetaData bucketMetaData = new BucketMetaData(bucketName, CannedAclPolicy.PRIVATE, location);
        when(this.cache.get(bucketId)).thenReturn(bucketMetaData);
        user.getBucketNames().add(bucketName);

        // act
        String result = this.bucketManager.getBucketLocation(userName, bucketName);

        // assert
        assertEquals(location, result);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfUserExceedsMaximumNumberOfBucketsPerUser() {
        // setup
        for (int i = 0; i <= 5; i++) {
            user.getBucketNames().add("bucket" + i);
        }

        // act
        this.bucketManager.createBucket(userName, bucketName, null, null);
    }
}
