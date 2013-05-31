/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.exception.BadDigestException;
import com.bt.pi.sss.exception.BucketAlreadyExistsException;
import com.bt.pi.sss.exception.BucketNotEmptyException;
import com.bt.pi.sss.exception.BucketObjectCreationException;
import com.bt.pi.sss.exception.EntityMarshallingException;
import com.bt.pi.sss.exception.EntityTooLargeException;
import com.sun.jersey.api.NotFoundException;

@Component
public class FileSystemBucketUtils {
    public static final String DEFAULT_MAX_OBJECT_SIZE = "5368709120";
    private static final String S_SLASH_S = "%s/%s";
    private static final Log LOG = LogFactory.getLog(FileSystemBucketUtils.class);
    private static final String DEFAULT_TMP_FILE_PREFIX = "pisssTmpObject";
    private static final String DEFAULT_TMP_FILE_SUFFIX = null;
    private static final String DEFAULT_BUCKET_ROOT_DIRECTORY = "var/buckets";

    private String root;
    private long maxObjectSize;
    private String tmpFileSuffix;
    private String tmpFilePrefix;
    private String tmpDir;

    @Resource
    private ArchiveBucketHelper archiveBucketHelper;

    public FileSystemBucketUtils() {
        this.root = DEFAULT_BUCKET_ROOT_DIRECTORY;
        this.maxObjectSize = Long.parseLong(DEFAULT_MAX_OBJECT_SIZE);
        this.tmpFilePrefix = DEFAULT_TMP_FILE_PREFIX;
        this.tmpFileSuffix = DEFAULT_TMP_FILE_SUFFIX;
        this.tmpDir = System.getProperty("java.io.tmpdir");

    }

    public void create(String name) {
        LOG.debug(String.format("create(%s)", name));
        String path = getBucketPath(name);
        File directory = new File(path);
        if (directory.exists())
            throw new BucketAlreadyExistsException();
        boolean result = directory.mkdir();
        LOG.debug(String.format("directory %s created = %s", path, result));
        if (!result)
            throw new BucketAlreadyExistsException();
    }

    public void delete(String name) {
        LOG.debug(String.format("delete(%s)", name));
        String path = getBucketPath(name);

        File directory = new File(path);

        File[] listFiles = directory.listFiles();
        if (null == listFiles) {
            LOG.debug(String.format("bucket directory %s does not exist so cannot be deleted", name));
            return;
        }

        if (listFiles.length > 0)
            throw new BucketNotEmptyException();

        boolean deleted = directory.delete();
        LOG.debug(String.format("directory %s deleted = %s", path, deleted));
        if (!deleted)
            throw new BucketNotEmptyException();
    }

    public String archiveBucket(String bucketName) {
        LOG.debug(String.format("archiveBucket(%s)", bucketName));
        String path = validateBucketExists(bucketName);
        return archiveBucketHelper.archiveBucket(bucketName, path);
    }

    protected String validateBucketExists(String bucketName) {
        String path = getBucketPath(bucketName);
        File bucketFile = new File(path);
        if (!bucketFile.exists()) {
            LOG.error(String.format("directory %s not found", bucketFile));
            throw new NotFoundException();
        }
        return path;
    }

