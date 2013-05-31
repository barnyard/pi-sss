package com.bt.pi.sss;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.sss.entities.ObjectMetaData;

@RunWith(MockitoJUnitRunner.class)
public class BucketUserDeleteHelperTest {

    @InjectMocks
    private BucketUserDeleteHelper bucketUserDeleteHelper = new BucketUserDeleteHelper();

    @Mock
    private BucketManager bucketManager;
    @Mock
    private Executor threadPoolTaskExecutor;

    private final String owner = "owner";
    private final String bucketName = "bucketName";
    private CountDownLatch threadLatch = new CountDownLatch(1);

    @Before
    public void setUp() {
        ObjectMetaData object1 = new ObjectMetaData("object1");
        ObjectMetaData object2 = new ObjectMetaData("object2");
        when(bucketManager.getListOfFilesInBucket(owner, bucketName)).thenReturn(new TreeSet<ObjectMetaData>(Arrays.asList(object1, object2)));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.err.println("Invoking thread pool executor");
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                threadLatch.countDown();
                return null;
            }
        }).when(threadPoolTaskExecutor).execute(isA(Runnable.class));
    }

    @Test
    public void shouldArchiveBucketIfBucketNotEmpty() throws Exception {

        // act
        bucketUserDeleteHelper.deleteFullBucket(owner, bucketName);
        threadLatch.await();
        // assert
        verify(bucketManager).archiveBucket(bucketName);
    }

    @Test
    public void shouldDeleteBucketIfEmpty() throws Exception {
        // setup
        when(bucketManager.getListOfFilesInBucket(owner, bucketName)).thenReturn(new TreeSet<ObjectMetaData>());
        // act
        bucketUserDeleteHelper.deleteFullBucket(owner, bucketName);
        // assert
        verify(bucketManager).deleteBucket(owner, bucketName);
        verify(bucketManager, never()).deleteObject(eq(owner), eq(bucketName), isA(String.class));
    }

    @Test
    public void shouldDeleteBucketIfNotEmpty() throws Exception {
        // setup

        // act
        bucketUserDeleteHelper.deleteFullBucket(owner, bucketName);
        // assert
        verify(bucketManager).deleteBucket(owner, bucketName);
        verify(bucketManager).deleteObject(eq(owner), eq(bucketName), eq("object1"));
        verify(bucketManager).deleteObject(eq(owner), eq(bucketName), eq("object2"));
    }

}
