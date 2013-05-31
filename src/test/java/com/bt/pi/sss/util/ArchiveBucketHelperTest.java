package com.bt.pi.sss.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveBucketHelperTest {

    @InjectMocks
    private ArchiveBucketHelper archiveBucketHelper = new ArchiveBucketHelper();

    @Mock
    private CommandRunner commandRunner;

    private String bucketName = "test1";
    private String root = String.format("%s/unittesting/buckets", System.getProperty("java.io.tmpdir"));
    private String archiveRoot = String.format("%s/unittesting/buckets_archive", System.getProperty("java.io.tmpdir"));

    @Before
    public void setUp() throws Exception {
        File archiveDir = new File(archiveRoot);
        FileUtils.forceMkdir(archiveDir);
        archiveBucketHelper.setArchiveDir(archiveRoot);
        assertTrue(archiveDir.exists());
    }

    @Test
    public void shouldArchiveBucket() throws Exception {
        // setup
        String bucketPath = String.format("%s/%s", root, bucketName);
        // act
        String archiveBucketName = archiveBucketHelper.archiveBucket(bucketName, bucketPath);
        System.err.println("Archived bucket file name: " + archiveBucketName);
        String archiveBucketPath = String.format("%s/%s", archiveRoot, archiveBucketName);

        String copyDirCmd = String.format("cp -r %s %s", bucketPath, archiveBucketPath);
        // assert
        assertTrue(archiveBucketName.startsWith(bucketName));
        verify(commandRunner).runNicely(copyDirCmd);

    }

}