    public List<File> listFiles(String bucketName) {
        LOG.debug(String.format("listFiles(%s)", bucketName));
        List<File> result = new ArrayList<File>();
        File directory = new File(validateBucketExists(bucketName));
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(ObjectMetaData.FILE_SUFFIX))
                continue;
            LOG.debug(String.format("adding %s", file.getName()));
            result.add(file);
        }
        return result;
    }

    public String writeObject(String bucketName, String objectName, InputStream inputStream, String contentType, String contentMd5, String contentDisposition, Map<String, List<String>> xAmzMetaHeaders) {
        LOG.debug(String.format("writeObject(%s, %s, %s, %s, %s, %s, %s)", bucketName, objectName, inputStream, contentType, contentMd5, contentDisposition, xAmzMetaHeaders));
        File file = new File(getObjectPath(bucketName, objectName));
        File tmpFile = null;

        try {
            tmpFile = writeToTmpFile(inputStream);
            byte[] fileMd5Bytes = calculateMd5(tmpFile);
            checkMD5(fileMd5Bytes, contentMd5);
            if (tmpFile.length() > this.maxObjectSize) {
                FileUtils.deleteQuietly(tmpFile);
                throw new EntityTooLargeException();
            }
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    FileUtils.deleteQuietly(tmpFile);
                    throw new BucketObjectCreationException("unable to delete old version of file");
                }
            }

            boolean renameTo = tmpFile.renameTo(file);
            if (!renameTo) {
                FileUtils.deleteQuietly(tmpFile);
                throw new BucketObjectCreationException("unable to overwrite old version of file");
            }
            ObjectMetaData objectMetaData = new ObjectMetaData(file, contentType, contentDisposition, xAmzMetaHeaders, Hex.encodeHexString(fileMd5Bytes)); // create
            // metadata file
            return objectMetaData.getETag();
        } catch (EntityMarshallingException e) {
            LOG.error(e);
            throw new BucketObjectCreationException(e.getMessage());
        } catch (FileNotFoundException e) {
            LOG.error(e);
            throw new BucketObjectCreationException(e.getMessage());
        } catch (IOException e) {
            LOG.error(e);
            throw new BucketObjectCreationException(e.getMessage());
        } finally {
            file = null;
            tmpFile = null;
        }
    }

    private void checkMD5(byte[] fileMd5, String incomingMd5) {
        LOG.debug(String.format("checkMD5(%s, %s)", base64Md5(fileMd5), incomingMd5));
        if (null == incomingMd5)
            return;
        if (incomingMd5.equals(base64Md5(fileMd5)))
            return;
        LOG.warn(String.format("MD5 digest mis-match, calculated = [%s], supplied = [%s]", fileMd5, incomingMd5));
        throw new BadDigestException();
    }

    private String base64Md5(byte[] md5) {
        return new String(Base64.encodeBase64(md5));
    }

    private byte[] calculateMd5(File file) {
        LOG.debug(String.format("getMD5(%s)", file));
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return DigestUtils.md5(fis);
        } catch (IOException e) {
            LOG.error(String.format("error calculating MD5 hash for %s", file.getAbsolutePath()), e);
            throw new BucketObjectCreationException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private File writeToTmpFile(InputStream inputStream) throws IOException {
        LOG.debug(String.format("writeToTmpFile(%s)", inputStream));
        FileOutputStream fos = null;
        try {
            File result = File.createTempFile(this.tmpFilePrefix, this.tmpFileSuffix, new File(this.tmpDir));
            fos = new FileOutputStream(result);
            IOUtils.copyLarge(inputStream, fos);

            return result;
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(inputStream);
        }
    }

    public File readObject(String bucketName, String objectName) {
        return new File(getObjectPath(bucketName, objectName));
    }

    public void deleteObject(String bucketName, String objectName) {
        LOG.debug(String.format("deleteObject(%s, %s)", bucketName, objectName));
        FileUtils.deleteQuietly(new File(getObjectPath(bucketName, objectName)));
        FileUtils.deleteQuietly(new File(getObjectPath(bucketName, objectName) + ObjectMetaData.FILE_SUFFIX));
    }

    private String getBucketPath(String bucketName) {
        return String.format(S_SLASH_S, this.root, bucketName);
    }

    @Property(key = "bucketRootDirectory", defaultValue = DEFAULT_BUCKET_ROOT_DIRECTORY)
    public void setBucketRootDirectory(String path) {
        LOG.debug(String.format("setBucketRootDirectory(%s)", path));
        this.root = path;
    }

    private String encodeObjectName(String objectName) {
        try {
            return URLEncoder.encode(objectName, "UTF8");
        } catch (UnsupportedEncodingException e) {
            String message = String.format("error encoding object name: %s", objectName);
            LOG.error(message, e);
            throw new BucketObjectCreationException(message);
        }
    }

    private String getObjectPath(String bucketName, String objectName) {
        return String.format("%s/%s/%s", this.root, bucketName, encodeObjectName(objectName));
    }

    @Property(key = "maxObjectSize", defaultValue = DEFAULT_MAX_OBJECT_SIZE)
    public void setMaxObjectSizeInBytes(Long size) {
        LOG.debug(String.format("setMaxObjectSizeInBytes(%d)", size));
        this.maxObjectSize = size;
    }

    // TODO make @Property default to null
    public void setTmpFileSuffix(String aTmpFileSuffix) {
        this.tmpFileSuffix = aTmpFileSuffix;
    }

    @Property(key = "tmpFilePrefix", defaultValue = DEFAULT_TMP_FILE_PREFIX)
    public void setTmpFilePrefix(String aTmpFilePrefix) {
        this.tmpFilePrefix = aTmpFilePrefix;
    }

    @Property(key = "fileSystemBucketUtils.tmpDir", defaultValue = DEFAULT_BUCKET_ROOT_DIRECTORY)
    public void setTmpDir(String aTmpDir) {
        LOG.debug(String.format("setTmpDir(%s)", aTmpDir));
        this.tmpDir = aTmpDir;
    }

}
