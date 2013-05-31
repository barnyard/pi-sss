package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.BadDigestException;
import com.bt.pi.sss.exception.EntityTooLargeException;
import com.sun.jersey.api.NotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemBucketUtilsTest {
    @InjectMocks
    private FileSystemBucketUtils fileSystemBucketUtils = new FileSystemBucketUtils();

    @Mock
    private ArchiveBucketHelper archiveBucketHelper;
    private String root = String.format("%s/unittesting/buckets", System.getProperty("java.io.tmpdir"));
    private String archiveRoot = String.format("%s/unittesting/buckets_archive", System.getProperty("java.io.tmpdir"));
    private String bucketName = "test1";
    private String data = "test data";
    private String objectName = "object1";

    @Before
    public void setUp() throws Exception {

        this.fileSystemBucketUtils.setBucketRootDirectory(root);
        FileUtils.forceMkdir(new File(root));
        FileUtils.forceMkdir(new File(root + "/" + bucketName));
        this.fileSystemBucketUtils.setTmpDir(System.getProperty("java.io.tmpdir"));
        this.fileSystemBucketUtils.setTmpFilePrefix("unittesting");
        this.fileSystemBucketUtils.setTmpFileSuffix(null);

    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(new File(root));
    }

    @Test
    public void testWriteObjectAlsoCreatesMetaDataFile() throws Exception {
        // setup
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());

        // act
        this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, null, null, null, null);

        // assert
        File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, URLEncoder.encode(objectName, "UTF8"), ObjectMetaData.FILE_SUFFIX));
        assertTrue(metadataFile.exists());
    }

    @Test
    public void testWriteObjectEscapesObjectName() throws Exception {
        // setup
        String objectNameWithPseudoPath = "object1/object2";
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());

        // act
        this.fileSystemBucketUtils.writeObject(bucketName, objectNameWithPseudoPath, inputStream, null, null, null, null);

        // assert
        File file = new File(String.format("%s/%s/%s", root, bucketName, URLEncoder.encode(objectNameWithPseudoPath, "UTF8")));
        assertTrue(file.exists());
        assertEquals(data, FileUtils.readFileToString(file));
        File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, URLEncoder.encode(objectNameWithPseudoPath, "UTF8"), ObjectMetaData.FILE_SUFFIX));
        assertTrue(metadataFile.exists());
    }

    @Test(expected = NotFoundException.class)
    public void testListFilesFileNotFound() throws Exception {
        //
        FileUtils.deleteDirectory(new File(root + "/" + bucketName));

        // act
        this.fileSystemBucketUtils.listFiles(bucketName);
    }

    @Test(expected = EntityTooLargeException.class)
    public void testWriteObjectRejectsFileOverMaxSize() throws Exception {
        // setup
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        this.fileSystemBucketUtils.setMaxObjectSizeInBytes(new Long(data.length() - 1));

        // act
        try {
            this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, null, null, null, null);
        } catch (EntityTooLargeException e) {
            // assert
            File file = new File(String.format("%s/%s/%s", root, bucketName, URLEncoder.encode(objectName, "UTF8")));
            assertFalse(file.exists());
            File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, URLEncoder.encode(objectName, "UTF8"), ObjectMetaData.FILE_SUFFIX));
            assertFalse(metadataFile.exists());

            throw e;
        }
    }

    @Test
    public void testWriteObjectOverwritesMetaDataFile() throws Exception {
        // setup
        String newData = "aaaaaaaaaaaaaaaaaaaaaaaaa";
        InputStream inputStream = new ByteArrayInputStream(newData.getBytes());
        String newContentType = "text/plain";
        File file = new File(String.format("%s/%s/%s", root, bucketName, objectName));
        FileUtils.writeStringToFile(file, data);
        File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, objectName, ObjectMetaData.FILE_SUFFIX));
        FileUtils.writeStringToFile(metadataFile, "{}");

        // act
        this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, newContentType, null, null, null);

        // assert
        assertEquals(newData, FileUtils.readFileToString(new File(String.format("%s/%s/%s", root, bucketName, objectName))));
        assertTrue(FileUtils.readFileToString(new File(String.format("%s/%s/%s%s", root, bucketName, objectName, ObjectMetaData.FILE_SUFFIX))).contains(newContentType));
    }

    @Test
    public void testDeleteObjectAlsoDeletesMetaDataFile() throws IOException {
        // setup
        File file = new File(String.format("%s/%s/%s", root, bucketName, objectName));
        FileUtils.writeStringToFile(file, data);
        File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, objectName, ObjectMetaData.FILE_SUFFIX));
        FileUtils.writeStringToFile(metadataFile, data);

        // act
        this.fileSystemBucketUtils.deleteObject(bucketName, objectName);

        // assert
        assertFalse(file.exists());
        assertFalse(metadataFile.exists());
    }

    @Test(expected = BadDigestException.class)
    public void testWriteObjectBadMD5() {
        // setup
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        String md5 = "abcdefg";

        // act
        this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, null, md5, null, null);
    }

    @Test
    public void testWriteObjectWithMD5() throws IOException {
        // setup
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        byte[] md5bytes = DigestUtils.md5(new ByteArrayInputStream(data.getBytes()));
        String md5 = new String(Base64.encodeBase64(md5bytes));

        // act
        this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, null, md5, null, null);

        // assert
        File file = new File(String.format("%s/%s/%s", root, bucketName, URLEncoder.encode(objectName, "UTF8")));
        assertTrue(file.exists());
        File metadataFile = new File(String.format("%s/%s/%s%s", root, bucketName, URLEncoder.encode(objectName, "UTF8"), ObjectMetaData.FILE_SUFFIX));
        System.out.println(FileUtils.readFileToString(metadataFile));
        assertTrue(metadataFile.exists());
    }

    @Test
    public void testWriteObjectReturnsMD5() throws IOException {
        // setup
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        byte[] md5bytes = DigestUtils.md5(new ByteArrayInputStream(data.getBytes()));
        String md5 = Hex.encodeHexString(md5bytes);

        // act
        String result = this.fileSystemBucketUtils.writeObject(bucketName, objectName, inputStream, null, null, null, null);

        // assert
        assertEquals(md5, result);
    }

    // seeing edge case in robustness where bucket has been deleted by another thread and NPE was thrown
    @Test
    public void testDeleteBucketThatHasAlreadyBeenDeleted() throws Exception {
        // setup
        FileUtils.deleteDirectory(new File(String.format("%s/%s", root, bucketName)));

        // act
        this.fileSystemBucketUtils.delete(bucketName);

        // assert
        // No exception
    }

    @Test
    public void shouldArchiveBucket() throws Exception {
        // setup
        String bucketPath = String.format("%s/%s", root, bucketName);
        // act
        fileSystemBucketUtils.archiveBucket(bucketName);
        // assert

        verify(archiveBucketHelper).archiveBucket(bucketName, bucketPath);

    }
}
