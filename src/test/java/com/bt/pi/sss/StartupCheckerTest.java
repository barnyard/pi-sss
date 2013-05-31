package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.sss.exception.PisssConfigException;

public class StartupCheckerTest {
    private StartupChecker startupChecker;
    private String root = String.format("%s/unittesting/buckets", System.getProperty("java.io.tmpdir"));
    private File rootDir;

    @Before
    public void setUp() throws Exception {
        this.startupChecker = new StartupChecker();
        FileUtils.deleteDirectory(new File(String.format("%s/unittesting", System.getProperty("java.io.tmpdir"))));
        this.startupChecker.setBucketRootDirectory(root);
        this.rootDir = new File(root);
        this.rootDir.deleteOnExit();
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(new File(String.format("%s/unittesting", System.getProperty("java.io.tmpdir"))));
    }

    @Test
    public void testAfterPropertiesSetRootNotExists() throws Exception {
        // act
        this.startupChecker.afterPropertiesSet();

        // assert
        assertTrue(rootDir.isDirectory());
    }

    @Test(expected = PisssConfigException.class)
    public void testAfterPropertiesSetRootNotDirectory() throws Exception {
        // setup
        FileUtils.writeStringToFile(this.rootDir, "bah");

        // act
        try {
            this.startupChecker.afterPropertiesSet();
        } catch (PisssConfigException e) {
            // assert
            assertEquals(String.format("%s is not a directory", root), e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAfterPropertiesSetOk() throws Exception {
        // setup
        assertTrue(this.rootDir.mkdirs());

        // act
        this.startupChecker.afterPropertiesSet();

        // assert
        // no exception
    }
}
