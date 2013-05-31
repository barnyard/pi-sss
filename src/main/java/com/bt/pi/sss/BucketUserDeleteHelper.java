package com.bt.pi.sss;

import java.util.SortedSet;
import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.sss.entities.ObjectMetaData;

@Component
public class BucketUserDeleteHelper {
    private static final Log LOG = LogFactory.getLog(BucketUserDeleteHelper.class);

    @Resource
    private BucketManager bucketManager;

    private Executor taskExecutor;

    public BucketUserDeleteHelper() {
        taskExecutor = null;
    }

    public void deleteFullBucket(final String owner, final String bucketName) {
        LOG.debug("Deleting bucket " + bucketName);

        archiveBucketInThreadAndDeleteFiles(owner, bucketName, new Continuation<String, Exception>() {

            @Override
            public void receiveResult(String archiveBucketName) {
                bucketManager.deleteBucket(owner, bucketName);
            }

            @Override
            public void receiveException(Exception exception) {
                LOG.info("Exception when archiving bucket " + bucketName, exception);
            }

        });

    }

    private void archiveBucketInThreadAndDeleteFiles(final String owner, final String bucketName, final Continuation<String, Exception> continuation) {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {

                LOG.debug(String.format("Going to archive bucket %s", bucketName));
                String archiveBucket = null;
                final SortedSet<ObjectMetaData> filesInBucket = bucketManager.getListOfFilesInBucket(owner, bucketName);
                if (!filesInBucket.isEmpty()) {
                    archiveBucket = bucketManager.archiveBucket(bucketName);
                    LOG.debug(String.format("Bucket %s was succesfully archived with name %s", bucketName, archiveBucket));

                    for (ObjectMetaData object : filesInBucket) {
                        bucketManager.deleteObject(owner, bucketName, object.getName());
                    }

                }
                continuation.receiveResult(archiveBucket);

            }
        });
    }

    @Resource(type = ThreadPoolTaskExecutor.class)
    public void setTaskExecutor(Executor theTaskExecutor) {
        this.taskExecutor = theTaskExecutor;
    }

}
