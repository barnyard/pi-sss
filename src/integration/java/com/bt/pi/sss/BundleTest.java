package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleTest extends IntegrationTestBase {

    private static String javaHome;
    private static String commandBase;

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        IntegrationTestBase.setUpBeforeClass();
        javaHome = System.getProperty("java.home", "/usr");
        commandBase = String.format("unset http_proxy; JAVA_HOME=%s EC2_AMITOOL_HOME=etc/ec2-ami-tools-1.3-26357 etc/ec2-ami-tools-1.3-26357/bin/", javaHome);
    }

    @Test
    public void testEc2UploadBundle() throws Exception {
        // setup
        String manifestFile = "etc/bundled-image/manifest.xml";

        // act
        runCommand(String.format("%s%s --url %s --bucket %s --manifest %s --access-key %s --secret-key %s", commandBase, "ec2-upload-bundle", s3Url, bucketName, manifestFile, ACCESS_KEY, SECRET_KEY));

        // assert
        assertFileIsUploaded("manifest.xml");
        assertFileIsUploaded("test-file.part.0");
        assertFileIsUploaded("test-file.part.1");
    }

    private void assertFileIsUploaded(String filename) throws Exception {
        File file = new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, filename));
        assertTrue(file.exists());
        assertEquals(FileUtils.readFileToString(file), FileUtils.readFileToString(new File("etc/bundled-image/" + filename)));
    }

    @Test
    public void testEc2DeleteBundle() throws Exception {
        // setup
        bucketName = "bundle";
        testEc2UploadBundle();

        // act
        runCommand(String.format("%s%s --yes --clear --url %s --bucket %s --manifest %s --access-key %s --secret-key %s", commandBase, "ec2-delete-bundle", s3Url, bucketName, "etc/bundled-image/manifest.xml", ACCESS_KEY, SECRET_KEY));

        // assert
        assertFalse(new File(String.format("%s/%s", BUCKET_ROOT, bucketName)).exists());
    }
}
