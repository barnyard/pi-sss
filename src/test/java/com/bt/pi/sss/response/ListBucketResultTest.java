package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXB;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.response.ListBucketResult;
import com.bt.pi.sss.response.ListEntry;
import com.bt.pi.sss.response.MetadataEntry;

public class ListBucketResultTest {
    private String bucketName = "bucket1";

    @Test
    public void testListBucketResult() {
        assertNotNull(new ListBucketResult());
    }

    @Test
    public void testListBucketResultWithMarkerBefore() {
        // setup
        SortedSet<ObjectMetaData> listOfFilesInBucket = new TreeSet<ObjectMetaData>(Arrays
                .asList(new ObjectMetaData[] { ObjectMetaData.fromName("bbb"), ObjectMetaData.fromName("ddd"), ObjectMetaData.fromName("aaa"), ObjectMetaData.fromName("ccc") }));
        String marker = "bbc";

        // act
        ListBucketResult result = new ListBucketResult(bucketName, null, marker, null, listOfFilesInBucket);

        // assert
        assertEquals(marker, result.getMarker());
        assertEquals(bucketName, result.getName());
        List<ListEntry> contents = result.getContents();
        assertEquals(2, contents.size());
        assertEquals("ccc", contents.get(0).getKey());
        assertEquals("ddd", contents.get(1).getKey());
    }

    @Test
    public void testListBucketResultWithMarkerMatches() {
        // setup
        SortedSet<ObjectMetaData> listOfFilesInBucket = new TreeSet<ObjectMetaData>(Arrays
                .asList(new ObjectMetaData[] { ObjectMetaData.fromName("bbb"), ObjectMetaData.fromName("ddd"), ObjectMetaData.fromName("aaa"), ObjectMetaData.fromName("ccc") }));
        String marker = "ccc";

        // act
        ListBucketResult result = new ListBucketResult(bucketName, null, marker, null, listOfFilesInBucket);

        // assert
        assertEquals(marker, result.getMarker());
        assertEquals(bucketName, result.getName());
        List<ListEntry> contents = result.getContents();
        assertEquals(1, contents.size());
        assertEquals("ddd", contents.get(0).getKey());
    }

    @Test
    public void testListBucketResultPopulatesMetaData() {
        String filename = String.format("%s/%s", System.getProperty("java.io.tmpdir"), "test.dat");
        try {
            // setup
            SortedSet<ObjectMetaData> listOfFilesInBucket = new TreeSet<ObjectMetaData>();

            File file = new File(filename);
            Map<String, List<String>> metaHeaders = new HashMap<String, List<String>>();
            metaHeaders.put("x-amz-meta-1", Arrays.asList(new String[] { "aaa", "bbb" }));
            metaHeaders.put("x-amz-meta-2", Arrays.asList(new String[] { "ccc" }));
            ObjectMetaData objectMetaData1 = new ObjectMetaData(file, null, null, metaHeaders, null);
            listOfFilesInBucket.add(objectMetaData1);

            // act
            ListBucketResult result = new ListBucketResult(bucketName, null, null, null, listOfFilesInBucket);

            // assert
            List<MetadataEntry> metadata = result.getMetadata();
            assertEquals(3, metadata.size());
            assertEquals("x-amz-meta-1", metadata.get(0).getKey());
            assertEquals("x-amz-meta-1", metadata.get(1).getKey());
            assertEquals("x-amz-meta-2", metadata.get(2).getKey());
            assertEquals("aaa", metadata.get(0).getValue());
            assertEquals("bbb", metadata.get(1).getValue());
            assertEquals("ccc", metadata.get(2).getValue());
        } finally {
            FileUtils.deleteQuietly(new File(filename + ObjectMetaData.FILE_SUFFIX));
        }
    }

    @Test
    public void testListBucketResultWithMaxkeys() {
        // setup
        SortedSet<ObjectMetaData> listOfFilesInBucket = new TreeSet<ObjectMetaData>(Arrays
                .asList(new ObjectMetaData[] { ObjectMetaData.fromName("bbb"), ObjectMetaData.fromName("ddd"), ObjectMetaData.fromName("aaa"), ObjectMetaData.fromName("ccc") }));
        Integer maxKeys = 2;

        // act
        ListBucketResult result = new ListBucketResult(bucketName, null, null, maxKeys, listOfFilesInBucket);
        JAXB.marshal(result, System.out);

        // assert
        assertEquals(maxKeys.intValue(), result.getMaxKeys());
        assertEquals(bucketName, result.getName());
        List<ListEntry> contents = result.getContents();
        assertEquals(2, contents.size());
        assertEquals("aaa", contents.get(0).getKey());
        assertEquals("bbb", contents.get(1).getKey());
    }
}
