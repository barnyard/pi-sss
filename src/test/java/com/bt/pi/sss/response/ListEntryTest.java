package com.bt.pi.sss.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.bt.pi.sss.entities.ObjectMetaData;

public class ListEntryTest {

    @Test
    public void testListEntryJaxb() {
        assertNotNull(new ListEntry());
    }

    @Test
    public void testListEntry() throws Exception {
        // setup
        String tmpdir = System.getProperty("java.io.tmpdir");
        String key = "key";
        File f = new File(tmpdir + "/" + key);
        f.deleteOnExit();
        String data = "data";
        FileUtils.writeStringToFile(f, data);
        FileUtils.writeStringToFile(new File(String.format("%s/%s%s", tmpdir, key, ObjectMetaData.FILE_SUFFIX)), "{}");

        ObjectMetaData objectMetaData = new ObjectMetaData(f);

        // act
        ListEntry result = new ListEntry(objectMetaData);

        // assert
        assertEquals(key, result.getKey());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        assertEquals(df.format(objectMetaData.getLastModified().getTime()), result.getLastModified());
        assertNull(result.getOwner());
        assertEquals(data.length(), result.getSize());
        assertNull(result.getStorageClass());
    }
}
