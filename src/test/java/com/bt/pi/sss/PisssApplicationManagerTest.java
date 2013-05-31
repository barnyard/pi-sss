package com.bt.pi.sss;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.sss.entities.BucketCollectionEntity;

@RunWith(MockitoJUnitRunner.class)
public class PisssApplicationManagerTest {
    @InjectMocks
    private PisssApplicationManager pisssApplicationManager = spy(new PisssApplicationManager());
    @Mock
    private BucketUserDeleteHelper bucketUserDeleteHelper;

    @Before
    public void setup() {

    }

    @Test
    public void shouldGetAndSetAppPort() {
        // act
        pisssApplicationManager.setPisssPort(123);

        // assert
        assertEquals(123, pisssApplicationManager.getPort());
    }

    @Test
    public void getApplicationName() throws Exception {
        // act
        String applicationName = pisssApplicationManager.getApplicationName();

        // assert
        assertThat(applicationName, equalTo("pi-sss-manager"));
    }

    @Test
    public void getStartTimeout() throws Exception {
        // setup
        pisssApplicationManager.setStartTimeoutMillis(123);

        // act
        long result = pisssApplicationManager.getStartTimeout();

        // assert
        assertThat(result, equalTo(123L));
    }

    @Test
    public void getActivationCheckPeriod() throws Exception {
        // setup
        pisssApplicationManager.setActivationCheckPeriodSecs(123);

        // act
        int result = pisssApplicationManager.getActivationCheckPeriodSecs();

        // assert
        assertThat(result, equalTo(123));
    }

    @Test
    public void shouldRemoveNodeFromApplicationRecordAndForceCheck() {
        // setup
        String anotherNodeId = "anotherNode";
        doNothing().when(pisssApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
        doNothing().when(pisssApplicationManager).forceActivationCheck();

        // act
        pisssApplicationManager.handleNodeDeparture(anotherNodeId);

        // assert
        verify(pisssApplicationManager).removeNodeIdFromApplicationRecord(anotherNodeId);
        verify(pisssApplicationManager).forceActivationCheck();
    }

    @Test
    public void shouldDeleteBucketsIfReceivedMessage() throws Exception {
        // setup
        BucketCollectionEntity bucketCollectionEntity = new BucketCollectionEntity();
        bucketCollectionEntity.setOwner("owner");
        bucketCollectionEntity.setBucketNames(new HashSet<String>(Arrays.asList("bucket1", "bucket2")));

        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(bucketCollectionEntity);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.DELETE);
        // act
        pisssApplicationManager.deliver(null, receivedMessageContext);
        // assert
        verify(bucketUserDeleteHelper).deleteFullBucket("owner", "bucket1");
        verify(bucketUserDeleteHelper).deleteFullBucket("owner", "bucket2");

    }
}
