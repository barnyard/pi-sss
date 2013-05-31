package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bt.pi.sss.exception.EntityTooLargeException;
import com.bt.pi.sss.exception.MissingContentLengthException;
import com.bt.pi.sss.util.HeaderUtils;

public class HeaderUtilsTest {
    private HeaderUtils headerUtils;
    private HttpHeaders httpHeaders;
    private Set<Entry<String, List<String>>> headerEntrySet;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        this.headerUtils = new HeaderUtils();
        httpHeaders = Mockito.mock(HttpHeaders.class);
        MultivaluedMap<String, String> headerMap = Mockito.mock(MultivaluedMap.class);
        Mockito.when(httpHeaders.getRequestHeaders()).thenReturn(headerMap);
        headerEntrySet = new HashSet<Entry<String, List<String>>>();
        Mockito.when(headerMap.entrySet()).thenReturn(headerEntrySet);
    }

    @Test(expected = MissingContentLengthException.class)
    public void testCheckForContentLengthMissing() {
        // setup

        // act
        this.headerUtils.validateContentLength(null);
    }

    @Test(expected = MissingContentLengthException.class)
    public void testCheckForContentLengthInvalid() {
        // setup

        // act
        this.headerUtils.validateContentLength("bogus");
    }

    @Test(expected = MissingContentLengthException.class)
    public void testCheckForContentLengthNoValues() {
        // setup

        // act
        this.headerUtils.validateContentLength("");
    }

    @Test
    public void testCheckForContentLengthNormal() {
        // setup

        // act
        this.headerUtils.validateContentLength("12");

        // assert
        // no exception
    }

    @Test(expected = EntityTooLargeException.class)
    public void testCheckForContentLengthTooBig() {
        // setup
        long max = 12;
        this.headerUtils.setMaxObjectSizeInBytes(max);

        // act
        this.headerUtils.validateContentLength("13");
    }

    @Test
    public void testGetXAmzMetaHeaders() {
        // setup
        Entry<String, List<String>> entry1 = makeEntry("someOldHeader", Arrays.asList(new String[] { "a" }));
        this.headerEntrySet.add(entry1);

        Entry<String, List<String>> entry2 = makeEntry("x-amz-meta-1", Arrays.asList(new String[] { "b", "c" }));
        this.headerEntrySet.add(entry2);

        Entry<String, List<String>> entry3 = makeEntry("x-amz-meta-ddd", Arrays.asList(new String[] { "d", "e", "f" }));
        this.headerEntrySet.add(entry3);

        // act
        Map<String, List<String>> result = this.headerUtils.getXAmzMetaHeaders(httpHeaders);

        // assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x-amz-meta-1"));
        assertEquals(2, result.get("x-amz-meta-1").size());
        assertTrue(result.get("x-amz-meta-1").contains("b"));
        assertTrue(result.get("x-amz-meta-1").contains("c"));
        assertTrue(result.containsKey("x-amz-meta-ddd"));
        assertEquals(3, result.get("x-amz-meta-ddd").size());
        assertTrue(result.get("x-amz-meta-ddd").contains("d"));
        assertTrue(result.get("x-amz-meta-ddd").contains("e"));
        assertTrue(result.get("x-amz-meta-ddd").contains("f"));
    }

    @Test
    public void testGetXAmzMetaHeadersNone() {
        // setup
        Entry<String, List<String>> entry1 = makeEntry("someOldHeader", Arrays.asList(new String[] { "a" }));
        this.headerEntrySet.add(entry1);

        // act
        Map<String, List<String>> result = this.headerUtils.getXAmzMetaHeaders(httpHeaders);

        // assert
        assertNull(result);
    }

    private Entry<String, List<String>> makeEntry(final String key, final List<String> entry) {
        return new Entry<String, List<String>>() {
            @Override
            public List<String> setValue(List<String> value) {
                return null;
            }

            @Override
            public List<String> getValue() {
                return entry;
            }

            @Override
            public String getKey() {
                return key;
            }
        };
    }
}
