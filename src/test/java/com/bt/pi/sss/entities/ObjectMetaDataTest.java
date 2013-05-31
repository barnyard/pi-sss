package com.bt.pi.sss.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.EntityMarshallingException;

public class ObjectMetaDataTest {
    private String filename = String.format("%s/%s", System.getProperty("java.io.tmpdir"), "unittesting.tmp");
    private String metadataFilename = filename + ObjectMetaData.FILE_SUFFIX;

    @Before
    public void before() {
        new File(filename).delete();
        new File(metadataFilename).delete();
    }

    @After
    public void after() {
        new File(filename).delete();
        new File(metadataFilename).delete();
    }

    @Test
    public void getETagWithNoFile() {
        // setup
        String objectName = "fred";
        ObjectMetaData objectMetaData = new ObjectMetaData(objectName);

        // act
        String result = objectMetaData.getETag();

        // assert
        assertNull(result);
    }

    @Test
    public void testObjectMetaDataReadsFromDisk() throws Exception {
        // setup
        FileUtils.writeStringToFile(new File(filename), "data");
        String contentType = "text/plain";
        String contentDisposition = "some value";
        String eTag = "abcd1234";
        FileUtils.writeStringToFile(new File(metadataFilename), "{\"contentType\":\"" + contentType + "\",\"contentDisposition\":\"" + contentDisposition + "\",\"eTag\":\"" + eTag + "\"}");

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename));

        // assert
        assertEquals("unittesting.tmp", result.getName());
        assertEquals(contentType, result.getContentType());
        assertEquals(contentDisposition, result.getContentDisposition());
        assertEquals(eTag, result.getETag());
        assertEquals("data".length(), result.getSize());
    }

    @Test
    public void testObjectMetaDataReadsFromDiskAndDefaultsContentType() throws Exception {
        // setup
        FileUtils.writeStringToFile(new File(filename), "data");
        FileUtils.writeStringToFile(new File(metadataFilename), "{}");

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename));

        // assert
        assertEquals("unittesting.tmp", result.getName());
        assertEquals(ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE, result.getContentType());
    }

    public void testObjectMetaDataReadsFromDiskAndSetsMetaHeaders() throws Exception {
        // setup
        FileUtils.writeStringToFile(new File(filename), "data");
        FileUtils.writeStringToFile(new File(metadataFilename), "{\"xAmzMetaHeaders\":{\"x-amz-meta-1\":[\"a\",\"b\"],\"x-amz-meta-2\":[\"c\",\"d\"]}}");

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename));

        // assert
        assertEquals("unittesting.tmp", result.getName());
        Map<String, List<String>> metaHeaders = result.getXAmzMetaHeaders();
        assertTrue(metaHeaders.containsKey("x-amz-meta-1"));
        assertEquals(2, metaHeaders.get("x-amz-meta-1").size());
        assertTrue(metaHeaders.get("x-amz-meta-1").contains("a"));
        assertTrue(metaHeaders.get("x-amz-meta-1").contains("b"));
        assertTrue(metaHeaders.containsKey("x-amz-meta-2"));
        assertEquals(2, metaHeaders.get("x-amz-meta-2").size());
        assertTrue(metaHeaders.get("x-amz-meta-2").contains("c"));
        assertTrue(metaHeaders.get("x-amz-meta-2").contains("d"));
    }

    @Test
    public void testObjectMetaDataFileNotFound() throws Exception {
        // setup
        FileUtils.writeStringToFile(new File(filename), "data");

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename));

        // assert
        assertEquals("unittesting.tmp", result.getName());
        assertEquals(ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE, result.getContentType());
    }

    @Test
    public void testObjectMetaDataWritesToDiskAndDefaultsContentType() throws Exception {
        // setup
        String aContentType = null;

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename), aContentType, null, null, null);

        // assert
        assertEquals(ObjectMetaData.DEFAULT_OBJECT_CONTENT_TYPE, result.getContentType());
        assertTrue(new File(metadataFilename).exists());
        System.out.println(FileUtils.readFileToString(new File(metadataFilename)));
        assertTrue(FileUtils.readFileToString(new File(metadataFilename)).contains("contentType\":\"binary/octet-stream"));
    }

    @Test
    public void testObjectMetaDataWritesToDiskWithContentType() throws Exception {
        // setup
        String aContentType = "text/plain";

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename), aContentType, null, null, null);

        // assert
        assertEquals(aContentType, result.getContentType());
        assertTrue(new File(metadataFilename).exists());
        System.out.println(FileUtils.readFileToString(new File(metadataFilename)));
        assertTrue(FileUtils.readFileToString(new File(metadataFilename)).contains("contentType\":\"" + aContentType));
    }

    @Test
    public void testObjectMetaDataWritesToDiskWithMetaHeaders() throws Exception {
        // setup
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-amz-meta-1", Arrays.asList(new String[] { "a", "b" }));
        headers.put("x-amz-meta-2", Arrays.asList(new String[] { "c", "d" }));

        // act
        new ObjectMetaData(new File(filename), null, null, headers, null);

        // assert
        assertTrue(new File(metadataFilename).exists());
        System.out.println(FileUtils.readFileToString(new File(metadataFilename)));
        assertTrue(FileUtils.readFileToString(new File(metadataFilename)).contains("\"xAmzMetaHeaders\":{\"x-amz-meta-1\":[\"a\",\"b\"],\"x-amz-meta-2\":[\"c\",\"d\"]}"));
    }

    @Test
    public void testObjectMetaDataWritesToDiskWithContentDisposition() throws Exception {
        // setup
        String aContentType = "text/plain";
        String aContentDisposition = "some disposition";

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename), aContentType, aContentDisposition, null, null);

        // assert
        assertEquals(aContentType, result.getContentType());
        assertTrue(new File(metadataFilename).exists());
        System.out.println(FileUtils.readFileToString(new File(metadataFilename)));
        assertTrue(FileUtils.readFileToString(new File(metadataFilename)).contains("contentDisposition\":\"" + aContentDisposition));
    }

    @Test
    public void testObjectMetaDataWritesToDiskWithETag() throws Exception {
        // setup
        String eTag = "abcd1234";

        // act
        ObjectMetaData result = new ObjectMetaData(new File(filename), null, null, null, eTag);

        // assert
        assertEquals(eTag, result.getETag());
        assertTrue(new File(metadataFilename).exists());
        System.out.println(FileUtils.readFileToString(new File(metadataFilename)));
        assertTrue(FileUtils.readFileToString(new File(metadataFilename)).contains("eTag\":\"" + eTag));
    }

    @Test(expected = EntityMarshallingException.class)
    public void testConstructorThrowEntityMarshallingExceptionWhenMapperThrowsJsonGenerationException() throws Exception {
        // setup
        String aContentType = "text/plain";
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.doThrow(new JsonGenerationException(message)).when(objectMapper).writeValue(Matchers.isA(File.class), Matchers.isA(ObjectMetaData.class));

        // act
        try {
            new ObjectMetaData(new File(filename), aContentType, null, null, null) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @Test(expected = EntityMarshallingException.class)
    public void testConstructorThrowEntityMarshallingExceptionWhenMapperThrowsJsonMappingException() throws Exception {
        // setup
        String aContentType = "text/plain";
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.doThrow(new JsonMappingException(message)).when(objectMapper).writeValue(Matchers.isA(File.class), Matchers.isA(ObjectMetaData.class));

        // act
        try {
            new ObjectMetaData(new File(filename), aContentType, null, null, null) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @Test(expected = EntityMarshallingException.class)
    public void testConstructorThrowEntityMarshallingExceptionWhenMapperThrowsIOException() throws Exception {
        // setup
        String aContentType = "text/plain";
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.doThrow(new IOException(message)).when(objectMapper).writeValue(Matchers.isA(File.class), Matchers.isA(ObjectMetaData.class));

        // act
        try {
            new ObjectMetaData(new File(filename), aContentType, null, null, null) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test(expected = EntityMarshallingException.class)
    public void testFromfileMapperThrowsJsonParseException() throws Exception {
        // setup
        File file = new File(filename);
        FileUtils.writeStringToFile(new File(filename), "stuff");
        FileUtils.writeStringToFile(new File(metadataFilename), "{\"contentType\":\"text/plain\"}");
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.when(objectMapper.readValue(Matchers.isA(File.class), Matchers.isA(Class.class))).thenThrow(new JsonParseException(message, new JsonLocation(new String(), 1, 1, 1)));

        // act
        try {
            new ObjectMetaData(file) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test(expected = EntityMarshallingException.class)
    public void testFromfileMapperThrowsJsonMappingException() throws Exception {
        // setup
        File file = new File(filename);
        FileUtils.writeStringToFile(new File(filename), "stuff");
        FileUtils.writeStringToFile(new File(metadataFilename), "{\"contentType\":\"text/plain\"}");
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.when(objectMapper.readValue(Matchers.isA(File.class), Matchers.isA(Class.class))).thenThrow(new JsonMappingException(message));

        // act
        try {
            new ObjectMetaData(file) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test(expected = EntityMarshallingException.class)
    public void testFromfileMapperThrowsIOException() throws Exception {
        // setup
        File file = new File(filename);
        FileUtils.writeStringToFile(new File(filename), "stuff");
        FileUtils.writeStringToFile(new File(metadataFilename), "{\"contentType\":\"text/plain\"}");
        final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        String message = "shit happens";
        Mockito.when(objectMapper.readValue(Matchers.isA(File.class), Matchers.isA(Class.class))).thenThrow(new IOException(message));

        // act
        try {
            new ObjectMetaData(file) {
                @Override
                protected ObjectMapper getObjectMapper() {
                    return objectMapper;
                }
            };
        } catch (EntityMarshallingException e) {
            // assert
            assertTrue(e.getMessage().contains(message));
            throw e;
        }
    }

    @Test
    public void testGetETag() throws Exception {
        // setup
        File file = new File(filename);
        FileUtils.writeStringToFile(new File(filename), "stuff");
        String eTag = "abcd124";
        ObjectMetaData objectMetaData = new ObjectMetaData(file, null, null, null, eTag);

        // act
        String result = objectMetaData.getETag();

        // assert
        assertEquals(eTag, result);
    }
}
